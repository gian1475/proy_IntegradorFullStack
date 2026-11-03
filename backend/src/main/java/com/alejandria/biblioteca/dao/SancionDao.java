package com.alejandria.biblioteca.dao;

import com.alejandria.biblioteca.entity.Sancion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SancionDao extends JpaRepository<Sancion, Integer> {

    /** Suspensiones que siguen 'activa' aunque ya pasó su fecha de fin (hay que cerrarlas). */
    // fin de la suspension: "ahora" lo manda java (hora de lima), igual que en marcarVencidos
    @Query("SELECT s FROM Sancion s JOIN FETCH s.incidencia i JOIN FETCH i.prestamo p JOIN FETCH p.prestatario "
         + "WHERE s.tipoSancion = 'suspensión' AND s.estadoSancion = 'activa' AND s.fechaFin < :ahora")
    List<Sancion> suspensionesTerminadas(@Param("ahora") LocalDateTime ahora);

    /** ¿Al prestatario le queda alguna suspensión activa? Si no, deja de estar 'suspendido'. */
    @Query("SELECT COUNT(s) > 0 FROM Sancion s "
         + "WHERE s.incidencia.prestamo.prestatario.idUsuario = :idPrestatario "
         + "AND s.tipoSancion = 'suspensión' AND s.estadoSancion = 'activa'")
    boolean tieneSuspensionActiva(@Param("idPrestatario") Integer idPrestatario);
}
