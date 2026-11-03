package com.alejandria.biblioteca.dao;

import com.alejandria.biblioteca.dto.incidencia.IncidenciaDTO;
import com.alejandria.biblioteca.entity.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IncidenciaDao extends JpaRepository<Incidencia, Integer> {

    /** ¿El préstamo ya tiene una incidencia de ese tipo? (no se registra dos veces el mismo retraso) */
    boolean existsByPrestamoIdPrestamoAndTipoIncidencia(Integer idPrestamo, String tipoIncidencia);

    /** Todas las incidencias con su préstamo y su sanción, de la más reciente a la más antigua. */
    // incidencias (RF06): JOIN de incidencia con prestamo, usuario y libro, y LEFT JOIN con sancion
    // (en la bd una incidencia puede no tener sancion; las que registra el encargado siempre tienen)
    @Query("SELECT new com.alejandria.biblioteca.dto.incidencia.IncidenciaDTO("
         + "  i.idIncidencia, p.idPrestamo, CONCAT(u.nombre, ' ', u.apellido), u.dni, l.titulo, "
         + "  i.tipoIncidencia, i.descripcion, i.fechaIncidencia, i.estadoIncidencia, "
         + "  s.idSancion, s.tipoSancion, s.monto, s.descripcion, s.fechaInicio, s.fechaFin, s.estadoSancion) "
         + "FROM Incidencia i "
         + "JOIN i.prestamo p "
         + "JOIN p.prestatario u "
         + "JOIN p.libro l "
         + "LEFT JOIN Sancion s ON s.incidencia = i "
         + "ORDER BY i.fechaIncidencia DESC, i.idIncidencia DESC")
    List<IncidenciaDTO> listarConSancion();
}
