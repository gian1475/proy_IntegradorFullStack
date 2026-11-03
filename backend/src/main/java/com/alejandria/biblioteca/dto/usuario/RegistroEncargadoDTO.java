package com.alejandria.biblioteca.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos que llena el admin para registrar un encargado (RF09). Mismas reglas que el registro del prestatario.
 * turno: "mañana" o "tarde" (lo revisa UsuarioService, que también lo pasa a minúsculas).
 */
public record RegistroEncargadoDTO(
        @NotBlank(message = "El nombre debe tener entre 1 y 50 caracteres")
        @Size(max = 50, message = "El nombre debe tener entre 1 y 50 caracteres")
        String nombre,

        @NotBlank(message = "El apellido debe tener entre 1 y 50 caracteres")
        @Size(max = 50, message = "El apellido debe tener entre 1 y 50 caracteres")
        String apellido,

        @NotNull(message = "El DNI debe tener 8 dígitos")
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
        String dni,

        @NotNull(message = "El correo no es válido")
        @Pattern(regexp = "[^@\\s]+@[^@\\s]+\\.[^@\\s]+", message = "El correo no es válido")
        @Size(max = 100, message = "El correo no puede tener más de 100 caracteres")
        String correo,

        @NotNull(message = "Ingrese la contraseña")
        String contrasena,

        String turno
) {}
