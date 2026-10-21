package com.backend.demo.controller;

import com.backend.demo.dto.ReservaDTO;
import com.backend.demo.dto.ReservaRespuestaDTO;
import com.backend.demo.exception.NegocioException;
import com.backend.demo.seguridad.AutorizacionInterceptor;
import com.backend.demo.seguridad.Rol;
import com.backend.demo.seguridad.RolesPermitidos;
import com.backend.demo.seguridad.Sesion;
import com.backend.demo.service.ReservaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservas")
@CrossOrigin(origins = "http://localhost:4200")
public class ReservaController {

    private final ReservaService reservaService;

    @Autowired
    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

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

    /** Un prestatario solo puede ver o modificar sus propias reservas. */
    private void verificarPropietario(Sesion sesion, Integer idUsuario) {
        if (Rol.PRESTATARIO.equals(sesion.rol()) && !sesion.idUsuario().equals(idUsuario)) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "Solo puede gestionar sus propias reservas");
        }
    }
}
