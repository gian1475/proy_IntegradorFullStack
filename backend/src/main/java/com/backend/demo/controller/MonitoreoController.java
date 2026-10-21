package com.backend.demo.controller;

import com.backend.demo.dto.MonitoreoDTO;
import com.backend.demo.seguridad.Rol;
import com.backend.demo.seguridad.RolesPermitidos;
import com.backend.demo.service.MonitoreoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/** Resumen de monitoreo para el panel del Administrador. */
@RestController
@RequestMapping("/api/monitoreo")
@CrossOrigin(origins = "http://localhost:4200")
public class MonitoreoController {

    private final MonitoreoService monitoreoService;

    @Autowired
    public MonitoreoController(MonitoreoService monitoreoService) {
        this.monitoreoService = monitoreoService;
    }

    @GetMapping
    @RolesPermitidos(Rol.ADMINISTRADOR)
    public MonitoreoDTO resumen() {
        return monitoreoService.obtenerResumen();
    }
}
