package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dto.reporte.TablaReporte;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Los 8 reportes contra la BD real (Neon): cada consulta corre sin error, con y sin rango de fechas,
 * y cada formato sale bien. Solo lee: no cambia nada.
 */
@DisplayName("Reportes RF08 en la BD: los 8 tipos en Excel, PDF y CSV")
@SpringBootTest
@Transactional(readOnly = true)
class ReporteBdTest {

    @Autowired private ReporteService reporteService;

    @ParameterizedTest
    @ValueSource(strings = {"prestamos", "devoluciones", "reservas", "incidencias", "bajas", "stock", "indicadores", "tiempos"})
    void cadaReporteSeArmaConYSinFechas(String tipo) {
        TablaReporte todas = reporteService.armar(tipo, null, null);
        TablaReporte delMes = reporteService.armar(tipo, LocalDate.now().withDayOfMonth(1), LocalDate.now());

        assertTrue(todas.columnas().length > 0);
        assertTrue(todas.filas().stream().allMatch(f -> f.length == todas.columnas().length)); // ninguna fila descuadrada
        assertNotNull(delMes.periodo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"excel", "pdf", "csv"})
    void cadaFormatoSeGenera(String formato) {
        byte[] archivo = reporteService.generar("stock", formato, null, null);

        assertTrue(archivo.length > 0);
        if ("pdf".equals(formato)) {
            assertEquals("%PDF", new String(archivo, 0, 4, StandardCharsets.US_ASCII));
        }
    }
}
