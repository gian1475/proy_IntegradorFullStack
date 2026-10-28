package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.exception.NegocioException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Rango de fechas de un filtro (historial y reportes). Las dos fechas son opcionales y se incluyen completas:
 * "hasta el 27/10" significa hasta el 27/10 a las 23:59.
 */
// filtro de fechas: "desde" empieza a las 00:00 y "hasta" termina a las 00:00 del dia siguiente.
// postgresql no acepta una fecha en null dentro de la consulta, asi q sin fecha se usa un limite muy lejano
public record RangoFechas(LocalDate desde, LocalDate hasta) {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final LocalDateTime SIN_INICIO = LocalDateTime.of(1900, 1, 1, 0, 0);
    private static final LocalDateTime SIN_FIN = LocalDateTime.of(3000, 1, 1, 0, 0);

    public RangoFechas {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "La fecha 'desde' no puede ser posterior a la fecha 'hasta'");
        }
    }

    /** Primer momento incluido. Sin "desde" se usa el año 1900 (no se filtra). */
    public LocalDateTime inicio() {
        return desde == null ? SIN_INICIO : desde.atStartOfDay();
    }

    /** Primer momento que ya NO se incluye. Sin "hasta" se usa el año 3000 (no se filtra). */
    public LocalDateTime fin() {
        return hasta == null ? SIN_FIN : hasta.plusDays(1).atStartOfDay();
    }

    /** Texto para el encabezado de los reportes. */
    public String texto() {
        if (desde == null && hasta == null) {
            return "Todas las fechas";
        }
        if (desde == null) {
            return "Hasta el " + hasta.format(FORMATO);
        }
        if (hasta == null) {
            return "Desde el " + desde.format(FORMATO);
        }
        return "Del " + desde.format(FORMATO) + " al " + hasta.format(FORMATO);
    }
}
