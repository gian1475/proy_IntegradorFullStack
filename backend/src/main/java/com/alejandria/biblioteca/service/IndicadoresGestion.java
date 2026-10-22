package com.alejandria.biblioteca.service;

/**
 * Fórmulas de los indicadores de gestión del apartado 1.2.2 del informe.
 * Son métodos sueltos (sin BD) para poder probarlos con JUnit.
 */
public final class IndicadoresGestion {

    private IndicadoresGestion() {}

    /** (parte / total) × 100 con un decimal. Si no hay datos (total 0) devuelve null: no se divide entre cero. */
    // indicadores: PDF = (N retrasos / N prestamos) x 100 y TIM = (N incidencias / N prestamos) x 100
    public static Double porcentaje(long parte, long total) {
        if (total == 0) {
            return null;
        }
        return Math.round(parte * 1000.0 / total) / 10.0;
    }

    /** La meta es un máximo (por ejemplo "5 % o menos"). Sin dato, no se sabe si cumple (null). */
    public static Boolean cumpleMetaMaxima(Double valor, double meta) {
        return valor == null ? null : valor <= meta;
    }
}
