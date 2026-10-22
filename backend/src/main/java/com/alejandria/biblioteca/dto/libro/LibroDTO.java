package com.alejandria.biblioteca.dto.libro;

import com.alejandria.biblioteca.entity.Autor;
import com.alejandria.biblioteca.entity.Libro;

import java.util.Comparator;
import java.util.List;

/** Libro tal como lo recibe el frontend, con su categoría y sus autores ya cargados. */
public record LibroDTO(
        Integer idLibro,
        String titulo,
        String isbn,
        Integer anioPublicacion,
        String editorial,
        Integer stock,
        String estadoLibro,
        CategoriaResumen categoria,
        List<AutorResumen> autores
) {
    public record CategoriaResumen(Integer idCategoria, String nombre) {}

    public record AutorResumen(Integer idAutor, String nombre, String apellido) {}

    /** Debe llamarse dentro de una transacción, porque lee la lista LAZY de autores. */
    public static LibroDTO desde(Libro libro) {
        CategoriaResumen categoria = libro.getCategoria() == null ? null
                : new CategoriaResumen(libro.getCategoria().getIdCategoria(), libro.getCategoria().getNombre());

        // los autores se guardan en un Set (sin orden): se ordenan por id para que siempre salgan igual
        List<AutorResumen> autores = libro.getAutores().stream()
                .sorted(Comparator.comparing(Autor::getIdAutor))
                .map(a -> new AutorResumen(a.getIdAutor(), a.getNombre(), a.getApellido()))
                .toList();

        return new LibroDTO(libro.getIdLibro(), libro.getTitulo(), libro.getIsbn(), libro.getAnioPublicacion(),
                libro.getEditorial(), libro.getStock(), libro.getEstadoLibro(), categoria, autores);
    }
}
