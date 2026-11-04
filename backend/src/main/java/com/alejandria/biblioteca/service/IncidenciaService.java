package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.IncidenciaDao;
import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dao.SancionDao;
import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.incidencia.IncidenciaDTO;
import com.alejandria.biblioteca.dto.incidencia.RegistroIncidenciaDTO;
import com.alejandria.biblioteca.dto.prestamo.DevolucionDTO;
import com.alejandria.biblioteca.entity.Incidencia;
import com.alejandria.biblioteca.entity.Prestamo;
import com.alejandria.biblioteca.entity.Sancion;
import com.alejandria.biblioteca.entity.Usuario;
import com.alejandria.biblioteca.exception.NegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RF06 (CU09): el encargado registra una incidencia de un préstamo (retraso, daño o pérdida) con su sanción.
 * - Advertencia: es informativa, no bloquea. Queda 'cumplida' al registrarse.
 * - Suspensión: de 1 a 30 días, hasta el cierre del último día. El prestatario queda 'suspendido'.
 * - Multa: bloquea préstamos y reservas hasta que se registra el pago.
 */
// incidencias y sanciones (RF06): aqui estan las reglas. el bloqueo lo revisa PrestatarioService (sancion activa)
@Service
public class IncidenciaService {

    private static final Logger log = LoggerFactory.getLogger(IncidenciaService.class);

    private static final String RETRASO = "retraso";
    private static final String DANO = "daño";
    private static final String PERDIDA = "pérdida";
    private static final String ADVERTENCIA = "advertencia";
    private static final String SUSPENSION = "suspensión";
    private static final String MULTA = "multa";
    private static final String ACTIVA = "activa";

    private final PrestamoDao prestamoDao;
    private final IncidenciaDao incidenciaDao;
    private final SancionDao sancionDao;
    private final UsuarioDao usuarioDao;
    private final PrestamoService prestamoService;
    private final PrestatarioService prestatarioService;
    private final HorarioAtencion horario;

    public IncidenciaService(PrestamoDao prestamoDao, IncidenciaDao incidenciaDao, SancionDao sancionDao,
                             UsuarioDao usuarioDao, PrestamoService prestamoService,
                             PrestatarioService prestatarioService, HorarioAtencion horario) {
        this.prestamoDao = prestamoDao;
        this.incidenciaDao = incidenciaDao;
        this.sancionDao = sancionDao;
        this.usuarioDao = usuarioDao;
        this.prestamoService = prestamoService;
        this.prestatarioService = prestatarioService;
        this.horario = horario;
    }

    /** Todas las incidencias con su sanción. Antes cierra las suspensiones que ya terminaron. */
    @Transactional
    public List<IncidenciaDTO> listar() {
        prestatarioService.cerrarSuspensionesTerminadas();
        return incidenciaDao.listarConSancion();
    }

    // ---------- Registrar ----------

