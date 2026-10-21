package com.backend.demo.dao;

import com.backend.demo.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaDao extends JpaRepository<Reserva, Integer> {
    List<Reserva> findByPrestatarioIdUsuario(Integer idUsuario);
    List<Reserva> findByEstadoReserva(String estadoReserva);

    List<Reserva> findByPrestatarioIdUsuarioOrderByFechaReservaDesc(Integer idUsuario);

    List<Reserva> findAllByOrderByFechaReservaDesc();

    /** Reserva pendiente de un prestatario para un libro (para no duplicarla y para atenderla al prestar). */
    Optional<Reserva> findFirstByPrestatarioIdUsuarioAndLibroIdLibroAndEstadoReserva(
            Integer idUsuario, Integer idLibro, String estadoReserva);

    /** Reservas pendientes cuya hora límite de recojo ya pasó. */
    List<Reserva> findByEstadoReservaAndFechaRecojoLimiteBefore(String estadoReserva, LocalDateTime fecha);
}
