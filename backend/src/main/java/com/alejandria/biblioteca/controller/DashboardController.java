package com.alejandria.biblioteca.controller;

import com.alejandria.biblioteca.dto.reporte.DashboardDTO;
import com.alejandria.biblioteca.seguridad.Rol;
import com.alejandria.biblioteca.seguridad.RolesPermitidos;
import com.alejandria.biblioteca.service.DashboardService;
import org.springframework.web.bind.annotation.*;

/** Dashboard del Administrador (patrón Dashboard, indicadores de gestión). */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // dashboard / permisos admin: GET /api/dashboard, solo el admin
    @GetMapping
    @RolesPermitidos(Rol.ADMINISTRADOR)
    public DashboardDTO obtener() {
        return dashboardService.obtener();
    }
}
