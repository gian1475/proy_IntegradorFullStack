package com.backend.demo.seguridad;

import com.google.common.base.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Bloqueo por intentos fallidos de login (Guava)")
class LimitadorIntentosLoginTest {

    /** Reloj falso: permite "adelantar el tiempo" sin esperar 15 minutos de verdad. */
    static class RelojFalso extends Ticker {
        private long nanos = 0;

        @Override
        public long read() { return nanos; }

        void avanzar(Duration tiempo) { nanos += tiempo.toNanos(); }
    }

    private RelojFalso reloj;
    private LimitadorIntentosLogin limitador;

    @BeforeEach
    void preparar() {
        reloj = new RelojFalso();
        limitador = new LimitadorIntentosLogin(reloj);
    }

    @Test
    void bloqueaAlLlegarAlMaximoDeIntentos() {
        for (int i = 1; i < LimitadorIntentosLogin.MAX_INTENTOS; i++) {
            limitador.registrarFallo("alumno@alejandria.edu.pe");
            assertFalse(limitador.estaBloqueado("alumno@alejandria.edu.pe"));
        }
        limitador.registrarFallo("alumno@alejandria.edu.pe");
        assertTrue(limitador.estaBloqueado("alumno@alejandria.edu.pe"));
    }

    @Test
    void elCorreoNoDistingueMayusculas() {
        for (int i = 0; i < LimitadorIntentosLogin.MAX_INTENTOS; i++) {
            limitador.registrarFallo("Alumno@Alejandria.edu.pe");
        }
        assertTrue(limitador.estaBloqueado("alumno@alejandria.edu.pe"));
    }

    @Test
    void seDesbloqueaDespuesDelTiempoDeBloqueo() {
        for (int i = 0; i < LimitadorIntentosLogin.MAX_INTENTOS; i++) {
            limitador.registrarFallo("alumno@alejandria.edu.pe");
        }
        reloj.avanzar(LimitadorIntentosLogin.TIEMPO_BLOQUEO.plusSeconds(1));
        assertFalse(limitador.estaBloqueado("alumno@alejandria.edu.pe"));
    }

    @Test
    void unLoginCorrectoReiniciaLosIntentos() {
        limitador.registrarFallo("alumno@alejandria.edu.pe");
        limitador.registrarFallo("alumno@alejandria.edu.pe");
        limitador.registrarExito("alumno@alejandria.edu.pe");
        assertEquals(1, limitador.registrarFallo("alumno@alejandria.edu.pe"));
    }

    @Test
    void cadaCorreoTieneSuPropioContador() {
        for (int i = 0; i < LimitadorIntentosLogin.MAX_INTENTOS; i++) {
            limitador.registrarFallo("alumno@alejandria.edu.pe");
        }
        assertFalse(limitador.estaBloqueado("otro@alejandria.edu.pe"));
    }
}
