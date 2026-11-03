package com.alejandria.biblioteca.dto.prestamo;

import java.time.LocalDateTime;

/** Préstamo con el nombre del prestatario y el título del libro (resultado del JOIN). */
public record PrestamoDetalleDTO(
        Integer idPrestamo,
        String nombrePrestatario,
        String dniPrestatario,
        String tituloLibro,
        LocalDateTime fechaPrestamo,
        LocalDateTime fechaDevolucionPrevista,
        Boolean ampliado,
        String estadoPrestamo
) {}
