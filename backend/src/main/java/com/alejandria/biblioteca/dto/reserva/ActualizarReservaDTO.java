package com.alejandria.biblioteca.dto.reserva;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * RF15: nueva fecha y franja de recojo de una reserva pendiente. Como al crearla, se manda el final de la
 * franja elegida (por ejemplo, 30/10 17:30 para la franja 14:30 - 17:30).
 */
public record ActualizarReservaDTO(
        @NotNull(message = "Elija la fecha y la franja de recojo") LocalDateTime fechaRecojoLimite
) {}
