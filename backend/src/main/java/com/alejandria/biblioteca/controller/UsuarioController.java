package com.alejandria.biblioteca.controller;

import com.alejandria.biblioteca.dto.usuario.EncargadoDTO;
import com.alejandria.biblioteca.dto.usuario.RegistroEncargadoDTO;
import com.alejandria.biblioteca.seguridad.Rol;
import com.alejandria.biblioteca.service.UsuarioService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ruta usuarios: /api/usuarios. permisos admin: solo el admin registra y ve a los encargados (RF09)
@RestController
@RequestMapping("/api/usuarios/encargados")
@RolesAllowed(Rol.ADMINISTRADOR)
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<EncargadoDTO> listarEncargados() {
        return usuarioService.listarEncargados();
    }

    // crear perfil 2 (encargado): POST /api/usuarios/encargados
    @PostMapping
    public ResponseEntity<EncargadoDTO> registrarEncargado(@Valid @RequestBody RegistroEncargadoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrarEncargado(dto));
    }
}
