package com.alejandria.biblioteca.dto.incidencia;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Lo que llena el encargado al registrar una incidencia con su sanción (RF06, CU09).
 * Los días solo se usan en la suspensión y el monto solo en la multa (eso lo revisa IncidenciaService).
 */
public record RegistroIncidenciaDTO(
        @NotNull(message = "Elija el préstamo") Integer idPrestamo,

        @NotNull(message = "El tipo de incidencia debe ser retraso, daño o pérdida")
        @Pattern(regexp = "retraso|daño|pérdida", message = "El tipo de incidencia debe ser retraso, daño o pérdida")
        String tipoIncidencia,

        @Size(max = 255, message = "La descripción no puede tener más de 255 caracteres") String descripcion,

        @NotNull(message = "La sanción debe ser advertencia, suspensión o multa")
        @Pattern(regexp = "advertencia|suspensión|multa", message = "La sanción debe ser advertencia, suspensión o multa")
        String tipoSancion,

        @Min(value = 1, message = "La suspensión es de 1 a 30 días")
        @Max(value = 30, message = "La suspensión es de 1 a 30 días")
        Integer diasSuspension,

        // monto: DECIMAL(8,2) en la bd -> hasta 999999.99
        @DecimalMin(value = "0.01", message = "El monto de la multa debe ser mayor que 0")
        @Digits(integer = 6, fraction = 2, message = "El monto puede tener hasta 6 cifras y 2 decimales")
        BigDecimal monto
) {}
