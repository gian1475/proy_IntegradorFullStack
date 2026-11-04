package com.alejandria.biblioteca.seguridad;

import com.alejandria.biblioteca.service.MonitoreoService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** B3 (RNF03): application.properties activa el conteo de peticiones de hasta 2 s en las métricas reales. */
@DisplayName("Monitoreo: la aplicación mide las peticiones de hasta 2 s (RNF03)")
@SpringBootTest
@AutoConfigureMockMvc
class MonitoreoRnf03Test {

    @Autowired private MockMvc mvc;
    @Autowired private MeterRegistry registro;

    @Test
    void despuesDeUnaPeticionHayPorcentajeDePeticionesRapidas() throws Exception {
        mvc.perform(get("/api/categorias")).andExpect(status().isOk());

        Double porcentaje = MonitoreoService.porcentajeHasta(
                registro.find("http.server.requests").timers(), Duration.ofSeconds(2));

        assertNotNull(porcentaje, "falta management.metrics.distribution.slo.http.server.requests=2s");
        assertTrue(porcentaje > 0);
    }
}
