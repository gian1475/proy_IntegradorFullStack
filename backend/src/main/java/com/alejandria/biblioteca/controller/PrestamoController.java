package com.alejandria.biblioteca.controller;

import com.alejandria.biblioteca.dto.prestamo.DevolucionDTO;
import com.alejandria.biblioteca.dto.prestamo.PrestamoDTO;
import com.alejandria.biblioteca.dto.prestamo.PrestamoDetalleDTO;
import com.alejandria.biblioteca.dto.prestamo.RegistroPrestamoDTO;
import com.alejandria.biblioteca.exception.NegocioException;
import com.alejandria.biblioteca.seguridad.AutorizacionInterceptor;
import com.alejandria.biblioteca.seguridad.Rol;
import com.alejandria.biblioteca.seguridad.RolesPermitidos;
import com.alejandria.biblioteca.seguridad.Sesion;
import com.alejandria.biblioteca.service.PrestamoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// ruta crud prestamo: /api/prestamos. este es el crud del negocio (prestamos en sala)
// permisos encargado: el q registra, amplia y devuelve es el encargado
@RestController
@RequestMapping("/api/prestamos")
public class PrestamoController {

    private final PrestamoService prestamoService;

    public PrestamoController(PrestamoService prestamoService) {
        this.prestamoService = prestamoService;
    }

    /** ADICIONAR: el encargado que registra se toma de la sesión, no del formulario. */
    // crud prestamo adicionar: POST /api/prestamos (solo encargado)
    @PostMapping
    @RolesPermitidos(Rol.ENCARGADO)
    public ResponseEntity<PrestamoDTO> registrar(@RequestBody RegistroPrestamoDTO dto,
                                                 @RequestAttribute(AutorizacionInterceptor.ATRIBUTO_SESION) Sesion sesion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prestamoService.registrar(dto, sesion.idUsuario()));
    }

    /** CONSULTAR (JOIN): préstamos activos con nombre del prestatario y título del libro. */
    // crud prestamo consultar (consulta dos tablas): GET /api/prestamos/activos (encargado y admin)
    @GetMapping("/activos")
    @RolesPermitidos({Rol.ENCARGADO, Rol.ADMINISTRADOR})
    public List<PrestamoDetalleDTO> listarActivos() {
        return prestamoService.listarActivos();
    }

    /** CONSULTAR (una tabla): historial de un prestatario. Un prestatario solo ve el suyo. */
    // crud prestamo consultar (consulta una tabla): GET /api/prestamos/prestatario/{id}
    @GetMapping("/prestatario/{idPrestatario}")
    @RolesPermitidos({Rol.ENCARGADO, Rol.ADMINISTRADOR, Rol.PRESTATARIO})
    public List<PrestamoDTO> listarPorPrestatario(@PathVariable Integer idPrestatario,
                                                  @RequestAttribute(AutorizacionInterceptor.ATRIBUTO_SESION) Sesion sesion) {
        // permisos prestatario: solo puede ver sus propios prestamos, no los de otro
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
    // crud prestamo actualizar (ampliar): PUT /api/prestamos/{id}/ampliar
    @PutMapping("/{id}/ampliar")
    @RolesPermitidos(Rol.ENCARGADO)
    public PrestamoDTO ampliar(@PathVariable Integer id) {
        return prestamoService.ampliar(id);
    }

    /** ACTUALIZAR: registrar la devolución con el estado físico del libro. */
    // crud prestamo actualizar (devolucion): PUT /api/prestamos/{id}/devolucion
    @PutMapping("/{id}/devolucion")
    @RolesPermitidos(Rol.ENCARGADO)
    public PrestamoDTO devolver(@PathVariable Integer id, @RequestBody DevolucionDTO dto) {
        return prestamoService.devolver(id, dto);
    }

    /** ELIMINAR (lógico): el préstamo queda 'anulado', no se borra la fila. */
    // crud prestamo eliminar: DELETE /api/prestamos/{id}. es eliminacion logica, ver PrestamoService.anular
    @DeleteMapping("/{id}")
    @RolesPermitidos({Rol.ENCARGADO, Rol.ADMINISTRADOR})
    public ResponseEntity<Map<String, String>> anular(@PathVariable Integer id) {
        prestamoService.anular(id);
        return ResponseEntity.ok(Map.of("mensaje", "Préstamo anulado correctamente"));
    }
}
