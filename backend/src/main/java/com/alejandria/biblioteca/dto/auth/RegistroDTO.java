package com.alejandria.biblioteca.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos del registro de un prestatario. Los límites son los mismos de la tabla usuario,
 * para que la BD nunca rechace el dato. La contraseña se revisa en ValidadorContrasena.
 */
// validar datos: cada anotacion es una regla. si un dato no la cumple, el backend responde 400 con ese mensaje
public record RegistroDTO(
        @NotBlank(message = "El nombre debe tener entre 1 y 50 caracteres")
        @Size(max = 50, message = "El nombre debe tener entre 1 y 50 caracteres")
        String nombre,

        @NotBlank(message = "El apellido debe tener entre 1 y 50 caracteres")
        @Size(max = 50, message = "El apellido debe tener entre 1 y 50 caracteres")
        String apellido,

        // dni de exactamente 8 numeros (la columna dni es VARCHAR(8))
        @NotNull(message = "El DNI debe tener 8 dígitos")
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
        String dni,

        // algo@algo.algo, sin espacios
        @NotNull(message = "El correo no es válido")
        @Pattern(regexp = "[^@\\s]+@[^@\\s]+\\.[^@\\s]+", message = "El correo no es válido")
        @Size(max = 100, message = "El correo no puede tener más de 100 caracteres")
        String correo,

        @NotNull(message = "Ingrese la contraseña")
        String contrasena
) {}
