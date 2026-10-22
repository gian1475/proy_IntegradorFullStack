package com.alejandria.biblioteca.dto.reporte;

import java.time.LocalDateTime;
import java.util.List;

/** Todo lo que muestra el dashboard del administrador. */
public record DashboardDTO(
        LocalDateTime generadoEn,
        String mes,
        // Catálogo
        long titulos,
        long ejemplaresDisponibles,
        List<Conteo> librosPorEstado,
        // Hoy
        long prestamosActivos,
        long prestamosVencidos,
        long reservasPendientes,
        long prestatarios,
        // Este mes
        long prestamosMes,
        long devolucionesMes,
        long devolucionesConRetrasoMes,
        long devolucionesDanadasMes,
        List<IndicadorDTO> indicadores,
        List<Conteo> prestamosPorCategoria,
        List<Conteo> prestamosUltimos7Dias
) {
    /** Un nombre con su cantidad (para las barras del dashboard). */
    public record Conteo(String nombre, long cantidad) {}
}
