package com.backend.demo.controller;

import com.backend.demo.dto.LoginDTO;
import com.backend.demo.dto.LoginRespuestaDTO;
import com.backend.demo.dto.RegistroDTO;
import com.backend.demo.service.AuthService;
import com.backend.demo.service.SesionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private final AuthService authService;

    @Autowired
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
