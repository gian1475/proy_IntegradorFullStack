package com.alejandria.biblioteca.dto.reporte;

import java.util.List;

/**
 * Un reporte listo para exportar: título, periodo, columnas, filas (todo en texto) y notas aclaratorias.
 * El mismo objeto se convierte en Excel, PDF o CSV (ver ReporteService).
 */
public record TablaReporte(
        String titulo,
        String periodo,
        String[] columnas,
        List<String[]> filas,
        List<String> notas
) {}
