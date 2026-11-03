package com.alejandria.biblioteca.dto.reserva;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Datos para crear una reserva: libro, prestatario y hasta cuándo puede recogerlo.
 * La fecha de recojo no lleva anotación: solo es obligatoria si hay ejemplar (sin stock va a la cola de espera).
 */
public record ReservaDTO(
        @NotNull(message = "Datos incompletos para procesar la reserva") Integer idLibro,
        @NotNull(message = "Datos incompletos para procesar la reserva") Integer idUsuario,
        LocalDateTime fechaRecojoLimite
) {}
