package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.seguridad.Sesion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Sesión: se cierra a los 15 minutos sin actividad (RNF04)")
class SesionServiceTest {

    /** Reloj falso: permite adelantar el tiempo sin esperar de verdad. */
    static class RelojFalso extends Clock {
        private Instant ahora = Instant.parse("2026-10-28T14:00:00Z");

        @Override public ZoneId getZone() { return ZoneId.of("America/Lima"); }
        @Override public Clock withZone(ZoneId zona) { return this; }
        @Override public Instant instant() { return ahora; }

        void avanzar(Duration tiempo) { ahora = ahora.plus(tiempo); }
    }

    private RelojFalso reloj;
    private SesionService servicio;

    @BeforeEach
    void preparar() {
        reloj = new RelojFalso();
        servicio = new SesionService(15, reloj);
    }

    @Test
    void laSesionSeCierraA15MinutosSinActividad() {
        Sesion s = servicio.crear(1, "ENCARGADO");

        reloj.avanzar(Duration.ofMinutes(14));
        assertTrue(servicio.validar(s.token()).isPresent());

        reloj.avanzar(Duration.ofMinutes(16)); // 16 min desde la última petición
        assertTrue(servicio.validar(s.token()).isEmpty());
    }

    @Test
    void cadaPeticionRenuevaLos15Minutos() {
        Sesion s = servicio.crear(1, "ENCARGADO");

        // el usuario hace algo cada 10 minutos durante 40 minutos: la sesión sigue abierta
        for (int i = 0; i < 4; i++) {
            reloj.avanzar(Duration.ofMinutes(10));
            assertTrue(servicio.validar(s.token()).isPresent(), "a los " + (i + 1) * 10 + " min debería seguir activa");
        }
    }

    @Test
    void cerrarSesionInvalidaElToken() {
        Sesion s = servicio.crear(1, "PRESTATARIO");
        servicio.invalidar(s.token());
        assertTrue(servicio.validar(s.token()).isEmpty());
    }

    @Test
    void cadaLoginRecibeUnTokenDistinto() {
        assertNotEquals(servicio.crear(1, "PRESTATARIO").token(), servicio.crear(1, "PRESTATARIO").token());
    }
}
