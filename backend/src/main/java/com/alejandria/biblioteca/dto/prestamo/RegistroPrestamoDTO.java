package com.alejandria.biblioteca.dto.prestamo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Datos que envía el Encargado para registrar un préstamo en ventanilla. */
public record RegistroPrestamoDTO(
        @NotBlank(message = "Debe indicar el DNI del prestatario y el libro") String dniPrestatario,
        @NotNull(message = "Debe indicar el DNI del prestatario y el libro") Integer idLibro
) {}
