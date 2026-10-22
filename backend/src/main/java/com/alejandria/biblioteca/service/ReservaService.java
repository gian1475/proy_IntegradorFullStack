package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.ReservaDao;
import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.reserva.ReservaDTO;
import com.alejandria.biblioteca.dto.reserva.ReservaRespuestaDTO;
import com.alejandria.biblioteca.entity.Libro;
import com.alejandria.biblioteca.entity.Reserva;
import com.alejandria.biblioteca.entity.Usuario;
import com.alejandria.biblioteca.exception.NegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Reservas: al reservar se aparta un ejemplar (baja el stock).
 * Si se cancela o vence sin recogerse, el ejemplar vuelve al stock.
 */
@Service
public class ReservaService {

    private static final Logger log = LoggerFactory.getLogger(ReservaService.class);

    public static final int DIAS_MAXIMOS_ANTICIPACION = 7;

    private final ReservaDao reservaDao;
    private final UsuarioDao usuarioDao;
    private final LibroService libroService;
    private final PrestatarioService prestatarioService;

    public ReservaService(ReservaDao reservaDao, UsuarioDao usuarioDao,
                          LibroService libroService, PrestatarioService prestatarioService) {
        this.reservaDao = reservaDao;
        this.usuarioDao = usuarioDao;
        this.libroService = libroService;
        this.prestatarioService = prestatarioService;
    }

    // reserva crear: el prestatario (o el encargado en ventanilla) reserva y se aparta 1 ejemplar.
    // valida: fecha de recojo dentro de 7 dias, prestatario sin sancion y q no tenga otra reserva del mismo libro
    @Transactional
    public ReservaRespuestaDTO crear(ReservaDTO dto) {
        if (dto.getIdLibro() == null || dto.getIdUsuario() == null || dto.getFechaRecojoLimite() == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Datos incompletos para procesar la reserva");
        }

        LocalDateTime ahora = LocalDateTime.now();
        if (!dto.getFechaRecojoLimite().isAfter(ahora)
                || dto.getFechaRecojoLimite().isAfter(ahora.plusDays(DIAS_MAXIMOS_ANTICIPACION))) {
            throw new NegocioException(HttpStatus.BAD_REQUEST,
                    "El recojo debe ser en el futuro y dentro de los próximos " + DIAS_MAXIMOS_ANTICIPACION + " días");
        }

        Usuario prestatario = usuarioDao.findById(dto.getIdUsuario())
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        prestatarioService.validarHabilitado(prestatario);

        if (reservaDao.findFirstByPrestatarioIdUsuarioAndLibroIdLibroAndEstadoReserva(
                dto.getIdUsuario(), dto.getIdLibro(), "pendiente").isPresent()) {
            throw new NegocioException(HttpStatus.CONFLICT, "Ya tiene una reserva pendiente de este libro");
        }

        Libro libro = libroService.bloquearParaSalida(dto.getIdLibro());
        libroService.descontarStock(libro, "reservado"); // stock automatico: al reservar baja 1

        Reserva reserva = new Reserva();
        reserva.setLibro(libro);
        reserva.setPrestatario(prestatario);
        reserva.setFechaReserva(ahora);
        reserva.setFechaRecojoLimite(dto.getFechaRecojoLimite());
        reserva.setEstadoReserva("pendiente");
        reserva.setPosicionCola(1);

        Reserva guardada = reservaDao.save(reserva);
        log.info("Reserva #{} creada: libro '{}' para usuario {}, recojo hasta {}",
                guardada.getIdReserva(), libro.getTitulo(), prestatario.getIdUsuario(), dto.getFechaRecojoLimite());
        return ReservaRespuestaDTO.desde(guardada);
    }

    @Transactional
    public List<ReservaRespuestaDTO> listarTodas() {
        liberarVencidas();
        return reservaDao.findAllByOrderByFechaReservaDesc().stream().map(ReservaRespuestaDTO::desde).toList();
    }

