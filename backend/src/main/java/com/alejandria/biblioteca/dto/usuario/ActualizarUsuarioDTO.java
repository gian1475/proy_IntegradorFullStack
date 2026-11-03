package com.alejandria.biblioteca.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * RF12: datos que el admin puede cambiar de un prestatario o un encargado. Mismas reglas que el registro.
 * No lleva contraseña ni estado: el estado cambia con "Dar de baja" / "Reactivar" (RF13) y con las sanciones (RF06).
 * turno: solo para encargados, "mañana" o "tarde" (lo revisa UsuarioService); al prestatario no se le pide.
 */
public record ActualizarUsuarioDTO(
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

        String turno
) {}
