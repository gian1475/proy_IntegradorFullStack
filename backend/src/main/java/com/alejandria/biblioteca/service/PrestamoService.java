package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.LibroDao;
import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.prestamo.DevolucionDTO;
import com.alejandria.biblioteca.dto.prestamo.PrestamoDTO;
import com.alejandria.biblioteca.dto.prestamo.PrestamoDetalleDTO;
import com.alejandria.biblioteca.dto.prestamo.RegistroPrestamoDTO;
import com.alejandria.biblioteca.dto.reporte.ReportePrestamoDTO;
import com.alejandria.biblioteca.entity.Libro;
import com.alejandria.biblioteca.entity.Prestamo;
import com.alejandria.biblioteca.entity.Usuario;
import com.alejandria.biblioteca.exception.NegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Reglas del negocio de los préstamos en sala. */
@Service
public class PrestamoService {

    private static final Logger log = LoggerFactory.getLogger(PrestamoService.class);

    public static final int HORAS_PRESTAMO = 3;   // 3 horas: tiempo maximo del prestamo en sala
    public static final int HORAS_AMPLIACION = 1; // ampliacion: RF05 dice "hasta por 1 hora adicional" (solo 1 vez)

    private final PrestamoDao prestamoDao;
    private final LibroDao libroDao;
    private final UsuarioDao usuarioDao;
    private final LibroService libroService;
    private final PrestatarioService prestatarioService;
    private final ReservaService reservaService;
    private final HorarioAtencion horario;

    public PrestamoService(PrestamoDao prestamoDao, LibroDao libroDao, UsuarioDao usuarioDao,
                           LibroService libroService, PrestatarioService prestatarioService,
                           ReservaService reservaService, HorarioAtencion horario) {
        this.prestamoDao = prestamoDao;
        this.libroDao = libroDao;
        this.usuarioDao = usuarioDao;
        this.libroService = libroService;
        this.prestatarioService = prestatarioService;
        this.reservaService = reservaService;
        this.horario = horario;
    }

    /** Hora límite de devolución: inicio + 3 horas. */
    // 3 horas: hora limite = hora de inicio + 3 horas (tiene su prueba junit en PrestamoServiceTest)
    public static LocalDateTime calcularHoraLimite(LocalDateTime inicio) {
        return inicio.plusHours(HORAS_PRESTAMO);
    }

    // ---------- ADICIONAR ----------

