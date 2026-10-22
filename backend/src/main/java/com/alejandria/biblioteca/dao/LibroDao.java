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

    List<Libro> findByTituloContainingIgnoreCase(String titulo);

    @Query("SELECT l FROM Libro l JOIN l.autores a WHERE LOWER(a.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) OR LOWER(a.apellido) LIKE LOWER(CONCAT('%', :apellido, '%'))")
    List<Libro> findByAutorNombreOrApellido(@Param("nombre") String nombre, @Param("apellido") String apellido);

    /** Catálogo: todos los libros menos los dados de baja (eliminación lógica). */
    // crud libro consultar: SELECT de la tabla libro donde el estado no es 'baja', ordenado por titulo
    List<Libro> findByEstadoLibroNotOrderByTitulo(String estadoLibro);

    /** Dashboard: por cada estado, cuántos títulos y cuántos ejemplares disponibles (sin los de baja). Fila: [estado, titulos, stock]. */
    @Query(value = "SELECT estado_libro, COUNT(*), COALESCE(SUM(stock), 0) FROM libro "
                 + "WHERE estado_libro <> 'baja' GROUP BY estado_libro ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> contarPorEstado();

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdLibroNot(String isbn, Integer idLibro);

    /** Lee el libro bloqueando su fila hasta terminar la transacción, para que dos préstamos a la vez no descuadren el stock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Libro l WHERE l.idLibro = :idLibro")
    Optional<Libro> buscarParaActualizar(@Param("idLibro") Integer idLibro);
}
