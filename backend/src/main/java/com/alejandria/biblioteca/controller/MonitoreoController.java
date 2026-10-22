package com.alejandria.biblioteca.controller;

import com.alejandria.biblioteca.dto.reporte.MonitoreoDTO;
import com.alejandria.biblioteca.seguridad.Rol;
import com.alejandria.biblioteca.seguridad.RolesPermitidos;
import com.alejandria.biblioteca.service.MonitoreoService;
import org.springframework.web.bind.annotation.*;

/** Resumen de monitoreo para el panel del Administrador. */
@RestController
@RequestMapping("/api/monitoreo")
public class MonitoreoController {

    private final MonitoreoService monitoreoService;

    public MonitoreoController(MonitoreoService monitoreoService) {
        this.monitoreoService = monitoreoService;
    }

    // actuator / monitoreo: GET /api/monitoreo, solo el admin ve las metricas
    @GetMapping
    @RolesPermitidos(Rol.ADMINISTRADOR)
    public MonitoreoDTO resumen() {
        return monitoreoService.obtenerResumen();
    }
}
