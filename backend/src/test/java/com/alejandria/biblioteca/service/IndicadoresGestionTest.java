package com.alejandria.biblioteca.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Fórmulas de los indicadores de gestión (apartado 1.2.2 del informe)")
class IndicadoresGestionTest {

    @Test
    void porcentajeDeDevolucionesFueraDePlazoComoElEjemploDelInforme() {
        // 2.2.6, ejemplo 2: 20 de 400 préstamos se devolvieron después de las 3 horas = 5 %
        assertEquals(5.0, IndicadoresGestion.porcentaje(20, 400));
    }

    @Test
    void redondeaAUnDecimal() {
        assertEquals(33.3, IndicadoresGestion.porcentaje(1, 3));
    }

    @Test
    void sinPrestamosEnElMesNoHayValor() {
        assertNull(IndicadoresGestion.porcentaje(0, 0)); // no se divide entre cero
    }

    @Test
    void laMetaEsUnMaximo() {
        assertTrue(IndicadoresGestion.cumpleMetaMaxima(5.0, 5.0));   // PDF: meta 5 % o menos
        assertFalse(IndicadoresGestion.cumpleMetaMaxima(5.1, 5.0));
        assertNull(IndicadoresGestion.cumpleMetaMaxima(null, 5.0));  // sin dato no se sabe
    }
}
