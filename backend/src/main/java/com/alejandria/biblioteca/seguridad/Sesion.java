package com.alejandria.biblioteca.seguridad;

import java.time.LocalDateTime;

/**
 * Datos de una sesión activa: su token, quién es el usuario y su rol.
 * expiraEn es el vencimiento que se informa al iniciar sesión; el vencimiento real (15 min sin actividad)
 * lo controla la caché de Guava de SesionService.
 */
public record Sesion(String token, Integer idUsuario, String rol, LocalDateTime expiraEn) {}
