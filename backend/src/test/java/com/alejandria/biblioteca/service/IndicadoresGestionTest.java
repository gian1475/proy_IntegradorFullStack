package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dto.reporte.IndicadorDTO;
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
    void elTapEsElPromedioEnMinutosYLaMetaEsDosMinutosOMenos() {
        // 1.2.2: TAP = suma(hora de confirmacion - hora de inicio) / N
        IndicadorDTO tap = buscar(IndicadoresGestion.calcular(
                new IndicadoresGestion.Datos(10, 0, 0, 1.75, 10, 0, 0), "este mes"), "TAP");

        assertEquals(1.8, tap.valor());
        assertTrue(tap.cumpleMeta());
        assertEquals("Promedio de 10 préstamos con la hora de inicio de la atención este mes.", tap.detalle());
    }

    @Test
    void elIpiEsElPorcentajeDeTitulosQueCoincidenYLaMetaEsUnMinimo() {
        // 1.2.2: IPI = N libros coincidentes / N total auditados x 100, meta 95 % o mas
        IndicadorDTO bien = buscar(IndicadoresGestion.calcular(
                new IndicadoresGestion.Datos(0, 0, 0, null, 0, 20, 19), "este mes"), "IPI");
        IndicadorDTO mal = buscar(IndicadoresGestion.calcular(
                new IndicadoresGestion.Datos(0, 0, 0, null, 0, 20, 18), "este mes"), "IPI");

        assertEquals(95.0, bien.valor());
        assertTrue(bien.cumpleMeta());
        assertEquals(90.0, mal.valor());
        assertFalse(mal.cumpleMeta());
    }

    @Test
    void sinHoraDeInicioNiAuditoriasNoSeInventaUnValor() {
        var indicadores = IndicadoresGestion.calcular(new IndicadoresGestion.Datos(10, 1, 0, null, 0, 0, 0), "este mes");

        assertNull(buscar(indicadores, "TAP").valor());
        assertNull(buscar(indicadores, "TAP").cumpleMeta());
        assertNull(buscar(indicadores, "IPI").valor());
    }

    @Test
    void laMetaDelIpiEsUnMinimo() {
        assertTrue(IndicadoresGestion.cumpleMetaMinima(95.0, 95.0));
        assertFalse(IndicadoresGestion.cumpleMetaMinima(94.9, 95.0));
        assertNull(IndicadoresGestion.cumpleMetaMinima(null, 95.0));
    }

    private static IndicadorDTO buscar(java.util.List<IndicadorDTO> lista, String codigo) {
        return lista.stream().filter(i -> i.codigo().equals(codigo)).findFirst().orElseThrow();
    }

    @Test
    void laMetaEsUnMaximo() {
        assertTrue(IndicadoresGestion.cumpleMetaMaxima(5.0, 5.0));   // PDF: meta 5 % o menos
        assertFalse(IndicadoresGestion.cumpleMetaMaxima(5.1, 5.0));
        assertNull(IndicadoresGestion.cumpleMetaMaxima(null, 5.0));  // sin dato no se sabe
    }
}
