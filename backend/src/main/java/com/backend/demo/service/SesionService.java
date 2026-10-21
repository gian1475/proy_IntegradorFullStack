package com.backend.demo.service;

import com.backend.demo.seguridad.Sesion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda en memoria las sesiones activas (token -> sesión).
 * Si se reinicia el backend, todos deben volver a iniciar sesión.
 */
@Service
public class SesionService {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();
    private final Duration duracion;

    public SesionService(@Value("${app.sesion.duracion-horas:8}") long duracionHoras) {
        this.duracion = Duration.ofHours(duracionHoras);
    }

    /** Crea una sesión nueva con un token aleatorio que vence según la duración configurada. */
    public Sesion crear(Integer idUsuario, String rol) {
        sesiones.values().removeIf(Sesion::estaExpirada);
        String token = UUID.randomUUID().toString();
        Sesion sesion = new Sesion(token, idUsuario, rol, LocalDateTime.now().plus(duracion));
        sesiones.put(token, sesion);
        return sesion;
    }

    public Optional<Sesion> validar(String token) {
        if (token == null) {
            return Optional.empty();
        }
        Sesion sesion = sesiones.get(token);
        if (sesion == null) {
            return Optional.empty();
        }
        if (sesion.estaExpirada()) {
            sesiones.remove(token);
            return Optional.empty();
        }
        return Optional.of(sesion);
    }

    public void invalidar(String token) {
        if (token != null) {
            sesiones.remove(token);
        }
    }

    /** Extrae el token del header "Authorization: Bearer <token>". */
    public static String tokenDesdeHeader(String header) {
        if (header == null || !header.startsWith(PREFIJO_BEARER)) {
            return null;
        }
        return header.substring(PREFIJO_BEARER.length()).trim();
    }
}
