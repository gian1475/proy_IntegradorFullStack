package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dto.reporte.IndicadorDTO;

import java.util.List;

/**
 * Fórmulas de los indicadores de gestión del apartado 1.2.2 del informe.
 * Son métodos sueltos (sin BD) para poder probarlos con JUnit.
 */
public final class IndicadoresGestion {

    private IndicadoresGestion() {}

    /** (parte / total) × 100 con un decimal. Si no hay datos (total 0) devuelve null: no se divide entre cero. */
    // indicadores: PDF = (N retrasos / N prestamos) x 100 y TIM = (N incidencias / N prestamos) x 100
    public static Double porcentaje(long parte, long total) {
        if (total == 0) {
            return null;
        }
        return Math.round(parte * 1000.0 / total) / 10.0;
    }

    /** La meta es un máximo (por ejemplo "5 % o menos"). Sin dato, no se sabe si cumple (null). */
    public static Boolean cumpleMetaMaxima(Double valor, double meta) {
        return valor == null ? null : valor <= meta;
    }

    /**
     * Los 4 indicadores del apartado 1.2.2 con su línea base y su meta. Los usan el dashboard (el mes actual)
     * y el reporte de indicadores (el rango de fechas elegido). periodo: "este mes", "en el periodo"...
     */
    // indicadores: PDF y TIM se calculan con los prestamos del periodo. TAP e IPI no se inventan: se dice q falta
    public static List<IndicadorDTO> calcular(long prestamos, long conRetraso, long conIncidencia, String periodo) {
        Double pdf = porcentaje(conRetraso, prestamos);
        Double tim = porcentaje(conIncidencia, prestamos);
        return List.of(
                new IndicadorDTO("TAP", "Tiempo promedio de atención por préstamo", null, "min", "6 min", "≤ 2 min", null,
                        "Para calcularlo falta guardar la hora en que empieza la atención de cada préstamo."),
                new IndicadorDTO("PDF", "Porcentaje de devoluciones fuera de plazo", pdf, "%", "15 %", "≤ 5 %",
                        cumpleMetaMaxima(pdf, 5),
                        "Devoluciones con retraso: " + conRetraso + " de " + prestamos + " préstamos " + periodo + "."),
                new IndicadorDTO("IPI", "Índice de precisión del inventario", null, "%", "70 %", "≥ 95 %", null,
                        "Se calcula con la auditoría física mensual: libros que coinciden / libros auditados."),
                new IndicadorDTO("TIM", "Tasa de incidencias mensual", tim, "%", "2 %", "≤ 1 %",
                        cumpleMetaMaxima(tim, 1),
                        "Préstamos con incidencia por daño o pérdida: " + conIncidencia + " de " + prestamos + " " + periodo + ".")
        );
    }
}
