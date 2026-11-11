package com.alejandria.biblioteca.dto.libro;

/**
 * Fila de la consulta de DOS tablas (libro JOIN categoria).
 * De libro: id, titulo, isbn, stock y estado. De categoria: el nombre.
 */
public record LibroCategoriaDTO(
        Integer idLibro,
        String titulo,
        String isbn,
        Integer stock,
        String estadoLibro,
        String categoria
) {}
