package com.alejandria.biblioteca.service;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/** RNF03: el % de peticiones en 2 s o menos se cuenta desde que arrancó el sistema, igual que el total. */
// monitoreo RNF03 (D34): por defecto micrometer guarda el histograma ("cuantas tardaron hasta 2 s") solo de los
// ultimos 2 minutos, pero el total de peticiones es desde el inicio. al dividir uno por el otro el % bajaba solo
// con el tiempo (24/11: 28 % "No cumple" con un promedio de 570 ms). este filtro de micrometer le quita la ventana
@Configuration
public class MonitoreoConfig {

    /** La métrica de Spring Boot que mide cada petición HTTP. */
    static final String PETICIONES = "http.server.requests";

    @Bean
    MeterFilter histogramaDeLasPeticionesDesdeElInicio() {
        return histogramaSinVentana();
    }

    /** Para http.server.requests: una sola ventana de 10 años (en la práctica, desde que arrancó el sistema). */
    static MeterFilter histogramaSinVentana() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (!PETICIONES.equals(id.getName())) {
                    return config;
                }
                // merge: se quedan el limite de 2 s (application.properties) y lo demas; solo cambia la ventana
                return DistributionStatisticConfig.builder()
                        .expiry(Duration.ofDays(3650))
                        .bufferLength(1)
                        .build()
                        .merge(config);
            }
        };
    }
}
