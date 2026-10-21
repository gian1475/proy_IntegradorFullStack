package com.backend.demo.controller;

import com.backend.demo.dto.DevolucionDTO;
import com.backend.demo.dto.PrestamoDTO;
import com.backend.demo.dto.PrestamoDetalleDTO;
import com.backend.demo.dto.RegistroPrestamoDTO;
import com.backend.demo.exception.NegocioException;
import com.backend.demo.seguridad.AutorizacionInterceptor;
import com.backend.demo.seguridad.Rol;
import com.backend.demo.seguridad.RolesPermitidos;
import com.backend.demo.seguridad.Sesion;
import com.backend.demo.service.PrestamoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prestamos")
@CrossOrigin(origins = "http://localhost:4200")
public class PrestamoController {

    private final PrestamoService prestamoService;

    @Autowired
    public PrestamoController(PrestamoService prestamoService) {
        this.prestamoService = prestamoService;
    }

    /** ADICIONAR: el encargado que registra se toma de la sesión, no del formulario. */
    @PostMapping
    @RolesPermitidos(Rol.ENCARGADO)
    public ResponseEntity<PrestamoDTO> registrar(@RequestBody RegistroPrestamoDTO dto,
                                                 @RequestAttribute(AutorizacionInterceptor.ATRIBUTO_SESION) Sesion sesion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prestamoService.registrar(dto, sesion.idUsuario()));
    }

    /** CONSULTAR (JOIN): préstamos activos con nombre del prestatario y título del libro. */
    @GetMapping("/activos")
    @RolesPermitidos({Rol.ENCARGADO, Rol.ADMINISTRADOR})
    public List<PrestamoDetalleDTO> listarActivos() {
        return prestamoService.listarActivos();
    }

    /** CONSULTAR (una tabla): historial de un prestatario. Un prestatario solo ve el suyo. */
    @GetMapping("/prestatario/{idPrestatario}")
    @RolesPermitidos({Rol.ENCARGADO, Rol.ADMINISTRADOR, Rol.PRESTATARIO})
    public List<PrestamoDTO> listarPorPrestatario(@PathVariable Integer idPrestatario,
                                                  @RequestAttribute(AutorizacionInterceptor.ATRIBUTO_SESION) Sesion sesion) {
        if (Rol.PRESTATARIO.equals(sesion.rol()) && !sesion.idUsuario().equals(idPrestatario)) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "Solo puede consultar sus propios préstamos");
        }
        return prestamoService.listarPorPrestatario(idPrestatario);
    }

    @GetMapping("/{id}")
    @RolesPermitidos({Rol.ENCARGADO, Rol.ADMINISTRADOR})
    public PrestamoDTO obtener(@PathVariable Integer id) {
        return prestamoService.obtener(id);
    }

    /** ACTUALIZAR: ampliar la hora límite (una sola vez). */
    @PutMapping("/{id}/ampliar")
    @RolesPermitidos(Rol.ENCARGADO)
    public PrestamoDTO ampliar(@PathVariable Integer id) {
        return prestamoService.ampliar(id);
    }

    /** ACTUALIZAR: registrar la devolución con el estado físico del libro. */
    @PutMapping("/{id}/devolucion")
    @RolesPermitidos(Rol.ENCARGADO)
    public PrestamoDTO devolver(@PathVariable Integer id, @RequestBody DevolucionDTO dto) {
        return prestamoService.devolver(id, dto);
    }

    /** ELIMINAR (lógico): el préstamo queda 'anulado', no se borra la fila. */
    @DeleteMapping("/{id}")
    @RolesPermitidos({Rol.ENCARGADO, Rol.ADMINISTRADOR})
    public ResponseEntity<Map<String, String>> anular(@PathVariable Integer id) {
        prestamoService.anular(id);
        return ResponseEntity.ok(Map.of("mensaje", "Préstamo anulado correctamente"));
    }
}
