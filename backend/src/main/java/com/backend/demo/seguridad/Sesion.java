package com.backend.demo.seguridad;

import java.time.LocalDateTime;

/** Datos de una sesión activa: su token, quién es el usuario, su rol y cuándo vence. */
public record Sesion(String token, Integer idUsuario, String rol, LocalDateTime expiraEn) {

    public boolean estaExpirada() {
        return LocalDateTime.now().isAfter(expiraEn);
    }
}
