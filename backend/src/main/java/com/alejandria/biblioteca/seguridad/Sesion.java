package com.alejandria.biblioteca.seguridad;

import java.time.LocalDateTime;

/** Datos de una sesión activa: su token, quién es el usuario, su rol y cuándo vence. */
public record Sesion(String token, Integer idUsuario, String rol, LocalDateTime expiraEn) {

    public boolean estaExpirada(LocalDateTime ahora) {
        return ahora.isAfter(expiraEn);
    }

    /** La misma sesión con un nuevo vencimiento (se usa al renovarla con cada petición). */
    public Sesion conVencimiento(LocalDateTime nuevoVencimiento) {
        return new Sesion(token, idUsuario, rol, nuevoVencimiento);
    }
}
