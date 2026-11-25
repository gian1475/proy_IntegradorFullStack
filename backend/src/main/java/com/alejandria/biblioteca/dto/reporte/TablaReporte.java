package com.alejandria.biblioteca.dto.reporte;

import java.util.List;

/**
 * Un reporte listo para exportar: título, periodo, columnas, filas (todo en texto), notas aclaratorias
 * y un pie que va debajo de la tabla (por ejemplo, "8 registros eliminados de manera lógica").
 * El mismo objeto se convierte en Excel, PDF o CSV (ver ReporteService).
 */
public record TablaReporte(
        String titulo,
        String periodo,
        String[] columnas,
        List<String[]> filas,
        List<String> notas,
        List<String> pie
) {
    /** Reporte sin pie (la mayoría). */
    public TablaReporte(String titulo, String periodo, String[] columnas, List<String[]> filas, List<String> notas) {
        this(titulo, periodo, columnas, filas, notas, List.of());
    }
}
