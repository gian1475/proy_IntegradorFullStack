package com.alejandria.biblioteca.dao;

import com.alejandria.biblioteca.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservaDao extends JpaRepository<Reserva, Integer> {

    long countByEstadoReserva(String estadoReserva);

    /** ¿El libro tiene reservas en ese estado? (para no darlo de baja ni ampliar su préstamo si hay pendientes). */
    boolean existsByLibroIdLibroAndEstadoReserva(Integer idLibro, String estadoReserva);

    /** ¿El libro tiene reservas en alguno de esos estados? (pendientes o en la cola de espera). */
    boolean existsByLibroIdLibroAndEstadoReservaIn(Integer idLibro, List<String> estados);

    List<Reserva> findByPrestatarioIdUsuarioOrderByFechaReservaDesc(Integer idUsuario);

    List<Reserva> findAllByOrderByFechaReservaDesc();

    /** Reserva pendiente de un prestatario para un libro (para no duplicarla y para atenderla al prestar). */
    Optional<Reserva> findFirstByPrestatarioIdUsuarioAndLibroIdLibroAndEstadoReserva(
            Integer idUsuario, Integer idLibro, String estadoReserva);

    /** ¿El prestatario ya tiene ese libro reservado o está en su cola? (no se reserva dos veces). */
    boolean existsByPrestatarioIdUsuarioAndLibroIdLibroAndEstadoReservaIn(
            Integer idUsuario, Integer idLibro, List<String> estados);

    /** cola de espera: las reservas 'en_espera' de un libro, en orden de llegada (posicion 1, 2, 3...). */
    List<Reserva> findByLibroIdLibroAndEstadoReservaOrderByPosicionColaAscFechaReservaAsc(Integer idLibro, String estadoReserva);

    /** Reporte de reservas: las hechas en el rango de fechas. */
    @Query("SELECT r FROM Reserva r WHERE r.fechaReserva >= :desde AND r.fechaReserva < :hasta ORDER BY r.fechaReserva DESC")
    List<Reserva> listarParaReporte(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Reservas pendientes cuya hora límite de recojo ya pasó. */
    List<Reserva> findByEstadoReservaAndFechaRecojoLimiteBefore(String estadoReserva, LocalDateTime fecha);
}
