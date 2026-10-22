package com.alejandria.biblioteca.seguridad;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Bloquea un correo después de varios intentos fallidos de login (usa Google Guava).
 * El Cache de Guava guarda "correo -> intentos fallidos" y borra la entrada sola
 * cuando pasa el tiempo de bloqueo, así que no hay que limpiar nada a mano.
 */
// guava: cache q guarda "correo -> intentos fallidos". con 3 fallos se bloquea 15 min y se borra solo
@Component
public class LimitadorIntentosLogin {

    public static final int MAX_INTENTOS = 3; // RF10: "tras tres intentos fallidos"
    public static final Duration TIEMPO_BLOQUEO = Duration.ofMinutes(15);

    private final Cache<String, Integer> intentosFallidos;

    @Autowired
    public LimitadorIntentosLogin() {
        this(Ticker.systemTicker());
    }

    /** Permite usar un reloj falso en las pruebas JUnit. */
    LimitadorIntentosLogin(Ticker reloj) {
        this.intentosFallidos = CacheBuilder.newBuilder()
                .expireAfterWrite(TIEMPO_BLOQUEO)
                .ticker(reloj)
                .build();
    }

    public boolean estaBloqueado(String correo) {
        Integer intentos = intentosFallidos.getIfPresent(clave(correo));
        return intentos != null && intentos >= MAX_INTENTOS;
    }

    /** Suma un intento fallido y devuelve cuántos lleva. */
    public int registrarFallo(String correo) {
        return intentosFallidos.asMap().merge(clave(correo), 1, Integer::sum);
    }

    /** Un login correcto borra los intentos fallidos. */
    public void registrarExito(String correo) {
        intentosFallidos.invalidate(clave(correo));
    }

    private String clave(String correo) {
        return correo.trim().toLowerCase();
    }
}
