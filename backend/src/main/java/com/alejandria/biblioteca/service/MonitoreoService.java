package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dao.ReservaDao;
import com.alejandria.biblioteca.dto.reporte.MonitoreoDTO;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Junta en un solo resumen las métricas que Spring Boot Actuator registra con Micrometer
 * (memoria, CPU, peticiones HTTP, conexiones a la BD) y algunos números del negocio.
 */
@Service
public class MonitoreoService {

    private static final double BYTES_POR_MB = 1024.0 * 1024.0;

    private final MeterRegistry registro;
    private final HealthEndpoint healthEndpoint;
    private final SesionService sesionService;
    private final PrestamoDao prestamoDao;
    private final ReservaDao reservaDao;

    public MonitoreoService(MeterRegistry registro, HealthEndpoint healthEndpoint, SesionService sesionService,
                            PrestamoDao prestamoDao, ReservaDao reservaDao) {
        this.registro = registro;
        this.healthEndpoint = healthEndpoint;
        this.sesionService = sesionService;
        this.prestamoDao = prestamoDao;
        this.reservaDao = reservaDao;
    }

    @Transactional
    public MonitoreoDTO obtenerResumen() {
        prestamoDao.marcarVencidos(LocalDateTime.now());

        // Peticiones HTTP: Actuator mide cada petición con un Timer llamado "http.server.requests"
        Collection<Timer> peticiones = registro.find("http.server.requests").timers();
        long totalPeticiones = peticiones.stream().mapToLong(Timer::count).sum();
        long conError = peticiones.stream()
                .filter(t -> "CLIENT_ERROR".equals(t.getId().getTag("outcome"))
                          || "SERVER_ERROR".equals(t.getId().getTag("outcome")))
                .mapToLong(Timer::count).sum();
        double tiempoTotalMs = peticiones.stream().mapToDouble(t -> t.totalTime(TimeUnit.MILLISECONDS)).sum();

        TimeGauge tiempoActivo = registro.find("process.uptime").timeGauge();
        Double cpu = valor("process.cpu.usage");

        return new MonitoreoDTO(
                healthEndpoint.health().getStatus().getCode(),
                tiempoActivo == null ? null : (long) tiempoActivo.value(TimeUnit.SECONDS),
                sumaHeap("jvm.memory.used"),
                sumaHeap("jvm.memory.max"),
                cpu == null ? null : redondear(cpu * 100),
                valor("jvm.threads.live"),
                totalPeticiones,
                conError,
                totalPeticiones == 0 ? 0.0 : redondear(tiempoTotalMs / totalPeticiones),
                valor("hikaricp.connections.active"),
                valor("hikaricp.connections"),
                sesionService.contarActivas(),
                prestamoDao.countByEstadoPrestamo("activo"),
                prestamoDao.countByEstadoPrestamo("vencido"),
                reservaDao.countByEstadoReserva("pendiente")
        );
    }

    /** Valor de un Gauge (indicador), o null si la métrica no existe. */
    private Double valor(String nombre) {
        Gauge gauge = registro.find(nombre).gauge();
        return gauge == null ? null : gauge.value();
    }

    /** Suma la memoria "heap" (donde viven los objetos Java) de todas sus zonas, en MB. */
    private Double sumaHeap(String nombre) {
        double bytes = registro.find(nombre).tag("area", "heap").gauges().stream()
                .mapToDouble(Gauge::value)
                .filter(v -> v > 0)
                .sum();
        return redondear(bytes / BYTES_POR_MB);
    }

    private double redondear(double numero) {
        return Math.round(numero * 10) / 10.0;
    }
}
