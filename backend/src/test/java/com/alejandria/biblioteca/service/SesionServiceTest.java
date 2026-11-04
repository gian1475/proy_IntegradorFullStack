package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.seguridad.Sesion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Ticker;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Sesión: se cierra a los 15 minutos sin actividad (RNF04)")
class SesionServiceTest {

    /** Reloj falso de Guava: permite adelantar el tiempo sin esperar de verdad (igual que en LimitadorIntentosLoginTest). */
    static class RelojFalso extends Ticker {
        private long nanos = 0;

        @Override
        public long read() { return nanos; }

        void avanzar(Duration tiempo) { nanos += tiempo.toNanos(); }
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
    void alDarDeBajaAUnUsuarioSeCierranTodasSusSesiones() {
        // RF13: si el usuario estaba conectado, su token deja de servir en ese momento
        Sesion pc = servicio.crear(5, "PRESTATARIO");
        Sesion celular = servicio.crear(5, "PRESTATARIO");
        Sesion otro = servicio.crear(6, "PRESTATARIO");

        servicio.cerrarSesionesDe(5);

        assertTrue(servicio.validar(pc.token()).isEmpty());
        assertTrue(servicio.validar(celular.token()).isEmpty());
        assertTrue(servicio.validar(otro.token()).isPresent()); // los demás siguen conectados
    }

    @Test
    void elMonitoreoSoloCuentaLasSesionesQueNoVencieron() {
        servicio.crear(1, "ENCARGADO");
        servicio.crear(2, "PRESTATARIO");
        assertEquals(2, servicio.contarActivas());

        reloj.avanzar(Duration.ofMinutes(16));
        assertEquals(0, servicio.contarActivas());
    }

    @Test
    void cadaLoginRecibeUnTokenDistinto() {
        assertNotEquals(servicio.crear(1, "PRESTATARIO").token(), servicio.crear(1, "PRESTATARIO").token());
    }
}
