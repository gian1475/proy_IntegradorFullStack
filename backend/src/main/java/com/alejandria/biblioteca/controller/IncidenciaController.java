package com.alejandria.biblioteca.controller;

import com.alejandria.biblioteca.dto.incidencia.IncidenciaDTO;
import com.alejandria.biblioteca.dto.incidencia.RegistroIncidenciaDTO;
import com.alejandria.biblioteca.seguridad.Rol;
import com.alejandria.biblioteca.service.IncidenciaService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ruta incidencias (RF06): /api/incidencias. permisos encargado: solo el encargado registra incidencias y sanciones
// (CU09). el admin las ve en el reporte de incidencias y sanciones (RF08)
@RestController
@RequestMapping("/api/incidencias")
@RolesAllowed(Rol.ENCARGADO)
public class IncidenciaController {

    private final IncidenciaService incidenciaService;

    public IncidenciaController(IncidenciaService incidenciaService) {
        this.incidenciaService = incidenciaService;
    }

    @GetMapping
    public List<IncidenciaDTO> listar() {
        return incidenciaService.listar();
    }

    // registrar incidencia: POST /api/incidencias (incidencia + su sancion)
    @PostMapping
    public ResponseEntity<IncidenciaDTO> registrar(@Valid @RequestBody RegistroIncidenciaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidenciaService.registrar(dto));
    }

    // pago de multa: PUT /api/incidencias/sanciones/{id}/pago
    @PutMapping("/sanciones/{idSancion}/pago")
    public IncidenciaDTO registrarPago(@PathVariable Integer idSancion) {
        return incidenciaService.registrarPago(idSancion);
    }

    @PutMapping("/sanciones/{idSancion}/cancelar")
    public IncidenciaDTO cancelarSancion(@PathVariable Integer idSancion) {
        return incidenciaService.cancelarSancion(idSancion);
    }
}
