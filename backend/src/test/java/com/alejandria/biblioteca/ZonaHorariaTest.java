package com.alejandria.biblioteca;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Zona horaria: el backend usa la hora de Lima aunque la PC esté en otra zona")
class ZonaHorariaTest {

    private final TimeZone zonaOriginal = TimeZone.getDefault();

    @AfterEach
    void devolverLaZonaDeLaPc() {
        TimeZone.setDefault(zonaOriginal); // para no afectar a las demás pruebas
    }

    @Test
    void conLaPcEnHoraGmtElBackendQuedaEnHoraDeLima() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC")); // como un servidor en la nube

        BibliotecaApplication.fijarZonaHoraria();

        assertEquals(ZoneId.of("America/Lima"), ZoneId.systemDefault());
    }

    @Test
    void lasUnaDeLaMadrugadaGmtSonLasOchoDeLaNocheEnLima() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Instant momento = Instant.parse("2026-10-28T01:00:00Z");

        BibliotecaApplication.fijarZonaHoraria();

        // sin fijar la zona seria 28/10 01:00 y el prestamo quedaria "fuera de horario" en otro dia
        assertEquals(LocalDateTime.of(2026, 10, 27, 20, 0), LocalDateTime.ofInstant(momento, ZoneId.systemDefault()));
    }
}
