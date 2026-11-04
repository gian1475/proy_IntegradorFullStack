package com.alejandria.biblioteca.dto.auditoria;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** IPI: el conteo físico de un título que ingresa el administrador. */
public record RegistroAuditoriaDTO(
        @NotNull(message = "Elija el libro y escriba el conteo físico") Integer idLibro,
        @NotNull(message = "Elija el libro y escriba el conteo físico")
        @PositiveOrZero(message = "El conteo físico no puede ser negativo") Integer conteoFisico
) {}