    @Transactional
    public List<ReservaRespuestaDTO> listarPorUsuario(Integer idUsuario) {
        liberarVencidas();
        return reservaDao.findByPrestatarioIdUsuarioOrderByFechaReservaDesc(idUsuario).stream()
                .map(ReservaRespuestaDTO::desde).toList();
    }

    @Transactional(readOnly = true)
    public Integer obtenerIdPrestatario(Integer idReserva) {
        return buscar(idReserva).getPrestatario().getIdUsuario();
    }

    /** Cancela una reserva pendiente y devuelve el ejemplar al stock. */
    // reserva cancelar: tambien es logica, la reserva pasa a 'cancelada' y el ejemplar vuelve al stock
    @Transactional
    public void cancelar(Integer idReserva) {
        Reserva reserva = buscar(idReserva);
        if (!"pendiente".equals(reserva.getEstadoReserva())) {
            throw new NegocioException(HttpStatus.CONFLICT, "Solo se puede cancelar una reserva pendiente");
        }
        reserva.setEstadoReserva("cancelada");
        reservaDao.save(reserva);
        libroService.reponerStock(reserva.getLibro().getIdLibro());
        log.info("Reserva #{} cancelada", idReserva);
    }

    // eliminacion fisica: aqui SI se borra la fila de la bd (DELETE FROM reserva WHERE id_reserva = ?).
    // solo las canceladas o vencidas (RF17): ya no sirven para nada y su ejemplar ya volvio al stock.
    // las pendientes no se borran xq alguien va a recoger el libro, y las atendidas son historial de un prestamo
    @Transactional
    public void eliminar(Integer idReserva) {
        liberarVencidas(); // si una pendiente ya vencio, primero pasa a 'vencida'
        Reserva reserva = buscar(idReserva);
        String estado = reserva.getEstadoReserva();
        if (!"cancelada".equals(estado) && !"vencida".equals(estado)) {
            throw new NegocioException(HttpStatus.CONFLICT, "Solo se pueden eliminar reservas canceladas o vencidas");
        }
        reservaDao.delete(reserva); // eliminacion fisica: el delete de JpaRepository hace el DELETE en la tabla
        log.info("Reserva #{} ({}) eliminada físicamente de la BD", idReserva, estado);
    }

    /**
     * Si el prestatario tenía una reserva pendiente de ese libro, la marca como 'atendida'.
     * Devuelve true si la encontró (el ejemplar ya estaba apartado, no hay que descontar stock otra vez).
     */
    @Transactional
    public boolean atenderSiExiste(Integer idPrestatario, Integer idLibro) {
        Optional<Reserva> reserva = reservaDao.findFirstByPrestatarioIdUsuarioAndLibroIdLibroAndEstadoReserva(
                idPrestatario, idLibro, "pendiente");
        if (reserva.isEmpty() || reserva.get().getFechaRecojoLimite().isBefore(LocalDateTime.now())) {
            return false;
        }
        reserva.get().setEstadoReserva("atendida");
        reservaDao.save(reserva.get());
        return true;
    }

    /** ¿Alguien tiene reservado este libro? (no se amplía un préstamo si hay reservas pendientes, RF05). */
    @Transactional
    public boolean hayReservasPendientes(Integer idLibro) {
        liberarVencidas(); // las que ya vencieron no cuentan
        return reservaDao.existsByLibroIdLibroAndEstadoReserva(idLibro, "pendiente");
    }

    /** Las reservas pendientes cuya hora de recojo pasó quedan 'vencida' y liberan su ejemplar. */
    @Transactional
    public void liberarVencidas() {
        for (Reserva r : reservaDao.findByEstadoReservaAndFechaRecojoLimiteBefore("pendiente", LocalDateTime.now())) {
            r.setEstadoReserva("vencida");
            reservaDao.save(r);
            libroService.reponerStock(r.getLibro().getIdLibro());
            log.info("Reserva #{} vencida sin recoger; ejemplar liberado", r.getIdReserva());
        }
    }

    private Reserva buscar(Integer idReserva) {
        return reservaDao.findById(idReserva)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
    }
}
