package com.alejandria.biblioteca.controller;

import com.alejandria.biblioteca.dto.auditoria.AuditoriaDTO;
import com.alejandria.biblioteca.dto.auditoria.AuditoriaMesDTO;
import com.alejandria.biblioteca.dto.auditoria.RegistroAuditoriaDTO;
import com.alejandria.biblioteca.seguridad.Rol;
import com.alejandria.biblioteca.seguridad.Sesion;
import com.alejandria.biblioteca.service.AuditoriaService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// auditoria inventario (IPI): solo el admin registra el conteo fisico y ve el IPI del mes
@RestController
@RequestMapping("/api/auditorias")
@RolesAllowed(Rol.ADMINISTRADOR)
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public AuditoriaMesDTO resumenDelMes() {
        return auditoriaService.resumenDelMes();
    }

    @PostMapping
    public ResponseEntity<AuditoriaDTO> registrar(@Valid @RequestBody RegistroAuditoriaDTO dto,
                                                  @AuthenticationPrincipal Sesion sesion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auditoriaService.registrar(dto, sesion.idUsuario()));
    }
}
