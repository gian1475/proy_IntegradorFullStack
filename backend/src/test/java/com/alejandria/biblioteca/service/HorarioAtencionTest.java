package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.exception.NegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Horario de atención: de 8:00 a 20:00 (RF01 y RF05)")
class HorarioAtencionTest {

    private static final LocalDateTime HOY_10 = LocalDateTime.of(2026, 10, 28, 10, 0);

    private final HorarioAtencion horario = horario(HOY_10);

    @Test
    void dentroDelHorarioNoPasaNada() {
        assertDoesNotThrow(() -> horario.validarAbierto(HOY_10));
        assertDoesNotThrow(() -> horario.validarAbierto(HOY_10.withHour(8)));
        assertDoesNotThrow(() -> horario.validarAbierto(HOY_10.withHour(19).withMinute(59)));
    }

    @Test
    void antesDeAbrirOAlCerrarNoSeAtiende() {
        NegocioException error = assertThrows(NegocioException.class, () -> horario.validarAbierto(HOY_10.withHour(20)));
        assertEquals(409, error.getEstado().value());
        assertTrue(error.getMessage().contains("08:00 a 20:00"));
        assertThrows(NegocioException.class, () -> horario.validarAbierto(HOY_10.withHour(7).withMinute(59)));
        assertThrows(NegocioException.class, () -> horario.validarAbierto(HOY_10.withHour(22)));
    }

    @Test
    void unaHoraAntesDelCierreNoSeCambia() {
        LocalDateTime limite = HOY_10.withHour(13);
        assertEquals(limite, horario.limitarAlCierre(limite));
    }

    @Test
    void unaHoraDespuesDelCierreQuedaEnElCierre() {
        // prestamo a las 18:30: 18:30 + 3 h = 21:30, pero la biblioteca cierra a las 20:00
        assertEquals(HOY_10.withHour(20), horario.limitarAlCierre(HOY_10.withHour(21).withMinute(30)));
    }

    @Test
    void elRecojoDeUnaReservaDebeSerEnHorario() {
        assertTrue(horario.esHoraDeRecojo(LocalTime.of(11, 30)));
        assertTrue(horario.esHoraDeRecojo(LocalTime.of(20, 0))); // fin de la ultima franja
        assertFalse(horario.esHoraDeRecojo(LocalTime.of(8, 0)));
        assertFalse(horario.esHoraDeRecojo(LocalTime.of(21, 0)));
    }

    @Test
    void elCierreDeManana() {
        assertEquals(LocalDateTime.of(2026, 10, 29, 20, 0), horario.cierreDeManana());
    }

    /** Horario de 8:00 a 20:00 con un reloj fijo en la hora indicada (hora de Lima). */
    static HorarioAtencion horario(LocalDateTime ahora) {
        ZoneId lima = ZoneId.of("America/Lima");
        Clock reloj = Clock.fixed(ahora.atZone(lima).toInstant(), lima);
        return new HorarioAtencion(LocalTime.of(8, 0), LocalTime.of(20, 0), reloj);
    }
}
