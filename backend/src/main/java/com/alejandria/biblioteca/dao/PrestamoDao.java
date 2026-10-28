package com.alejandria.biblioteca.dao;

import com.alejandria.biblioteca.dto.prestamo.PrestamoDetalleDTO;
import com.alejandria.biblioteca.dto.reporte.ReportePrestamoDTO;
import com.alejandria.biblioteca.entity.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface PrestamoDao extends JpaRepository<Prestamo, Integer> {

    long countByEstadoPrestamo(String estadoPrestamo);

    /** ¿El libro tiene préstamos que aún no se devuelven? (para no darlo de baja). */
    boolean existsByLibroIdLibroAndEstadoPrestamoIn(Integer idLibro, List<String> estados);

    /** Consulta de UNA tabla: historial de préstamos de un prestatario. */
    // consulta una tabla: solo usa la tabla prestamo, filtrado por el prestatario.
    // trae todos los campos (SELECT *): id, prestatario, encargado, libro, fecha de inicio, hora limite,
    // fecha de devolucion real, ampliado, estado fisico y estado del prestamo
    @Query(value = "SELECT * FROM prestamo WHERE id_prestatario = :idPrestatario ORDER BY fecha_prestamo DESC",
           nativeQuery = true)
    List<Prestamo> buscarPorPrestatario(@Param("idPrestatario") Integer idPrestatario);

    /** Consulta con JOIN: préstamos activos con el nombre del prestatario y el título del libro. */
    // consulta dos tablas (JOIN): la tabla prestamo se une con usuario y con libro.
    //   de prestamo: id, fecha de inicio, hora limite, ampliado y estado
    //   de usuario: nombre, apellido y dni del prestatario
    //   de libro: el titulo
    // solo trae los prestamos q siguen en curso (activo o vencido)
    @Query("SELECT new com.alejandria.biblioteca.dto.prestamo.PrestamoDetalleDTO("
         + "  p.idPrestamo, u.nombre, u.apellido, u.dni, l.titulo, "
         + "  p.fechaPrestamo, p.fechaDevolucionPrevista, p.ampliado, p.estadoPrestamo) "
         + "FROM Prestamo p "
         + "JOIN p.prestatario u "
         + "JOIN p.libro l "
         + "WHERE p.estadoPrestamo IN ('activo', 'vencido') "
         + "ORDER BY p.fechaDevolucionPrevista")
    List<PrestamoDetalleDTO> listarActivosConDetalle();

    /** Historial (RF07): préstamos con prestatario, encargado y libro (JOIN de cuatro tablas), con filtros opcionales. */
    // historial: filtra por prestatario (dni), por libro (parte del titulo o isbn) y por rango de fechas del prestamo.
    // dni o libro en null no se aplican. trae todos los estados, tambien los anulados (eliminacion logica)
    @Query("SELECT new com.alejandria.biblioteca.dto.reporte.ReportePrestamoDTO("
         + "  p.idPrestamo, u.nombre, u.apellido, u.dni, e.nombre, e.apellido, l.titulo, "
         + "  p.fechaPrestamo, p.fechaDevolucionPrevista, p.fechaDevolucionReal, "
         + "  p.ampliado, p.estadoFisicoDevolucion, p.estadoPrestamo) "
         + "FROM Prestamo p "
         + "JOIN p.prestatario u "
         + "JOIN p.encargado e "
         + "JOIN p.libro l "
         + "WHERE (:dni IS NULL OR u.dni = :dni) "
         + "  AND (:libro IS NULL OR LOWER(l.titulo) LIKE LOWER(CONCAT('%', :libro, '%')) OR l.isbn = :libro) "
         + "  AND p.fechaPrestamo >= :desde AND p.fechaPrestamo < :hasta "
         + "ORDER BY p.fechaPrestamo DESC")
    List<ReportePrestamoDTO> buscarHistorial(@Param("dni") String dni, @Param("libro") String libro,
                                             @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Reporte de préstamos: la misma consulta del historial, solo con el rango de fechas. */
    // reporte prestamos: esta consulta es la q usan el excel (poi), el pdf (openpdf) y el csv
    default List<ReportePrestamoDTO> listarParaReporte(LocalDateTime desde, LocalDateTime hasta) {
        return buscarHistorial(null, null, desde, hasta);
    }

    /** Reporte de devoluciones: préstamos devueltos, filtrados por la fecha en que se devolvieron. */
    @Query("SELECT new com.alejandria.biblioteca.dto.reporte.ReportePrestamoDTO("
         + "  p.idPrestamo, u.nombre, u.apellido, u.dni, e.nombre, e.apellido, l.titulo, "
         + "  p.fechaPrestamo, p.fechaDevolucionPrevista, p.fechaDevolucionReal, "
         + "  p.ampliado, p.estadoFisicoDevolucion, p.estadoPrestamo) "
         + "FROM Prestamo p JOIN p.prestatario u JOIN p.encargado e JOIN p.libro l "
         + "WHERE p.estadoPrestamo = 'devuelto' "
         + "  AND p.fechaDevolucionReal >= :desde AND p.fechaDevolucionReal < :hasta "
         + "ORDER BY p.fechaDevolucionReal DESC")
    List<ReportePrestamoDTO> listarDevoluciones(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /**
     * Reporte de incidencias y sanciones (RF06 las registra). Cada fila: [id incidencia, id prestamo, prestatario,
     * dni, libro, tipo, descripcion, fecha, estado, tipo sancion, monto, inicio sancion, fin sancion, estado sancion].
     */
    // reporte incidencias: incidencia + su sancion (LEFT JOIN: puede no tener sancion) + prestamo, usuario y libro
    @Query(value = "SELECT i.id_incidencia, p.id_prestamo, u.nombre || ' ' || u.apellido, u.dni, l.titulo, "
                 + "  i.tipo_incidencia, i.descripcion, i.fecha_incidencia, i.estado_incidencia, "
                 + "  s.tipo_sancion, s.monto, s.fecha_inicio, s.fecha_fin, s.estado_sancion "
                 + "FROM incidencia i "
                 + "JOIN prestamo p ON p.id_prestamo = i.id_prestamo "
                 + "JOIN usuario u ON u.id_usuario = p.id_prestatario "
                 + "JOIN libro l ON l.id_libro = p.id_libro "
                 + "LEFT JOIN sancion s ON s.id_incidencia = i.id_incidencia "
                 + "WHERE i.fecha_incidencia >= :desde AND i.fecha_incidencia < :hasta "
                 + "ORDER BY i.fecha_incidencia DESC", nativeQuery = true)
    List<Object[]> listarIncidenciasConSancion(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /**
     * Reporte de tiempos: duración de los préstamos devueltos (de la entrega a la devolución), por categoría.
     * Cada fila: [categoria, cantidad, minimo, promedio, maximo] (en minutos).
     */
    // reporte tiempos: minimo, promedio y maximo q el libro estuvo prestado en sala. EXTRACT(EPOCH) da segundos
    @Query(value = "SELECT c.nombre, COUNT(*), "
                 + "  MIN(EXTRACT(EPOCH FROM (p.fecha_devolucion_real - p.fecha_prestamo)) / 60), "
                 + "  AVG(EXTRACT(EPOCH FROM (p.fecha_devolucion_real - p.fecha_prestamo)) / 60), "
                 + "  MAX(EXTRACT(EPOCH FROM (p.fecha_devolucion_real - p.fecha_prestamo)) / 60) "
                 + "FROM prestamo p "
                 + "JOIN libro l ON l.id_libro = p.id_libro "
                 + "JOIN categoria c ON c.id_categoria = l.id_categoria "
                 + "WHERE p.estado_prestamo = 'devuelto' AND p.fecha_devolucion_real IS NOT NULL "
                 + "  AND p.fecha_prestamo >= :desde AND p.fecha_prestamo < :hasta "
                 + "GROUP BY c.nombre ORDER BY c.nombre", nativeQuery = true)
    List<Object[]> tiemposPorCategoria(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    // ---------- Dashboard: conteos del mes (las fechas "desde" y "hasta" las manda java, hora de lima) ----------

    /** Préstamos del periodo (sin los anulados): es el "N préstamos" de los indicadores. */
    @Query(value = "SELECT COUNT(*) FROM prestamo WHERE estado_prestamo <> 'anulado' "
                 + "AND fecha_prestamo >= :desde AND fecha_prestamo < :hasta", nativeQuery = true)
    long contarPrestamosEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query(value = "SELECT COUNT(*) FROM prestamo WHERE estado_prestamo = 'devuelto' "
                 + "AND fecha_prestamo >= :desde AND fecha_prestamo < :hasta", nativeQuery = true)
    long contarDevueltosEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** indicadores PDF: devueltos después de la hora límite (las 3 horas, o 4 si se amplió). */
    @Query(value = "SELECT COUNT(*) FROM prestamo WHERE estado_prestamo = 'devuelto' "
                 + "AND fecha_devolucion_real > fecha_devolucion_prevista "
                 + "AND fecha_prestamo >= :desde AND fecha_prestamo < :hasta", nativeQuery = true)
    long contarDevueltosConRetrasoEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query(value = "SELECT COUNT(*) FROM prestamo WHERE estado_fisico_devolucion = 'dañado' "
                 + "AND fecha_prestamo >= :desde AND fecha_prestamo < :hasta", nativeQuery = true)
    long contarDevueltosDanadosEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** indicadores TIM: préstamos que terminaron con una incidencia por daño o pérdida. */
    @Query(value = "SELECT COUNT(DISTINCT p.id_prestamo) FROM prestamo p "
                 + "JOIN incidencia i ON i.id_prestamo = p.id_prestamo "
                 + "WHERE i.tipo_incidencia IN ('daño', 'pérdida') "
                 + "AND p.fecha_prestamo >= :desde AND p.fecha_prestamo < :hasta", nativeQuery = true)
    long contarPrestamosConIncidenciaEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Préstamos del periodo por categoría (JOIN prestamo + libro + categoria). Cada fila: [categoria, cantidad]. */
    @Query(value = "SELECT c.nombre, COUNT(p.id_prestamo) FROM categoria c "
                 + "LEFT JOIN libro l ON l.id_categoria = c.id_categoria "
                 + "LEFT JOIN prestamo p ON p.id_libro = l.id_libro AND p.estado_prestamo <> 'anulado' "
                 + "     AND p.fecha_prestamo >= :desde AND p.fecha_prestamo < :hasta "
                 + "GROUP BY c.nombre ORDER BY COUNT(p.id_prestamo) DESC, c.nombre", nativeQuery = true)
    List<Object[]> prestamosPorCategoriaEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Préstamos por día desde una fecha. Cada fila: [fecha, cantidad]. */
    @Query(value = "SELECT CAST(fecha_prestamo AS DATE), COUNT(*) FROM prestamo "
                 + "WHERE estado_prestamo <> 'anulado' AND fecha_prestamo >= :desde "
                 + "GROUP BY CAST(fecha_prestamo AS DATE)", nativeQuery = true)
    List<Object[]> prestamosPorDiaDesde(@Param("desde") LocalDateTime desde);

    /** Pasa a 'vencido' los préstamos activos cuya hora límite ya pasó. */
    // 3 horas: si ya paso la hora limite el prestamo pasa solo a 'vencido'.
    // hora de lima: la hora "ahora" la manda java. no se usa NOW() xq la bd de neon esta en hora GMT
    // (5 horas adelante) y todos los prestamos salian vencidos apenas se registraban
    @Modifying
    @Query(value = "UPDATE prestamo SET estado_prestamo = 'vencido' "
                 + "WHERE estado_prestamo = 'activo' AND fecha_devolucion_prevista < :ahora",
           nativeQuery = true)
    int marcarVencidos(@Param("ahora") LocalDateTime ahora);

    /** ¿El prestatario tiene alguna sanción activa? (sancion -> incidencia -> prestamo) */
    // sancion activa: a alguien con sancion activa no se le presta ni puede reservar.
    // hora de lima: "hoy" lo manda java por lo mismo de arriba (CURRENT_DATE de la bd ya es mañana desde las 19:00)
    @Query(value = "SELECT EXISTS ("
                 + "  SELECT 1 FROM sancion s "
                 + "  JOIN incidencia i ON i.id_incidencia = s.id_incidencia "
                 + "  JOIN prestamo p ON p.id_prestamo = i.id_prestamo "
                 + "  WHERE p.id_prestatario = :idPrestatario "
                 + "    AND s.estado_sancion = 'activa' "
                 + "    AND (s.fecha_fin IS NULL OR s.fecha_fin >= :hoy))",
           nativeQuery = true)
    boolean tieneSancionActiva(@Param("idPrestatario") Integer idPrestatario, @Param("hoy") LocalDate hoy);
}
