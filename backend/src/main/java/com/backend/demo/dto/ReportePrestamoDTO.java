package com.backend.demo.dto;

import java.time.LocalDateTime;

/** Una fila del reporte de préstamos (Excel / CSV). */
public record ReportePrestamoDTO(
        Integer idPrestamo,
        String prestatario,
        String dniPrestatario,
        String encargado,
        String tituloLibro,
        LocalDateTime fechaPrestamo,
        LocalDateTime fechaDevolucionPrevista,
        LocalDateTime fechaDevolucionReal,
        Boolean ampliado,
        String estadoFisicoDevolucion,
        String estadoPrestamo
) {
    /** Constructor que usa la consulta JPQL (recibe nombre y apellido por separado). */
    public ReportePrestamoDTO(Integer idPrestamo, String nombrePrestatario, String apellidoPrestatario,
                              String dniPrestatario, String nombreEncargado, String apellidoEncargado,
                              String tituloLibro, LocalDateTime fechaPrestamo, LocalDateTime fechaDevolucionPrevista,
                              LocalDateTime fechaDevolucionReal, Boolean ampliado, String estadoFisicoDevolucion,
                              String estadoPrestamo) {
        this(idPrestamo, nombrePrestatario + " " + apellidoPrestatario, dniPrestatario,
             nombreEncargado + " " + apellidoEncargado, tituloLibro, fechaPrestamo, fechaDevolucionPrevista,
             fechaDevolucionReal, ampliado, estadoFisicoDevolucion, estadoPrestamo);
    }
}
