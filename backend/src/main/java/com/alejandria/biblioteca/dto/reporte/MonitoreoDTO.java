package com.alejandria.biblioteca.dto.reporte;

/**
 * Resumen de métricas para la pestaña de monitoreo del Administrador.
 * Las métricas técnicas vienen de Spring Boot Actuator (Micrometer); las del negocio, de la BD.
 */
public record MonitoreoDTO(
        // Estado general (Actuator /health: aplicación + base de datos)
        String estado,
        // Aplicación (JVM)
        Long tiempoActivoSegundos,
        Double memoriaUsadaMB,
        Double memoriaMaximaMB,
        Double cpuProcesoPorcentaje,
        Double hilosActivos,
        // Peticiones HTTP
        Long peticionesHttp,
        Long peticionesConError,
        Double tiempoRespuestaPromedioMs,
        // RNF03: % de peticiones que respondieron en 2 s o menos (meta: 95 % o más). null si todavía no hay
        Double peticionesHasta2sPorcentaje,
        // Base de datos (pool de conexiones Hikari)
        Double conexionesBdActivas,
        Double conexionesBdTotales,
        // Negocio
        Long sesionesActivas,
        Long prestamosActivos,
        Long prestamosVencidos,
        Long reservasPendientes
) {}
