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
 * Si se cancela o vence sin recogerse, el ejemplar vuelve al stock (o pasa al primero de la cola de espera).
 * Si el libro no tiene ejemplares, la reserva entra a la cola de espera.
 */
@Service
public class ReservaService {

    private static final Logger log = LoggerFactory.getLogger(ReservaService.class);

    public static final int DIAS_MAXIMOS_ANTICIPACION = 7;

    private static final String PENDIENTE = "pendiente";
    private static final String EN_ESPERA = ColaEsperaService.EN_ESPERA;

    private final ReservaDao reservaDao;
    private final UsuarioDao usuarioDao;
    private final LibroService libroService;
    private final PrestatarioService prestatarioService;
    private final ColaEsperaService colaEspera;
    private final HorarioAtencion horario;

    public ReservaService(ReservaDao reservaDao, UsuarioDao usuarioDao,
                          LibroService libroService, PrestatarioService prestatarioService,
                          ColaEsperaService colaEspera, HorarioAtencion horario) {
        this.reservaDao = reservaDao;
        this.usuarioDao = usuarioDao;
        this.libroService = libroService;
        this.prestatarioService = prestatarioService;
        this.colaEspera = colaEspera;
        this.horario = horario;
    }

    // reserva crear: el prestatario (o el encargado en ventanilla) reserva y se aparta 1 ejemplar.
    // valida: fecha de recojo dentro de 7 dias, prestatario sin sancion y q no tenga otra reserva del mismo libro.
    // si el libro no tiene stock la reserva entra a la cola de espera (RF04)
    @Transactional
    public ReservaRespuestaDTO crear(ReservaDTO dto) {
        if (dto.getIdLibro() == null || dto.getIdUsuario() == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Datos incompletos para procesar la reserva");
        }

        Usuario prestatario = usuarioDao.findById(dto.getIdUsuario())
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        prestatarioService.validarHabilitado(prestatario);

        if (reservaDao.existsByPrestatarioIdUsuarioAndLibroIdLibroAndEstadoReservaIn(
                dto.getIdUsuario(), dto.getIdLibro(), List.of(PENDIENTE, EN_ESPERA))) {
            throw new NegocioException(HttpStatus.CONFLICT, "Ya tiene una reserva pendiente o está en la cola de este libro");
        }

        liberarVencidas(); // si una reserva vencio, su ejemplar vuelve antes de mirar el stock
        LocalDateTime ahora = horario.ahora();
        Libro libro = libroService.bloquearParaSalida(dto.getIdLibro());
        if (libro.getStock() == 0) {
            return entrarALaCola(prestatario, libro, ahora);
        }

        validarFechaRecojo(dto.getFechaRecojoLimite(), ahora);
        libroService.descontarStock(libro, "reservado"); // stock automatico: al reservar baja 1

        Reserva reserva = new Reserva();
        reserva.setLibro(libro);
        reserva.setPrestatario(prestatario);
        reserva.setFechaReserva(ahora);
        reserva.setFechaRecojoLimite(dto.getFechaRecojoLimite());
        reserva.setEstadoReserva(PENDIENTE);
        reserva.setPosicionCola(1);

        Reserva guardada = reservaDao.save(reserva);
        log.info("Reserva #{} creada: libro '{}' para usuario {}, recojo hasta {}",
                guardada.getIdReserva(), libro.getTitulo(), prestatario.getIdUsuario(), dto.getFechaRecojoLimite());
        return ReservaRespuestaDTO.desde(guardada);
    }

