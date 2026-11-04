package com.alejandria.biblioteca.dao;

import com.alejandria.biblioteca.entity.Libro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
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
    // tablas: libro + libro_autor + autor + categoria. SQL equivalente:
    //   SELECT DISTINCT l.* FROM libro l
    //   LEFT JOIN libro_autor la ON la.id_libro = l.id_libro LEFT JOIN autor a ON a.id_autor = la.id_autor
    //   LEFT JOIN categoria c ON c.id_categoria = l.id_categoria
    //   WHERE l.estado_libro <> 'baja' AND (titulo, isbn, nombre + apellido del autor o categoria contienen el texto)
    @Query("SELECT DISTINCT l FROM Libro l LEFT JOIN l.autores a LEFT JOIN l.categoria c "
         + "WHERE l.estadoLibro <> 'baja' AND ("
         + "  LOWER(l.titulo) LIKE LOWER(CONCAT('%', :texto, '%')) "
         + "  OR REPLACE(l.isbn, '-', '') LIKE CONCAT('%', REPLACE(:texto, '-', ''), '%') "
         + "  OR LOWER(CONCAT(a.nombre, ' ', a.apellido)) LIKE LOWER(CONCAT('%', :texto, '%')) "
         + "  OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))) "
         + "ORDER BY l.titulo")
    @EntityGraph(attributePaths = {"categoria", "autores"})
    List<Libro> buscarEnCatalogo(@Param("texto") String texto);

    /** Libros de un autor: el texto se busca en el nombre o en el apellido (por ejemplo "martin" o "robert"). */
    // D14: antes recibia el mismo texto dos veces (nombre, nombre); ahora es un solo parametro.
    // tablas: libro + libro_autor + autor (JOIN). SQL equivalente:
    //   SELECT DISTINCT l.* FROM libro l
    //   JOIN libro_autor la ON la.id_libro = l.id_libro JOIN autor a ON a.id_autor = la.id_autor
    //   WHERE LOWER(a.nombre) LIKE '%texto%' OR LOWER(a.apellido) LIKE '%texto%'
    // DISTINCT: si dos autores del mismo libro coinciden ("john": John Vlissides y Ralph Johnson), sale una vez.
    // @EntityGraph: los autores vienen en la misma consulta (sin el N+1 de D15). Los de baja los quita LibroService
    @Query("SELECT DISTINCT l FROM Libro l JOIN l.autores a "
         + "WHERE LOWER(a.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) "
         + "   OR LOWER(a.apellido) LIKE LOWER(CONCAT('%', :texto, '%')) "
         + "ORDER BY l.titulo")
    @EntityGraph(attributePaths = {"categoria", "autores"})
    List<Libro> buscarPorAutor(@Param("texto") String texto);

    /** Catálogo: todos los libros menos los dados de baja (eliminación lógica). */
    // crud libro consultar: los libros donde el estado no es 'baja', ordenados por titulo.
    // tablas: libro + categoria + libro_autor + autor (por el @EntityGraph; NO es una consulta de una tabla).
    // SQL equivalente: SELECT l.*, c.*, a.* FROM libro l LEFT JOIN categoria c ON ... LEFT JOIN libro_autor la ON ...
    //   LEFT JOIN autor a ON ... WHERE l.estado_libro <> 'baja' ORDER BY l.titulo
    // @EntityGraph: trae la categoria y los autores en la MISMA consulta (con JOIN). sin esto, hibernate pedia
    // los autores de cada libro con otra consulta (25 libros = 26 consultas a neon, unos 4 s)
    @EntityGraph(attributePaths = {"categoria", "autores"})
    List<Libro> findByEstadoLibroNotOrderByTitulo(String estadoLibro);

    /** Dashboard: por cada estado, cuántos títulos y cuántos ejemplares disponibles (sin los de baja). Fila: [estado, titulos, stock]. */
    // tabla: solo libro (consulta de una tabla, agrupada por estado_libro)
    @Query(value = "SELECT estado_libro, COUNT(*), COALESCE(SUM(stock), 0) FROM libro "
                 + "WHERE estado_libro <> 'baja' GROUP BY estado_libro ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> contarPorEstado();

    /** Reporte de bajas lógicas: los libros con estado 'baja'. */
    // consulta derivada de Spring Data (el SQL lo arma con el nombre del metodo):
    //   SELECT * FROM libro WHERE estado_libro = 'baja' ORDER BY titulo
    List<Libro> findByEstadoLibroOrderByTitulo(String estadoLibro);

    /**
     * Reporte de stock por categoría (sin los dados de baja). Cada fila:
     * [categoria, titulos, ejemplares disponibles, titulos sin stock, titulos en mantenimiento].
     */
    // tablas: categoria LEFT JOIN libro (una categoria sin libros igual sale, con 0)
    @Query(value = "SELECT c.nombre, COUNT(l.id_libro), COALESCE(SUM(l.stock), 0), "
                 + "  COUNT(l.id_libro) FILTER (WHERE l.stock = 0), "
                 + "  COUNT(l.id_libro) FILTER (WHERE l.estado_libro = 'mantenimiento') "
                 + "FROM categoria c "
                 + "LEFT JOIN libro l ON l.id_categoria = c.id_categoria AND l.estado_libro <> 'baja' "
                 + "GROUP BY c.nombre ORDER BY c.nombre", nativeQuery = true)
    List<Object[]> stockPorCategoria();

    // ISBN repetido al registrar: SELECT COUNT(*) > 0 FROM libro WHERE isbn = ?
    boolean existsByIsbn(String isbn);

    // ISBN repetido al editar (otro libro con ese ISBN): ... WHERE isbn = ? AND id_libro <> ?
    boolean existsByIsbnAndIdLibroNot(String isbn, Integer idLibro);

    /** Lee el libro bloqueando su fila hasta terminar la transacción, para que dos préstamos a la vez no descuadren el stock. */
    // SQL equivalente: SELECT * FROM libro WHERE id_libro = ? FOR UPDATE (lo comprueba ConcurrenciaStockTest)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Libro l WHERE l.idLibro = :idLibro")
    Optional<Libro> buscarParaActualizar(@Param("idLibro") Integer idLibro);
}
