package com.alejandria.biblioteca.controller;

import com.alejandria.biblioteca.dto.usuario.ActualizarUsuarioDTO;
import com.alejandria.biblioteca.dto.usuario.EncargadoDTO;
import com.alejandria.biblioteca.dto.usuario.RegistroEncargadoDTO;
import com.alejandria.biblioteca.dto.usuario.RestablecerContrasenaDTO;
import com.alejandria.biblioteca.dto.usuario.UsuarioAdminDTO;
import com.alejandria.biblioteca.seguridad.Rol;
import com.alejandria.biblioteca.service.UsuarioService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ruta usuarios: /api/usuarios. permisos admin: solo el admin registra encargados (RF09),
// edita los datos de un usuario (RF12) y lo da de baja (RF13)
@RestController
@RequestMapping("/api/usuarios")
@RolesAllowed(Rol.ADMINISTRADOR)
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/encargados")
    public List<EncargadoDTO> listarEncargados() {
        return usuarioService.listarEncargados();
    }

    // crear perfil 2 (encargado): POST /api/usuarios/encargados
    @PostMapping("/encargados")
    public ResponseEntity<EncargadoDTO> registrarEncargado(@Valid @RequestBody RegistroEncargadoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrarEncargado(dto));
    }

    // RF12 y RF13: prestatarios y encargados (el admin no sale en la lista)
    @GetMapping
    public List<UsuarioAdminDTO> listarUsuarios() {
        return usuarioService.listarUsuarios();
    }

    // RF12 editar usuario: PUT /api/usuarios/{id}
    @PutMapping("/{id}")
    public UsuarioAdminDTO actualizar(@PathVariable Integer id, @Valid @RequestBody ActualizarUsuarioDTO dto) {
        return usuarioService.actualizar(id, dto);
    }

    // RF13 baja logica: DELETE igual q libros y prestamos, pero la fila NO se borra: el estado pasa a 'inactivo'
    @DeleteMapping("/{id}")
    public UsuarioAdminDTO darDeBaja(@PathVariable Integer id) {
        return usuarioService.darDeBaja(id);
    }

    // restablecer contra (B7): PUT /api/usuarios/{id}/contrasena
    @PutMapping("/{id}/contrasena")
    public UsuarioAdminDTO restablecerContrasena(@PathVariable Integer id, @Valid @RequestBody RestablecerContrasenaDTO dto) {
        return usuarioService.restablecerContrasena(id, dto);
    }

    @PutMapping("/{id}/reactivar")
    public UsuarioAdminDTO reactivar(@PathVariable Integer id) {
        return usuarioService.reactivar(id);
    }
}
