package com.alejandria.biblioteca.dto.prestamo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Estado físico del libro al devolverlo: "bueno" o "dañado". */
public record DevolucionDTO(
        @NotNull(message = "El estado físico debe ser 'bueno' o 'dañado'")
        @Pattern(regexp = "bueno|dañado", message = "El estado físico debe ser 'bueno' o 'dañado'")
        String estadoFisico
) {}
