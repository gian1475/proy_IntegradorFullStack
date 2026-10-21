package com.backend.demo.dao;

import com.backend.demo.dto.PrestamoDetalleDTO;
import com.backend.demo.entity.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrestamoDao extends JpaRepository<Prestamo, Integer> {

    /** ¿El libro tiene préstamos que aún no se devuelven? (para no darlo de baja). */
    boolean existsByLibroIdLibroAndEstadoPrestamoIn(Integer idLibro, List<String> estados);

    /** Consulta de UNA tabla: historial de préstamos de un prestatario. */
    @Query(value = "SELECT * FROM prestamo WHERE id_prestatario = :idPrestatario ORDER BY fecha_prestamo DESC",
           nativeQuery = true)
    List<Prestamo> buscarPorPrestatario(@Param("idPrestatario") Integer idPrestatario);

    /** Consulta con JOIN: préstamos activos con el nombre del prestatario y el título del libro. */
    @Query("SELECT new com.backend.demo.dto.PrestamoDetalleDTO("
         + "  p.idPrestamo, u.nombre, u.apellido, u.dni, l.titulo, "
         + "  p.fechaPrestamo, p.fechaDevolucionPrevista, p.ampliado, p.estadoPrestamo) "
         + "FROM Prestamo p "
         + "JOIN p.prestatario u "
         + "JOIN p.libro l "
         + "WHERE p.estadoPrestamo IN ('activo', 'vencido') "
         + "ORDER BY p.fechaDevolucionPrevista")
    List<PrestamoDetalleDTO> listarActivosConDetalle();

    /** Pasa a 'vencido' los préstamos activos cuya hora límite ya pasó. */
    @Modifying
    @Query(value = "UPDATE prestamo SET estado_prestamo = 'vencido' "
                 + "WHERE estado_prestamo = 'activo' AND fecha_devolucion_prevista < NOW()",
           nativeQuery = true)
    int marcarVencidos();

    /** ¿El prestatario tiene alguna sanción activa? (sancion -> incidencia -> prestamo) */
    @Query(value = "SELECT EXISTS ("
                 + "  SELECT 1 FROM sancion s "
                 + "  JOIN incidencia i ON i.id_incidencia = s.id_incidencia "
                 + "  JOIN prestamo p ON p.id_prestamo = i.id_prestamo "
                 + "  WHERE p.id_prestatario = :idPrestatario "
                 + "    AND s.estado_sancion = 'activa' "
                 + "    AND (s.fecha_fin IS NULL OR s.fecha_fin >= CURRENT_DATE))",
           nativeQuery = true)
    boolean tieneSancionActiva(@Param("idPrestatario") Integer idPrestatario);
}
