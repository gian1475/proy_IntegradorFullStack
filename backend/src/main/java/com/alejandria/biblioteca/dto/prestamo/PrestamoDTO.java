package com.alejandria.biblioteca.dto.prestamo;

import com.alejandria.biblioteca.entity.Prestamo;

import java.time.LocalDateTime;

/** Datos de un préstamo tal como están en la tabla prestamo (solo ids, sin nombres). */
public record PrestamoDTO(
        Integer idPrestamo,
        Integer idPrestatario,
        Integer idEncargado,
        Integer idLibro,
        LocalDateTime fechaPrestamo,
        LocalDateTime fechaDevolucionPrevista,
        LocalDateTime fechaDevolucionReal,
        Boolean ampliado,
        String estadoFisicoDevolucion,
        String estadoPrestamo
) {
    public static PrestamoDTO desde(Prestamo p) {
        return new PrestamoDTO(p.getIdPrestamo(), p.getPrestatario().getIdUsuario(), p.getEncargado().getIdUsuario(),
                p.getLibro().getIdLibro(), p.getFechaPrestamo(), p.getFechaDevolucionPrevista(),
                p.getFechaDevolucionReal(), p.getAmpliado(), p.getEstadoFisicoDevolucion(), p.getEstadoPrestamo());
    }
}
