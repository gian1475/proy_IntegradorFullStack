package com.alejandria.biblioteca.controller;

import com.alejandria.biblioteca.dto.reserva.ReservaDTO;
import com.alejandria.biblioteca.dto.reserva.ReservaRespuestaDTO;
import com.alejandria.biblioteca.exception.NegocioException;
import com.alejandria.biblioteca.seguridad.AutorizacionInterceptor;
import com.alejandria.biblioteca.seguridad.Rol;
import com.alejandria.biblioteca.seguridad.RolesPermitidos;
import com.alejandria.biblioteca.seguridad.Sesion;
import com.alejandria.biblioteca.service.ReservaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    // permisos admin / permisos encargado: ellos ven todas las reservas, el prestatario no
    @GetMapping
    @RolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
    public List<ReservaRespuestaDTO> listarTodas() {
        return reservaService.listarTodas();
    }

    @GetMapping("/usuario/{idUsuario}")
    @RolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO, Rol.PRESTATARIO})
    public List<ReservaRespuestaDTO> listarPorUsuario(@PathVariable Integer idUsuario,
                                                      @RequestAttribute(AutorizacionInterceptor.ATRIBUTO_SESION) Sesion sesion) {
        verificarPropietario(sesion, idUsuario);
        return reservaService.listarPorUsuario(idUsuario);
    }

    @PostMapping
    @RolesPermitidos({Rol.ENCARGADO, Rol.PRESTATARIO})
    public ResponseEntity<ReservaRespuestaDTO> crearReserva(@RequestBody ReservaDTO dto,
                                                            @RequestAttribute(AutorizacionInterceptor.ATRIBUTO_SESION) Sesion sesion) {
        verificarPropietario(sesion, dto.getIdUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.crear(dto));
    }

    @PutMapping("/{id}/cancelar")
    @RolesPermitidos({Rol.ENCARGADO, Rol.PRESTATARIO})
    public ResponseEntity<Map<String, String>> cancelarReserva(@PathVariable Integer id,
                                                               @RequestAttribute(AutorizacionInterceptor.ATRIBUTO_SESION) Sesion sesion) {
        verificarPropietario(sesion, reservaService.obtenerIdPrestatario(id));
        reservaService.cancelar(id);
        return ResponseEntity.ok(Map.of("mensaje", "Reserva cancelada correctamente"));
    }

    // eliminacion fisica: DELETE /api/reservas/{id}, solo el admin (RF17).
    // comparar con cancelar (PUT .../cancelar): esa es logica, solo cambia el estado
    @DeleteMapping("/{id}")
    @RolesPermitidos(Rol.ADMINISTRADOR)
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable Integer id) {
        reservaService.eliminar(id);
        return ResponseEntity.ok(Map.of("mensaje", "Reserva eliminada de la base de datos"));
    }

    /** Un prestatario solo puede ver o modificar sus propias reservas. */
    // permisos prestatario: puede ver, crear y cancelar reservas pero SOLO las suyas (si no -> 403)
    private void verificarPropietario(Sesion sesion, Integer idUsuario) {
        if (Rol.PRESTATARIO.equals(sesion.rol()) && !sesion.idUsuario().equals(idUsuario)) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "Solo puede gestionar sus propias reservas");
        }
    }
}
