package com.backend.demo.service;

import com.backend.demo.dao.ReservaDao;
import com.backend.demo.dao.UsuarioDao;
import com.backend.demo.dto.ReservaDTO;
import com.backend.demo.dto.ReservaRespuestaDTO;
import com.backend.demo.entity.Libro;
import com.backend.demo.entity.Reserva;
import com.backend.demo.entity.Usuario;
import com.backend.demo.exception.NegocioException;
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
        libroService.descontarStock(libro, "reservado");

        Reserva reserva = new Reserva();
        reserva.setLibro(libro);
        reserva.setPrestatario(prestatario);
        reserva.setFechaReserva(ahora);
        reserva.setFechaRecojoLimite(dto.getFechaRecojoLimite());
        reserva.setEstadoReserva("pendiente");
        reserva.setPosicionCola(1);

        return ReservaRespuestaDTO.desde(reservaDao.save(reserva));
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
    @Transactional
    public void cancelar(Integer idReserva) {
        Reserva reserva = buscar(idReserva);
        if (!"pendiente".equals(reserva.getEstadoReserva())) {
            throw new NegocioException(HttpStatus.CONFLICT, "Solo se puede cancelar una reserva pendiente");
        }
        reserva.setEstadoReserva("cancelada");
        reservaDao.save(reserva);
        libroService.reponerStock(reserva.getLibro().getIdLibro());
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

    /** Las reservas pendientes cuya hora de recojo pasó quedan 'vencida' y liberan su ejemplar. */
    @Transactional
    public void liberarVencidas() {
        for (Reserva r : reservaDao.findByEstadoReservaAndFechaRecojoLimiteBefore("pendiente", LocalDateTime.now())) {
            r.setEstadoReserva("vencida");
            reservaDao.save(r);
            libroService.reponerStock(r.getLibro().getIdLibro());
        }
    }

    private Reserva buscar(Integer idReserva) {
        return reservaDao.findById(idReserva)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
    }
}
