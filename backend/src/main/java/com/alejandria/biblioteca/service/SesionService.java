package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.seguridad.Sesion;
import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Guarda en memoria las sesiones activas (token -> sesión).
 * Si se reinicia el backend, todos deben volver a iniciar sesión.
 */
// sesion 15 min: la sesion se cierra sola si el usuario pasa 15 minutos sin hacer nada (RNF04).
// guava: es una cache de Google Guava con expireAfterAccess. cada vez q se lee un token (cada peticion)
// guava le vuelve a contar los 15 minutos, y si pasan 15 min sin leerlo lo borra solo.
// antes esto se hacia a mano con un ConcurrentHashMap y una fecha de vencimiento por sesion (L3, 03/11)
@Service
public class SesionService {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final Cache<String, Sesion> sesiones;
    private final Duration inactividadMaxima;

    @Autowired
    public SesionService(@Value("${app.sesion.inactividad-minutos:15}") long minutosInactividad) {
        this(minutosInactividad, Ticker.systemTicker());
    }

    /** Permite usar un reloj falso de Guava en las pruebas JUnit. */
    SesionService(long minutosInactividad, Ticker reloj) {
        this.inactividadMaxima = Duration.ofMinutes(minutosInactividad);
        this.sesiones = CacheBuilder.newBuilder()
                .expireAfterAccess(inactividadMaxima)
                .ticker(reloj)
                .build();
    }

    // token: al logearse se crea un token (uuid) con el id y el rol del usuario; vence a los 15 min sin actividad
    public Sesion crear(Integer idUsuario, String rol) {
        String token = UUID.randomUUID().toString();
        // expiraEn solo se informa al front en la respuesta del login; el vencimiento real lo lleva guava
        Sesion sesion = new Sesion(token, idUsuario, rol, LocalDateTime.now().plus(inactividadMaxima));
        sesiones.put(token, sesion);
        return sesion;
    }

    /** Devuelve la sesión si el token es válido. Leerla le renueva los 15 minutos (expireAfterAccess). */
    public Optional<Sesion> validar(String token) {
        return token == null ? Optional.empty() : Optional.ofNullable(sesiones.getIfPresent(token));
    }

    public void invalidar(String token) {
        if (token != null) {
            sesiones.invalidate(token);
        }
    }

    /** RF13: al dar de baja a un usuario se cierran todas sus sesiones abiertas (su token deja de servir). */
    public void cerrarSesionesDe(Integer idUsuario) {
        sesiones.asMap().values().removeIf(s -> s.idUsuario().equals(idUsuario));
    }

    /** Cantidad de sesiones activas (no vencidas), para el monitoreo. */
    public long contarActivas() {
        sesiones.cleanUp(); // guava borra las vencidas antes de contar
        return sesiones.size();
    }

    /** Extrae el token del header "Authorization: Bearer <token>". */
    public static String tokenDesdeHeader(String header) {
        if (header == null || !header.startsWith(PREFIJO_BEARER)) {
            return null;
        }
        return header.substring(PREFIJO_BEARER.length()).trim();
    }
}
