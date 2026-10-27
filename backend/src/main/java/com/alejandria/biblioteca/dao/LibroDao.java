package com.alejandria.biblioteca.dao;

import com.alejandria.biblioteca.entity.Libro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LibroDao extends JpaRepository<Libro, Integer> {

    /** Consulta de disponibilidad (RF03): busca por título, autor, categoría o código (ISBN), sin los dados de baja. */
    // consulta disponibilidad: un solo texto busca en el titulo, el isbn (con o sin guiones), el nombre del autor
    // y la categoria. el resultado trae el stock y el estado del libro
    @Query("SELECT DISTINCT l FROM Libro l LEFT JOIN l.autores a LEFT JOIN l.categoria c "
         + "WHERE l.estadoLibro <> 'baja' AND ("
         + "  LOWER(l.titulo) LIKE LOWER(CONCAT('%', :texto, '%')) "
         + "  OR REPLACE(l.isbn, '-', '') LIKE CONCAT('%', REPLACE(:texto, '-', ''), '%') "
         + "  OR LOWER(CONCAT(a.nombre, ' ', a.apellido)) LIKE LOWER(CONCAT('%', :texto, '%')) "
         + "  OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))) "
         + "ORDER BY l.titulo")
    List<Libro> buscarEnCatalogo(@Param("texto") String texto);

    @Query("SELECT l FROM Libro l JOIN l.autores a WHERE LOWER(a.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) OR LOWER(a.apellido) LIKE LOWER(CONCAT('%', :apellido, '%'))")
    List<Libro> findByAutorNombreOrApellido(@Param("nombre") String nombre, @Param("apellido") String apellido);

    /** Catálogo: todos los libros menos los dados de baja (eliminación lógica). */
    // crud libro consultar: SELECT de la tabla libro donde el estado no es 'baja', ordenado por titulo
    List<Libro> findByEstadoLibroNotOrderByTitulo(String estadoLibro);

    /** Dashboard: por cada estado, cuántos títulos y cuántos ejemplares disponibles (sin los de baja). Fila: [estado, titulos, stock]. */
    @Query(value = "SELECT estado_libro, COUNT(*), COALESCE(SUM(stock), 0) FROM libro "
                 + "WHERE estado_libro <> 'baja' GROUP BY estado_libro ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> contarPorEstado();

    /** Reporte de bajas lógicas: los libros con estado 'baja'. */
    List<Libro> findByEstadoLibroOrderByTitulo(String estadoLibro);

    /**
     * Reporte de stock por categoría (sin los dados de baja). Cada fila:
     * [categoria, titulos, ejemplares disponibles, titulos sin stock, titulos en mantenimiento].
     */
    @Query(value = "SELECT c.nombre, COUNT(l.id_libro), COALESCE(SUM(l.stock), 0), "
                 + "  COUNT(l.id_libro) FILTER (WHERE l.stock = 0), "
                 + "  COUNT(l.id_libro) FILTER (WHERE l.estado_libro = 'mantenimiento') "
                 + "FROM categoria c "
                 + "LEFT JOIN libro l ON l.id_categoria = c.id_categoria AND l.estado_libro <> 'baja' "
                 + "GROUP BY c.nombre ORDER BY c.nombre", nativeQuery = true)
    List<Object[]> stockPorCategoria();

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdLibroNot(String isbn, Integer idLibro);

    /** Lee el libro bloqueando su fila hasta terminar la transacción, para que dos préstamos a la vez no descuadren el stock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Libro l WHERE l.idLibro = :idLibro")
    Optional<Libro> buscarParaActualizar(@Param("idLibro") Integer idLibro);
}
