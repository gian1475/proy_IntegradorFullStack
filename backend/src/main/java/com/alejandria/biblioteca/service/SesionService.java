package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.seguridad.Sesion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
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
// sesion 15 min: la sesion se cierra sola si el usuario pasa 15 minutos sin hacer nada (RNF04).
// cada peticion q hace el usuario le renueva los 15 minutos
@Service
public class SesionService {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();
    private final Duration inactividadMaxima;
    private final Clock reloj;

    @Autowired
    public SesionService(@Value("${app.sesion.inactividad-minutos:15}") long minutosInactividad) {
        this(minutosInactividad, Clock.systemDefaultZone());
    }

    /** Permite usar un reloj falso en las pruebas JUnit. */
    SesionService(long minutosInactividad, Clock reloj) {
        this.inactividadMaxima = Duration.ofMinutes(minutosInactividad);
        this.reloj = reloj;
    }

    // token: al logearse se crea un token (uuid) con el id y el rol del usuario; vence a los 15 min sin actividad
    public Sesion crear(Integer idUsuario, String rol) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        sesiones.values().removeIf(s -> s.estaExpirada(ahora));
        String token = UUID.randomUUID().toString();
        Sesion sesion = new Sesion(token, idUsuario, rol, ahora.plus(inactividadMaxima));
        sesiones.put(token, sesion);
        return sesion;
    }

    /** Devuelve la sesión si el token es válido y le renueva los 15 minutos. */
    public Optional<Sesion> validar(String token) {
        if (token == null) {
            return Optional.empty();
        }
        Sesion sesion = sesiones.get(token);
        if (sesion == null) {
            return Optional.empty();
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (sesion.estaExpirada(ahora)) {
            sesiones.remove(token);
            return Optional.empty();
        }
        // sesion 15 min: hubo actividad, se cuentan de nuevo los 15 minutos desde ahora
        Sesion renovada = sesion.conVencimiento(ahora.plus(inactividadMaxima));
        sesiones.put(token, renovada);
        return Optional.of(renovada);
    }

    public void invalidar(String token) {
        if (token != null) {
            sesiones.remove(token);
        }
    }

    /** Cantidad de sesiones activas (no vencidas), para el monitoreo. */
    public long contarActivas() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return sesiones.values().stream().filter(s -> !s.estaExpirada(ahora)).count();
    }

    /** Extrae el token del header "Authorization: Bearer <token>". */
    public static String tokenDesdeHeader(String header) {
        if (header == null || !header.startsWith(PREFIJO_BEARER)) {
            return null;
        }
        return header.substring(PREFIJO_BEARER.length()).trim();
    }
}