    // crud prestamo adicionar: aqui el encargado registra el prestamo en sala.
    // datos q se guardan en la tabla prestamo: prestatario (se busca por dni), libro, encargado
    // (sale de la sesion), fecha y hora de inicio, hora limite (+3h), ampliado = false y estado 'activo'
    @Transactional
    public PrestamoDTO registrar(RegistroPrestamoDTO dto, Integer idEncargado) {
        if (dto.getDniPrestatario() == null || dto.getIdLibro() == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Debe indicar el DNI del prestatario y el libro");
        }

        // horario de atencion: fuera de 8:00-20:00 no se presta
        LocalDateTime ahora = horario.ahora();
        horario.validarAbierto(ahora);

        Usuario prestatario = usuarioDao.findByDni(dto.getDniPrestatario().trim())
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "No existe un usuario con ese DNI"));

        // validaciones del prestamo: q sea prestatario, q este activo y sin sancion activa
        prestatarioService.validarHabilitado(prestatario);

        reservaService.liberarVencidas();
        // y q el libro no este de baja ni en mantenimiento
        Libro libro = libroService.bloquearParaSalida(dto.getIdLibro());

        // Si el prestatario tenía este libro reservado, el ejemplar ya estaba apartado: no se descuenta otra vez
        boolean veniaDeReserva = reservaService.atenderSiExiste(prestatario.getIdUsuario(), libro.getIdLibro());
        if (veniaDeReserva) {
            if (libro.getStock() == 0) {
                libro.setEstadoLibro("prestado");
                libroDao.save(libro);
            }
        } else {
            libroService.descontarStock(libro, "prestado"); // stock automatico: baja 1 (si hay 0 no deja prestar)
        }

        Prestamo prestamo = new Prestamo();
        prestamo.setPrestatario(prestatario);
        prestamo.setEncargado(usuarioDao.getReferenceById(idEncargado));
        prestamo.setLibro(libro);
        prestamo.setFechaPrestamo(ahora);
        // horario de atencion: si las 3 horas pasan de las 20:00, la hora limite queda en las 20:00
        prestamo.setFechaDevolucionPrevista(horario.limitarAlCierre(calcularHoraLimite(ahora)));
        prestamo.setAmpliado(false);
        prestamo.setEstadoPrestamo("activo");

        Prestamo guardado = prestamoDao.save(prestamo);
        // logback: el prestamo queda registrado en el log
        log.info("Préstamo #{} registrado: libro '{}' a DNI {} (encargado {}), límite {}{}",
                guardado.getIdPrestamo(), libro.getTitulo(), prestatario.getDni(), idEncargado,
                guardado.getFechaDevolucionPrevista(), veniaDeReserva ? " [desde reserva]" : "");
        return PrestamoDTO.desde(guardado);
    }

    // ---------- CONSULTAR ----------

    /** Consulta con JOIN (prestamo + usuario + libro). Antes actualiza los vencidos. */
    // crud prestamo consultar (consulta dos tablas): primero pasa a 'vencido' los q pasaron las 3 horas
    // y despues trae los activos con el JOIN (ver PrestamoDao.listarActivosConDetalle)
    @Transactional
    public List<PrestamoDetalleDTO> listarActivos() {
        prestamoDao.marcarVencidos(horario.ahora());
        return prestamoDao.listarActivosConDetalle();
    }

    /** Consulta de una sola tabla (prestamo). */
    // crud prestamo consultar (consulta una tabla): historial de un prestatario (ver PrestamoDao.buscarPorPrestatario).
    // se ve en la pantalla "Mis préstamos" del prestatario. antes de consultar se marcan los vencidos
    @Transactional
    public List<PrestamoDTO> listarPorPrestatario(Integer idPrestatario) {
        prestamoDao.marcarVencidos(horario.ahora());
        return prestamoDao.buscarPorPrestatario(idPrestatario).stream()
                .map(PrestamoDTO::desde)
                .toList();
    }

    /** Historial (RF07) para el encargado y el admin: filtros opcionales por DNI, libro y rango de fechas. */
    // historial: consulta con JOIN (prestamo + prestatario + encargado + libro), ver PrestamoDao.buscarHistorial.
    // trae todos los prestamos, tambien los anulados y los de usuarios dados de baja
    @Transactional
    public List<ReportePrestamoDTO> historial(String dni, String libro, LocalDate desde, LocalDate hasta) {
        RangoFechas rango = new RangoFechas(desde, hasta); // valida q "desde" no sea despues de "hasta"
        prestamoDao.marcarVencidos(horario.ahora());
        return prestamoDao.buscarHistorial(textoOpcional(dni), textoOpcional(libro), rango.inicio(), rango.fin());
    }

    @Transactional(readOnly = true)
    public PrestamoDTO obtener(Integer idPrestamo) {
        return PrestamoDTO.desde(buscar(idPrestamo));
    }

    // ---------- ACTUALIZAR ----------

    /** Amplía la hora límite. Solo una vez y solo si el préstamo no está vencido. */
    // crud prestamo actualizar (1) ampliar: se actualiza la hora limite (+1 hora) y el campo ampliado pasa a true.
    // solo se puede 1 vez, si el prestamo no esta vencido y si nadie tiene reservado ese libro (RF05)
    @Transactional
    public PrestamoDTO ampliar(Integer idPrestamo) {
        Prestamo prestamo = buscar(idPrestamo);

        if (!"activo".equals(prestamo.getEstadoPrestamo())
                || horario.ahora().isAfter(prestamo.getFechaDevolucionPrevista())) {
            throw new NegocioException(HttpStatus.CONFLICT, "Solo se puede ampliar un préstamo activo y no vencido");
        }
        if (prestamo.getAmpliado()) {
            throw new NegocioException(HttpStatus.CONFLICT, "El préstamo ya fue ampliado una vez");
        }
        if (reservaService.hayReservasPendientes(prestamo.getLibro().getIdLibro())) {
            throw new NegocioException(HttpStatus.CONFLICT, "No se puede ampliar: el libro tiene reservas pendientes");
        }

        // horario de atencion: "hasta" 1 hora, sin pasar del cierre. si ya termina a las 20:00 no hay nada q ampliar
        LocalDateTime limiteActual = prestamo.getFechaDevolucionPrevista();
        LocalDateTime nuevoLimite = horario.limitarAlCierre(limiteActual.plusHours(HORAS_AMPLIACION));
        if (!nuevoLimite.isAfter(limiteActual)) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "No se puede ampliar: el préstamo ya termina a la hora de cierre (" + horario.getCierre() + ")");
        }

        prestamo.setFechaDevolucionPrevista(nuevoLimite);
        prestamo.setAmpliado(true);
        log.info("Préstamo #{} ampliado hasta {}", idPrestamo, prestamo.getFechaDevolucionPrevista());
        return PrestamoDTO.desde(prestamoDao.save(prestamo));
    }

    /** Registra la devolución con el estado físico del libro y repone el stock. */
    // crud prestamo actualizar (2) devolucion: se actualiza la fecha de devolucion real,
    // el estado fisico del libro (bueno o dañado) y el estado pasa a 'devuelto'
    @Transactional
    public PrestamoDTO devolver(Integer idPrestamo, DevolucionDTO dto) {
        String estadoFisico = dto.getEstadoFisico();
        if (!"bueno".equals(estadoFisico) && !"dañado".equals(estadoFisico)) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "El estado físico debe ser 'bueno' o 'dañado'");
        }

        Prestamo prestamo = buscar(idPrestamo);
        if (!estaEnCurso(prestamo)) {
            throw new NegocioException(HttpStatus.CONFLICT, "El préstamo ya fue devuelto o anulado");
        }

        prestamo.setFechaDevolucionReal(horario.ahora());
        prestamo.setEstadoFisicoDevolucion(estadoFisico);
        prestamo.setEstadoPrestamo("devuelto");

        // RF02: si vuelve bueno, regresa al stock; si vuelve dañado, se va a mantenimiento y no regresa
        if ("dañado".equals(estadoFisico)) {
            libroService.registrarEjemplarDanado(prestamo.getLibro().getIdLibro());
        } else {
            libroService.reponerStock(prestamo.getLibro().getIdLibro()); // stock automatico: sube 1
        }
        // si devolvio despues de la hora limite queda marcado en el log como CON RETRASO
        boolean conRetraso = prestamo.getFechaDevolucionReal().isAfter(prestamo.getFechaDevolucionPrevista());
        log.info("Préstamo #{} devuelto en estado '{}'{}", idPrestamo, estadoFisico, conRetraso ? " CON RETRASO" : "");
        return PrestamoDTO.desde(prestamoDao.save(prestamo));
    }

    // ---------- ELIMINAR (lógico) ----------

    /** No borra la fila: cambia el estado a 'anulado'. Si el libro seguía prestado, repone el stock. */
    // crud prestamo eliminar -> eliminacion logica: UPDATE del estado a 'anulado'. la fila se queda
    // en la bd xq el prestamo es historial (reportes e indicadores) y las incidencias lo apuntan
    @Transactional
    public void anular(Integer idPrestamo) {
        Prestamo prestamo = buscar(idPrestamo);
        if ("anulado".equals(prestamo.getEstadoPrestamo())) {
            throw new NegocioException(HttpStatus.CONFLICT, "El préstamo ya está anulado");
        }

        if (estaEnCurso(prestamo)) {
            libroService.reponerStock(prestamo.getLibro().getIdLibro());
        }
        prestamo.setEstadoPrestamo("anulado"); // eliminacion logica: aqui cambia el estado
        prestamoDao.save(prestamo);
        log.info("Préstamo #{} anulado (eliminación lógica)", idPrestamo);
    }

    // ---------- Auxiliares ----------

    private Prestamo buscar(Integer idPrestamo) {
        return prestamoDao.findById(idPrestamo)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Préstamo no encontrado"));
    }

    /** Un filtro vacío ("" o espacios) es lo mismo que no filtrar. */
    private String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    /** Un préstamo está en curso si el libro aún no se devuelve (activo o vencido). */
    private boolean estaEnCurso(Prestamo prestamo) {
        return "activo".equals(prestamo.getEstadoPrestamo()) || "vencido".equals(prestamo.getEstadoPrestamo());
    }
}
