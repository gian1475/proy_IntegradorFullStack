package com.alejandria.biblioteca.controller;

import com.alejandria.biblioteca.dto.auth.LoginDTO;
import com.alejandria.biblioteca.dto.auth.LoginRespuestaDTO;
import com.alejandria.biblioteca.dto.auth.RegistroDTO;
import com.alejandria.biblioteca.service.AuthService;
import com.alejandria.biblioteca.service.SesionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginRespuestaDTO> login(@RequestBody LoginDTO loginDTO) {
        return ResponseEntity.ok(authService.login(loginDTO));
    }

    @PostMapping("/registro")
    public ResponseEntity<LoginRespuestaDTO> registro(@RequestBody RegistroDTO registroDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registro(registroDTO));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(SesionService.tokenDesdeHeader(authorization));
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada"));
    }
}
