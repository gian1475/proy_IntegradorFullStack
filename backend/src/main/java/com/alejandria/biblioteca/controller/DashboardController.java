package com.alejandria.biblioteca.controller;

import com.alejandria.biblioteca.dto.reporte.DashboardDTO;
import com.alejandria.biblioteca.seguridad.Rol;
import com.alejandria.biblioteca.service.DashboardService;
import com.alejandria.biblioteca.service.ReservaService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.web.bind.annotation.*;

/** Dashboard del Administrador (patrón Dashboard, indicadores de gestión). */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final ReservaService reservaService;

    public DashboardController(DashboardService dashboardService, ReservaService reservaService) {
        this.dashboardService = dashboardService;
        this.reservaService = reservaService;
    }

    // dashboard / permisos admin: GET /api/dashboard, solo el admin
    @GetMapping
    @RolesAllowed(Rol.ADMINISTRADOR)
    public DashboardDTO obtener() {
        reservaService.liberarVencidas(); // D26: libros por estado y reservas pendientes, al dia
        return dashboardService.obtener();
    }
}
