package com.backend.demo.dao;

import com.backend.demo.entity.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LibroDao extends JpaRepository<Libro, Integer> {

    List<Libro> findByTituloContainingIgnoreCase(String titulo);

    List<Libro> findByCategoriaNombreIgnoreCase(String categoriaNombre);

    @Query("SELECT l FROM Libro l JOIN l.autores a WHERE LOWER(a.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) OR LOWER(a.apellido) LIKE LOWER(CONCAT('%', :apellido, '%'))")
    List<Libro> findByAutorNombreOrApellido(@Param("nombre") String nombre, @Param("apellido") String apellido);

    List<Libro> findByEstadoLibroAndStockGreaterThan(String estadoLibro, Integer stock);
}
