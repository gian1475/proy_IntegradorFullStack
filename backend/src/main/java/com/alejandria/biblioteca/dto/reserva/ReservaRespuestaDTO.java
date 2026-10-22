package com.alejandria.biblioteca.dto.reserva;

import com.alejandria.biblioteca.entity.Autor;
import com.alejandria.biblioteca.entity.Libro;
import com.alejandria.biblioteca.entity.Reserva;

import java.time.LocalDateTime;

/** Reserva con los datos del libro y del prestatario, lista para mostrar en pantalla. */
public record ReservaRespuestaDTO(
        Integer idReserva,
        Integer idLibro,
        String tituloLibro,
        String isbnLibro,
        String autor,
        String categoria,
        Integer idPrestatario,
        String nombrePrestatario,
        LocalDateTime fechaReserva,
        LocalDateTime fechaRecojoLimite,
        String estadoReserva
) {
    /** Debe llamarse dentro de una transacción, porque lee la lista LAZY de autores del libro. */
    public static ReservaRespuestaDTO desde(Reserva r) {
        Libro libro = r.getLibro();
        String autor = libro.getAutores().stream().findFirst().map(Autor::getNombreCompleto).orElse("");
        String categoria = libro.getCategoria() != null ? libro.getCategoria().getNombre() : "";

        return new ReservaRespuestaDTO(
                r.getIdReserva(),
                libro.getIdLibro(),
                libro.getTitulo(),
                libro.getIsbn(),
                autor,
                categoria,
                r.getPrestatario().getIdUsuario(),
                r.getPrestatario().getNombre() + " " + r.getPrestatario().getApellido(),
                r.getFechaReserva(),
                r.getFechaRecojoLimite(),
                r.getEstadoReserva()
        );
    }
}
