package com.backend.demo.dao;

import com.backend.demo.entity.Libro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LibroDao extends JpaRepository<Libro, Integer> {

    List<Libro> findByTituloContainingIgnoreCase(String titulo);

    List<Libro> findByCategoriaNombreIgnoreCase(String categoriaNombre);

    @Query("SELECT l FROM Libro l JOIN l.autores a WHERE LOWER(a.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) OR LOWER(a.apellido) LIKE LOWER(CONCAT('%', :apellido, '%'))")
    List<Libro> findByAutorNombreOrApellido(@Param("nombre") String nombre, @Param("apellido") String apellido);

    List<Libro> findByEstadoLibroAndStockGreaterThan(String estadoLibro, Integer stock);

    /** Catálogo: todos los libros menos los dados de baja (eliminación lógica). */
    List<Libro> findByEstadoLibroNotOrderByTitulo(String estadoLibro);

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdLibroNot(String isbn, Integer idLibro);

    /** Lee el libro bloqueando su fila hasta terminar la transacción, para que dos préstamos a la vez no descuadren el stock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Libro l WHERE l.idLibro = :idLibro")
    Optional<Libro> buscarParaActualizar(@Param("idLibro") Integer idLibro);
}
