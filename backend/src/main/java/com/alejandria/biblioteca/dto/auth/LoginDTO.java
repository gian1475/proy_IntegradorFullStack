package com.alejandria.biblioteca.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginDTO(
        @NotBlank(message = "Datos incompletos") String correo,
        @NotBlank(message = "Datos incompletos") String contrasena
) {}
