package com.backend.demo.service;

import com.backend.demo.dao.LibroDao;
import com.backend.demo.dao.PrestamoDao;
import com.backend.demo.dao.UsuarioDao;
import com.backend.demo.dto.DevolucionDTO;
import com.backend.demo.dto.PrestamoDTO;
import com.backend.demo.dto.PrestamoDetalleDTO;
import com.backend.demo.dto.RegistroPrestamoDTO;
import com.backend.demo.entity.Libro;
import com.backend.demo.entity.Prestamo;
import com.backend.demo.entity.Usuario;
import com.backend.demo.exception.NegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Reglas del negocio de los préstamos en sala. */
@Service
public class PrestamoService {

    public static final int HORAS_PRESTAMO = 3;
    public static final int HORAS_AMPLIACION = 3;

    private final PrestamoDao prestamoDao;
    private final LibroDao libroDao;
    private final UsuarioDao usuarioDao;
    private final LibroService libroService;
    private final PrestatarioService prestatarioService;
    private final ReservaService reservaService;

    public PrestamoService(PrestamoDao prestamoDao, LibroDao libroDao, UsuarioDao usuarioDao,
                           LibroService libroService, PrestatarioService prestatarioService,
                           ReservaService reservaService) {
        this.prestamoDao = prestamoDao;
        this.libroDao = libroDao;
        this.usuarioDao = usuarioDao;
        this.libroService = libroService;
        this.prestatarioService = prestatarioService;
        this.reservaService = reservaService;
    }

    /** Hora límite de devolución: inicio + 3 horas. */
    public static LocalDateTime calcularHoraLimite(LocalDateTime inicio) {
        return inicio.plusHours(HORAS_PRESTAMO);
    }

    // ---------- ADICIONAR ----------

    @Transactional
    public PrestamoDTO registrar(RegistroPrestamoDTO dto, Integer idEncargado) {
        if (dto.getDniPrestatario() == null || dto.getIdLibro() == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Debe indicar el DNI del prestatario y el libro");
        }

        Usuario prestatario = usuarioDao.findByDni(dto.getDniPrestatario().trim())
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "No existe un usuario con ese DNI"));

        prestatarioService.validarHabilitado(prestatario);

        reservaService.liberarVencidas();
        Libro libro = libroService.bloquearParaSalida(dto.getIdLibro());

        // Si el prestatario tenía este libro reservado, el ejemplar ya estaba apartado: no se descuenta otra vez
        boolean veniaDeReserva = reservaService.atenderSiExiste(prestatario.getIdUsuario(), libro.getIdLibro());
        if (veniaDeReserva) {
            if (libro.getStock() == 0) {
                libro.setEstadoLibro("prestado");
                libroDao.save(libro);
            }
        } else {
            libroService.descontarStock(libro, "prestado");
        }

        LocalDateTime ahora = LocalDateTime.now();
        Prestamo prestamo = new Prestamo();
        prestamo.setPrestatario(prestatario);
        prestamo.setEncargado(usuarioDao.getReferenceById(idEncargado));
        prestamo.setLibro(libro);
        prestamo.setFechaPrestamo(ahora);
        prestamo.setFechaDevolucionPrevista(calcularHoraLimite(ahora));
        prestamo.setAmpliado(false);
        prestamo.setEstadoPrestamo("activo");

        return PrestamoDTO.desde(prestamoDao.save(prestamo));
    }

    // ---------- CONSULTAR ----------

    /** Consulta con JOIN (prestamo + usuario + libro). Antes actualiza los vencidos. */
    @Transactional
    public List<PrestamoDetalleDTO> listarActivos() {
        prestamoDao.marcarVencidos();
        return prestamoDao.listarActivosConDetalle();
    }

    /** Consulta de una sola tabla (prestamo). */
    @Transactional(readOnly = true)
    public List<PrestamoDTO> listarPorPrestatario(Integer idPrestatario) {
        return prestamoDao.buscarPorPrestatario(idPrestatario).stream()
                .map(PrestamoDTO::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public PrestamoDTO obtener(Integer idPrestamo) {
        return PrestamoDTO.desde(buscar(idPrestamo));
    }

    // ---------- ACTUALIZAR ----------

    /** Amplía la hora límite. Solo una vez y solo si el préstamo no está vencido. */
    @Transactional
    public PrestamoDTO ampliar(Integer idPrestamo) {
        Prestamo prestamo = buscar(idPrestamo);

        if (!"activo".equals(prestamo.getEstadoPrestamo())
                || LocalDateTime.now().isAfter(prestamo.getFechaDevolucionPrevista())) {
            throw new NegocioException(HttpStatus.CONFLICT, "Solo se puede ampliar un préstamo activo y no vencido");
        }
        if (prestamo.getAmpliado()) {
            throw new NegocioException(HttpStatus.CONFLICT, "El préstamo ya fue ampliado una vez");
        }

        prestamo.setFechaDevolucionPrevista(prestamo.getFechaDevolucionPrevista().plusHours(HORAS_AMPLIACION));
        prestamo.setAmpliado(true);
        return PrestamoDTO.desde(prestamoDao.save(prestamo));
    }

    /** Registra la devolución con el estado físico del libro y repone el stock. */
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

        prestamo.setFechaDevolucionReal(LocalDateTime.now());
        prestamo.setEstadoFisicoDevolucion(estadoFisico);
        prestamo.setEstadoPrestamo("devuelto");

        libroService.reponerStock(prestamo.getLibro().getIdLibro());
        return PrestamoDTO.desde(prestamoDao.save(prestamo));
    }

    // ---------- ELIMINAR (lógico) ----------

    /** No borra la fila: cambia el estado a 'anulado'. Si el libro seguía prestado, repone el stock. */
    @Transactional
    public void anular(Integer idPrestamo) {
        Prestamo prestamo = buscar(idPrestamo);
        if ("anulado".equals(prestamo.getEstadoPrestamo())) {
            throw new NegocioException(HttpStatus.CONFLICT, "El préstamo ya está anulado");
        }

        if (estaEnCurso(prestamo)) {
            libroService.reponerStock(prestamo.getLibro().getIdLibro());
        }
        prestamo.setEstadoPrestamo("anulado");
        prestamoDao.save(prestamo);
    }

    // ---------- Auxiliares ----------

    private Prestamo buscar(Integer idPrestamo) {
        return prestamoDao.findById(idPrestamo)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Préstamo no encontrado"));
    }

    /** Un préstamo está en curso si el libro aún no se devuelve (activo o vencido). */
    private boolean estaEnCurso(Prestamo prestamo) {
        return "activo".equals(prestamo.getEstadoPrestamo()) || "vencido".equals(prestamo.getEstadoPrestamo());
    }
}
