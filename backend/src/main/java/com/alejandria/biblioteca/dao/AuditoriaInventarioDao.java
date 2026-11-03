package com.alejandria.biblioteca.dao;

import com.alejandria.biblioteca.entity.AuditoriaInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditoriaInventarioDao extends JpaRepository<AuditoriaInventario, Integer> {

    /** Auditorías del periodo, la más reciente primero, con su libro y su administrador (una sola consulta). */
    @Query("SELECT a FROM AuditoriaInventario a JOIN FETCH a.libro JOIN FETCH a.administrador "
         + "WHERE a.fechaAuditoria >= :desde AND a.fechaAuditoria < :hasta "
         + "ORDER BY a.fechaAuditoria DESC, a.idAuditoria DESC")
    List<AuditoriaInventario> listarEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    // indicadores IPI: "N total auditados" = titulos distintos auditados en el periodo (si un titulo se conto
    // dos veces, cuenta una sola vez)
    @Query(value = "SELECT COUNT(DISTINCT id_libro) FROM auditoria_inventario "
                 + "WHERE fecha_auditoria >= :desde AND fecha_auditoria < :hasta", nativeQuery = true)
    long contarAuditadosEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    // indicadores IPI: "N libros coincidentes" = titulos cuyo ULTIMO conteo del periodo coincide con el stock
    // del sistema. DISTINCT ON de postgresql se queda con la primera fila de cada libro (la mas reciente)
    @Query(value = "SELECT COUNT(*) FROM ("
                 + "  SELECT DISTINCT ON (id_libro) stock_sistema, conteo_fisico FROM auditoria_inventario "
                 + "  WHERE fecha_auditoria >= :desde AND fecha_auditoria < :hasta "
                 + "  ORDER BY id_libro, fecha_auditoria DESC, id_auditoria DESC"
                 + ") ultimo WHERE stock_sistema = conteo_fisico", nativeQuery = true)
    long contarCoincidentesEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
