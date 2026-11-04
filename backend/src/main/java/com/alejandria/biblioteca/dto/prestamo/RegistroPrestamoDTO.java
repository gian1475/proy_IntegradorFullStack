package com.alejandria.biblioteca.dto.prestamo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Datos que envía el Encargado para registrar un préstamo en ventanilla.
 * segundosAtencion (TAP, opcional): cuánto tardó desde que empezó a llenar el formulario hasta confirmar.
 * Se manda la duración y no la hora para que no importe si el reloj de la PC está desfasado del servidor.
 */
public record RegistroPrestamoDTO(
        @NotBlank(message = "Debe indicar el DNI del prestatario y el libro") String dniPrestatario,
        @NotNull(message = "Debe indicar el DNI del prestatario y el libro") Integer idLibro,
        Integer segundosAtencion
) {}
