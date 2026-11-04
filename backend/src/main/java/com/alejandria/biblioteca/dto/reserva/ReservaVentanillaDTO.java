package com.alejandria.biblioteca.dto.reserva;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

/**
 * B2 (RF04, actor Encargado): el encargado reserva en la ventanilla a nombre de un prestatario, con su DNI.
 * La fecha de recojo es el final de la franja elegida; sin stock va null y la reserva entra a la cola de espera.
 */
public record ReservaVentanillaDTO(
        @NotNull(message = "El DNI debe tener 8 dígitos")
        @Pattern(regexp = "\\s*\\d{8}\\s*", message = "El DNI debe tener 8 dígitos")
        String dniPrestatario,

        @NotNull(message = "Datos incompletos para procesar la reserva")
        Integer idLibro,

        LocalDateTime fechaRecojoLimite
) {}