    /** Sin ejemplares: la reserva no aparta nada, queda 'en_espera' con su puesto en la cola y sin fecha de recojo. */
    // cola de espera (RF04): la fecha de recojo se pone recien cuando le toque un ejemplar (ver ColaEsperaService)
    private ReservaRespuestaDTO entrarALaCola(Usuario prestatario, Libro libro, LocalDateTime ahora) {
        Reserva reserva = new Reserva();
        reserva.setLibro(libro);
        reserva.setPrestatario(prestatario);
        reserva.setFechaReserva(ahora);
        reserva.setFechaRecojoLimite(null);
        reserva.setEstadoReserva(EN_ESPERA);
        reserva.setPosicionCola(colaEspera.siguientePosicion(libro.getIdLibro()));

        Reserva guardada = reservaDao.save(reserva);
        log.info("Reserva #{} en cola de espera: libro '{}' para usuario {}, posición {}",
                guardada.getIdReserva(), libro.getTitulo(), prestatario.getIdUsuario(), guardada.getPosicionCola());
        return ReservaRespuestaDTO.desde(guardada);
    }

    /** La fecha de recojo es obligatoria si hay ejemplar: en el futuro, dentro de 7 días y en horario de atención. */
    private void validarFechaRecojo(LocalDateTime fechaRecojo, LocalDateTime ahora) {
        if (fechaRecojo == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Elija la fecha y la franja de recojo");
        }
        if (!fechaRecojo.isAfter(ahora) || fechaRecojo.isAfter(ahora.plusDays(DIAS_MAXIMOS_ANTICIPACION))) {
            throw new NegocioException(HttpStatus.BAD_REQUEST,
                    "El recojo debe ser en el futuro y dentro de los próximos " + DIAS_MAXIMOS_ANTICIPACION + " días");
        }
        // horario de atencion: la franja de recojo tiene q terminar a mas tardar al cierre (20:00)
        if (!horario.esHoraDeRecojo(fechaRecojo.toLocalTime())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "La hora de recojo debe estar dentro del horario de atención ("
                    + horario.getApertura() + " a " + horario.getCierre() + ")");
        }
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

    /** Cancela una reserva pendiente (devuelve el ejemplar) o sale de la cola de espera. */
    // reserva cancelar: tambien es logica, la reserva pasa a 'cancelada' y el ejemplar vuelve al stock
    // (o al primero de la cola). si estaba en la cola, los de atras avanzan un puesto (RF16)
    @Transactional
    public void cancelar(Integer idReserva) {
        Reserva reserva = buscar(idReserva);
        if (EN_ESPERA.equals(reserva.getEstadoReserva())) {
            reserva.setEstadoReserva("cancelada");
            reservaDao.save(reserva);
            colaEspera.reordenar(reserva.getLibro().getIdLibro()); // no tenia ejemplar: el stock no cambia
            log.info("Reserva #{} cancelada (salió de la cola de espera)", idReserva);
            return;
        }
        if (!PENDIENTE.equals(reserva.getEstadoReserva())) {
            throw new NegocioException(HttpStatus.CONFLICT, "Solo se puede cancelar una reserva pendiente o en cola de espera");
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
                idPrestatario, idLibro, PENDIENTE);
        if (reserva.isEmpty() || reserva.get().getFechaRecojoLimite().isBefore(horario.ahora())) {
            return false;
        }
        reserva.get().setEstadoReserva("atendida");
        reservaDao.save(reserva.get());
        return true;
    }

    /** ¿Alguien tiene reservado o está esperando este libro? (no se amplía un préstamo si hay reservas, RF05). */
    // ampliacion: si alguien lo reservo o esta en la cola de espera, ampliar lo haria esperar mas
    @Transactional
    public boolean hayReservasPendientes(Integer idLibro) {
        liberarVencidas(); // las que ya vencieron no cuentan
        return reservaDao.existsByLibroIdLibroAndEstadoReservaIn(idLibro, List.of(PENDIENTE, EN_ESPERA));
    }

    /** Las reservas pendientes cuya hora de recojo pasó quedan 'vencida' y liberan su ejemplar. */
    @Transactional
    public void liberarVencidas() {
        for (Reserva r : reservaDao.findByEstadoReservaAndFechaRecojoLimiteBefore(PENDIENTE, horario.ahora())) {
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
