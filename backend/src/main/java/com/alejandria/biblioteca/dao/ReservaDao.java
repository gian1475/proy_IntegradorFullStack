package com.alejandria.biblioteca.dao;

import com.alejandria.biblioteca.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservaDao extends JpaRepository<Reserva, Integer> {

    long countByEstadoReserva(String estadoReserva);

    /** ¿El libro tiene reservas en ese estado? (para no darlo de baja ni ampliar su préstamo si hay pendientes). */
    boolean existsByLibroIdLibroAndEstadoReserva(Integer idLibro, String estadoReserva);

    List<Reserva> findByPrestatarioIdUsuarioOrderByFechaReservaDesc(Integer idUsuario);

    List<Reserva> findAllByOrderByFechaReservaDesc();

    /** Reserva pendiente de un prestatario para un libro (para no duplicarla y para atenderla al prestar). */
    Optional<Reserva> findFirstByPrestatarioIdUsuarioAndLibroIdLibroAndEstadoReserva(
            Integer idUsuario, Integer idLibro, String estadoReserva);

    /** Reservas pendientes cuya hora límite de recojo ya pasó. */
    List<Reserva> findByEstadoReservaAndFechaRecojoLimiteBefore(String estadoReserva, LocalDateTime fecha);
}
