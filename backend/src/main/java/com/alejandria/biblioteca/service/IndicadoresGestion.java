package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.AuditoriaInventarioDao;
import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dto.reporte.IndicadorDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fórmulas de los indicadores de gestión del apartado 1.2.2 del informe.
 * Son métodos sueltos (sin BD) para poder probarlos con JUnit; los datos los junta Datos.leer.
 */
public final class IndicadoresGestion {

    private IndicadoresGestion() {}

    /**
     * Lo que hace falta para los 4 indicadores de un periodo.
     * tapMinutos: promedio de (confirmación - inicio de la atención) en minutos, null si ningún préstamo lo tiene.
     */
    public record Datos(long prestamos, long conRetraso, long conIncidencia,
                        Double tapMinutos, long prestamosConTap, long auditados, long coinciden) {

        /** Lee de la BD los datos del periodo [desde, hasta). Lo usan el dashboard y el reporte de indicadores. */
        public static Datos leer(PrestamoDao prestamoDao, AuditoriaInventarioDao auditoriaDao,
                                 LocalDateTime desde, LocalDateTime hasta) {
            return new Datos(
                    prestamoDao.contarPrestamosEntre(desde, hasta),
                    prestamoDao.contarDevueltosConRetrasoEntre(desde, hasta),
                    prestamoDao.contarPrestamosConIncidenciaEntre(desde, hasta),
                    prestamoDao.promedioMinutosAtencionEntre(desde, hasta),
                    prestamoDao.contarConInicioAtencionEntre(desde, hasta),
                    auditoriaDao.contarAuditadosEntre(desde, hasta),
                    auditoriaDao.contarCoincidentesEntre(desde, hasta));
        }
    }

    /** (parte / total) × 100 con un decimal. Si no hay datos (total 0) devuelve null: no se divide entre cero. */
    // indicadores: PDF = (N retrasos / N prestamos) x 100, TIM = (N incidencias / N prestamos) x 100
    // e IPI = (N libros coincidentes / N total auditados) x 100
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

    /** La meta es un mínimo (IPI: "95 % o más"). Sin dato, no se sabe si cumple (null). */
    public static Boolean cumpleMetaMinima(Double valor, double meta) {
        return valor == null ? null : valor >= meta;
    }

    /**
     * Los 4 indicadores del apartado 1.2.2 con su línea base y su meta. Los usan el dashboard (el mes actual)
     * y el reporte de indicadores (el rango de fechas elegido). periodo: "este mes", "en el periodo"...
     */
    // indicadores: si falta informacion el valor queda vacio ("Sin dato"), no se inventa
    public static List<IndicadorDTO> calcular(Datos d, String periodo) {
        Double pdf = porcentaje(d.conRetraso(), d.prestamos());
        Double tim = porcentaje(d.conIncidencia(), d.prestamos());
        // indicadores TAP: promedio en minutos con un decimal, solo de los prestamos q tienen la hora de inicio
        Double tap = d.prestamosConTap() == 0 || d.tapMinutos() == null ? null : Math.round(d.tapMinutos() * 10) / 10.0;
        Double ipi = porcentaje(d.coinciden(), d.auditados());
        return List.of(
                new IndicadorDTO("TAP", "Tiempo promedio de atención por préstamo", tap, "min", "6 min", "≤ 2 min",
                        cumpleMetaMaxima(tap, 2),
                        tap == null
                                ? "Ningún préstamo " + periodo + " tiene la hora de inicio de la atención."
                                : "Promedio de " + d.prestamosConTap() + " préstamos con la hora de inicio de la atención " + periodo + "."),
                new IndicadorDTO("PDF", "Porcentaje de devoluciones fuera de plazo", pdf, "%", "15 %", "≤ 5 %",
                        cumpleMetaMaxima(pdf, 5),
                        "Devoluciones con retraso: " + d.conRetraso() + " de " + d.prestamos() + " préstamos " + periodo + "."),
                new IndicadorDTO("IPI", "Índice de precisión del inventario", ipi, "%", "70 %", "≥ 95 %",
                        cumpleMetaMinima(ipi, 95),
                        ipi == null
                                ? "No hay auditorías físicas del inventario " + periodo + "."
                                : "Títulos que coinciden con el conteo físico: " + d.coinciden() + " de " + d.auditados() + " auditados " + periodo + "."),
                new IndicadorDTO("TIM", "Tasa de incidencias mensual", tim, "%", "2 %", "≤ 1 %",
                        cumpleMetaMaxima(tim, 1),
                        "Préstamos con incidencia por daño o pérdida: " + d.conIncidencia() + " de " + d.prestamos() + " " + periodo + ".")
        );
    }
}
