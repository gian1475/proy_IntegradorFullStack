package com.alejandria.biblioteca.dto.usuario;

import jakarta.validation.constraints.NotNull;

/**
 * B7: contraseña nueva que el admin pone a un usuario que olvidó la suya (lo atiende en la ventanilla).
 * La regla de la contraseña (8 a 64 caracteres, 72 bytes, minúscula, mayúscula, número y símbolo) la revisa
 * UsuarioService con ValidadorContrasena, igual que en el registro.
 */
public record RestablecerContrasenaDTO(
        @NotNull(message = "Ingrese la contraseña") String contrasena
) {}
