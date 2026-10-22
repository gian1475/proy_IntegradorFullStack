package com.alejandria.biblioteca.dto.reporte;

/**
 * Un indicador de gestión (apartado 1.2.2 del informe) con su valor del mes.
 * valor y cumpleMeta son null cuando el sistema no tiene los datos para calcularlo.
 */
public record IndicadorDTO(
        String codigo,      // TAP, PDF, IPI, TIM
        String nombre,
        Double valor,
        String unidad,
        String lineaBase,
        String meta,
        Boolean cumpleMeta,
        String detalle      // cómo se calculó o qué falta para calcularlo
) {}
