package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dao.ReservaDao;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.IndicatedHealthDescriptor;
import org.springframework.boot.health.contributor.Status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * B3 (RNF03): "al menos el 95 % de las operaciones en 2 segundos o menos; un promedio, por sí solo, no basta".
 * Micrometer cuenta cuántas peticiones tardaron hasta 2 s (un "SLO" del histograma) y se calcula el porcentaje.
 */
@DisplayName("Monitoreo: % de peticiones en 2 segundos o menos (RNF03)")
class MonitoreoServiceTest {

    private Timer timerConLimiteDe2s(SimpleMeterRegistry registro, String uri) {
        return Timer.builder("http.server.requests").tag("uri", uri)
                .serviceLevelObjectives(Duration.ofSeconds(2))
                .register(registro);
    }

    // antes de contar los prestamos vencidos los marca con la hora del reloj del sistema (HorarioAtencion),
    // no con la de la PC. la BD y el estado de salud se reemplazan con Mockito
    @Test
    void marcaLosVencidosConLaHoraDelRelojDelSistema() {
        LocalDateTime ahora = LocalDateTime.of(2026, 2, 15, 10, 0);
        PrestamoDao prestamoDao = mock(PrestamoDao.class);
        // estado de salud "UP" de mentira (en spring boot 4 HealthDescriptor es sellada: se simula la clase concreta)
        IndicatedHealthDescriptor salud = mock(IndicatedHealthDescriptor.class);
        when(salud.getStatus()).thenReturn(Status.UP);
        HealthEndpoint healthEndpoint = mock(HealthEndpoint.class);
        when(healthEndpoint.health()).thenReturn(salud);
        MonitoreoService servicio = new MonitoreoService(new SimpleMeterRegistry(), healthEndpoint,
                new SesionService(15), prestamoDao, mock(ReservaDao.class), HorarioAtencionTest.horario(ahora));

        servicio.obtenerResumen();

        verify(prestamoDao).marcarVencidos(ahora);
    }

    @Test
    void cuentaLasPeticionesQueTardaronHasta2Segundos() {
        SimpleMeterRegistry registro = new SimpleMeterRegistry();
        Timer libros = timerConLimiteDe2s(registro, "/api/libros");
        Timer reportes = timerConLimiteDe2s(registro, "/api/reportes");
        for (int i = 0; i < 18; i++) libros.record(Duration.ofMillis(150));
        reportes.record(Duration.ofMillis(1900));
        reportes.record(Duration.ofSeconds(3)); // la unica lenta

        // 19 de 20 = 95 %: cumple justo el RNF03
        assertEquals(95.0, MonitoreoService.porcentajeHasta(List.of(libros, reportes), Duration.ofSeconds(2)));
    }

    @Test
    void sinPeticionesNoHayDato() {
        SimpleMeterRegistry registro = new SimpleMeterRegistry();
        assertNull(MonitoreoService.porcentajeHasta(List.of(timerConLimiteDe2s(registro, "/x")), Duration.ofSeconds(2)));
    }

    @Test
    void siNoSeConfiguroElLimiteNoSeInventaUnValor() {
        SimpleMeterRegistry registro = new SimpleMeterRegistry();
        Timer sinLimite = Timer.builder("http.server.requests").register(registro);
        sinLimite.record(Duration.ofMillis(100));

        assertNull(MonitoreoService.porcentajeHasta(List.of(sinLimite), Duration.ofSeconds(2)));
    }
}