    // registrar incidencia (CU09): se guarda la incidencia y su sancion. el formato de los datos ya lo reviso
    // RegistroIncidenciaDTO (bean validation); aqui van las reglas q dependen del prestamo
    @Transactional
    public IncidenciaDTO registrar(RegistroIncidenciaDTO dto) {
        Prestamo prestamo = prestamoDao.findById(dto.idPrestamo())
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Préstamo no encontrado"));
        validarTipoDeIncidencia(prestamo, dto.tipoIncidencia());
        if (incidenciaDao.existsByPrestamoIdPrestamoAndTipoIncidencia(prestamo.getIdPrestamo(), dto.tipoIncidencia())) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "Ese préstamo ya tiene una incidencia por " + dto.tipoIncidencia());
        }
        LocalDateTime ahora = horario.ahora();

        // perdida: el libro no vuelve. el prestamo se cierra como una devolucion dañada, asi el ejemplar
        // no regresa al stock (si era el ultimo, el libro pasa a mantenimiento). lo hace PrestamoService
        if (PERDIDA.equals(dto.tipoIncidencia())) {
            prestamoService.devolver(prestamo.getIdPrestamo(), new DevolucionDTO("dañado"));
        }

        Incidencia incidencia = new Incidencia();
        incidencia.setPrestamo(prestamo);
        incidencia.setTipoIncidencia(dto.tipoIncidencia());
        incidencia.setDescripcion(StringUtils.hasText(dto.descripcion()) ? dto.descripcion().trim() : null);
        incidencia.setFechaIncidencia(ahora);
        incidencia.setEstadoIncidencia("pendiente");

        Sancion sancion = armarSancion(dto, incidencia, ahora);
        incidenciaDao.save(incidencia);
        Sancion guardada = sancionDao.save(sancion);

        // la suspension del usuario corresponde a su sancion: queda 'suspendido' y se cuenta la suspension
        if (SUSPENSION.equals(sancion.getTipoSancion())) {
            Usuario prestatario = prestamo.getPrestatario();
            // RF13: si el admin ya lo dio de baja sigue 'inactivo' (solo el admin lo reactiva)
            if (!"inactivo".equals(prestatario.getEstado())) {
                prestatario.setEstado("suspendido");
                usuarioDao.save(prestatario);
            }
            usuarioDao.sumarSuspension(prestatario.getIdUsuario());
        }

        log.info("Incidencia por {} en el préstamo #{}: {} ({})", dto.tipoIncidencia(), prestamo.getIdPrestamo(),
                sancion.getTipoSancion(), sancion.getDescripcion());
        return IncidenciaDTO.desde(guardada);
    }

    /** Cada incidencia corresponde a un momento del préstamo. */
    // tipos de incidencia: retraso -> paso su hora limite; daño -> ya se devolvio (el estado fisico se ve al devolver);
    // perdida -> el libro sigue fuera (prestamo en curso)
    private void validarTipoDeIncidencia(Prestamo prestamo, String tipo) {
        String estado = prestamo.getEstadoPrestamo();
        if ("anulado".equals(estado)) {
            throw new NegocioException(HttpStatus.CONFLICT, "No se registran incidencias de un préstamo anulado");
        }
        boolean enCurso = "activo".equals(estado) || "vencido".equals(estado);

        if (RETRASO.equals(tipo)) {
            LocalDateTime finDelPrestamo = prestamo.getFechaDevolucionReal() != null
                    ? prestamo.getFechaDevolucionReal() : horario.ahora();
            if (!finDelPrestamo.isAfter(prestamo.getFechaDevolucionPrevista())) {
                throw new NegocioException(HttpStatus.CONFLICT,
                        "El préstamo no tiene retraso: se devolvió (o sigue) dentro de su hora límite");
            }
        } else if (DANO.equals(tipo) && enCurso) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "Primero registre la devolución: el daño se registra sobre un préstamo devuelto");
        } else if (PERDIDA.equals(tipo) && !enCurso) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "La pérdida se registra sobre un préstamo en curso (el libro no se devolvió)");
        }
    }

    // tipos de sancion: advertencia (informativa), suspension (dias) o multa (monto)
    private Sancion armarSancion(RegistroIncidenciaDTO dto, Incidencia incidencia, LocalDateTime ahora) {
        Sancion sancion = new Sancion();
        sancion.setIncidencia(incidencia);
        sancion.setTipoSancion(dto.tipoSancion());
        sancion.setFechaInicio(ahora);
        sancion.setEstadoSancion(ACTIVA);

        switch (dto.tipoSancion()) {
            case ADVERTENCIA -> {
                // la advertencia es informativa: no hay nada que cumplir
                sancion.setDescripcion("Advertencia");
                sancion.setFechaFin(ahora);
                sancion.setEstadoSancion("cumplida");
                incidencia.setEstadoIncidencia("resuelta");
            }
            case SUSPENSION -> {
                if (dto.diasSuspension() == null) {
                    throw new NegocioException(HttpStatus.BAD_REQUEST, "Indique los días de suspensión (de 1 a 30)");
                }
                int dias = dto.diasSuspension();
                // termina al cierre (20:00) del ultimo dia
                sancion.setFechaFin(ahora.toLocalDate().plusDays(dias).atTime(horario.getCierre()));
                sancion.setDescripcion("Suspensión de " + dias + (dias == 1 ? " día" : " días"));
            }
            case MULTA -> {
                if (dto.monto() == null) {
                    throw new NegocioException(HttpStatus.BAD_REQUEST, "Indique el monto de la multa");
                }
                sancion.setMonto(dto.monto());
                sancion.setDescripcion("Multa de S/ " + dto.monto().setScale(2, RoundingMode.HALF_UP));
                // sin fecha de fin: termina cuando se registra el pago
            }
            default -> throw new NegocioException(HttpStatus.BAD_REQUEST, "La sanción debe ser advertencia, suspensión o multa");
        }
        return sancion;
    }

    // ---------- Cumplir o cancelar ----------

    /** La multa se cumple al registrar su pago (RF06). */
    // pago de multa: la sancion pasa a 'cumplida', la incidencia a 'resuelta' y deja de bloquear al prestatario
    @Transactional
    public IncidenciaDTO registrarPago(Integer idSancion) {
        Sancion sancion = buscarSancion(idSancion);
        if (!MULTA.equals(sancion.getTipoSancion())) {
            throw new NegocioException(HttpStatus.CONFLICT, "Solo las multas se pagan");
        }
        if (!ACTIVA.equals(sancion.getEstadoSancion())) {
            throw new NegocioException(HttpStatus.CONFLICT, "La multa ya no está pendiente");
        }
        return terminar(sancion, "cumplida");
    }

    /** Anula una sanción activa registrada por error. El prestatario recupera el servicio si no le queda otra. */
    @Transactional
    public IncidenciaDTO cancelarSancion(Integer idSancion) {
        Sancion sancion = buscarSancion(idSancion);
        if (!ACTIVA.equals(sancion.getEstadoSancion())) {
            throw new NegocioException(HttpStatus.CONFLICT, "Solo se cancelan sanciones activas");
        }
        return terminar(sancion, "cancelada");
    }

    private IncidenciaDTO terminar(Sancion sancion, String estado) {
        sancion.setEstadoSancion(estado);
        sancion.setFechaFin(horario.ahora());
        sancion.getIncidencia().setEstadoIncidencia("resuelta");
        Sancion guardada = sancionDao.save(sancion);
        prestatarioService.restablecer(sancion.getIncidencia().getPrestamo().getPrestatario());
        log.info("Sanción #{} ({}) {}", sancion.getIdSancion(), sancion.getTipoSancion(), estado);
        return IncidenciaDTO.desde(guardada);
    }

    private Sancion buscarSancion(Integer idSancion) {
        return sancionDao.findById(idSancion)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Sanción no encontrada"));
    }
}
