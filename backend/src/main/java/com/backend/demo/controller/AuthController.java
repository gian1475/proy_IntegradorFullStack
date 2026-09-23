package com.backend.demo.controller;

import com.backend.demo.dao.UsuarioDao;
import com.backend.demo.dto.LoginDTO;
import com.backend.demo.dto.RegistroDTO;
import com.backend.demo.dto.UsuarioDTO;
import com.backend.demo.entity.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UsuarioDao usuarioDao;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public AuthController(UsuarioDao usuarioDao, JdbcTemplate jdbcTemplate) {
        this.usuarioDao = usuarioDao;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO) {
        if (loginDTO.getCorreo() == null || loginDTO.getContrasena() == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Datos incompletos"));
        }

        Optional<Usuario> usuarioOpt = usuarioDao.findByCorreo(loginDTO.getCorreo().trim());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "Credenciales incorrectas"));
        }

        Usuario usuario = usuarioOpt.get();
        boolean passOk = false;

        if (usuario.getContrasena().startsWith("$2a$") || usuario.getContrasena().startsWith("$2b$")) {
            passOk = passwordEncoder.matches(loginDTO.getContrasena(), usuario.getContrasena());
        } else {
            passOk = usuario.getContrasena().equals(loginDTO.getContrasena());
        }

        if (!passOk) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "Credenciales incorrectas"));
        }

        String rol = "PRESTATARIO";
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM administrador WHERE id_usuario = ?", Integer.class, usuario.getIdUsuario());
            if (count != null && count > 0) rol = "ADMINISTRADOR";
        } catch (Exception ignored) {}

        UsuarioDTO response = new UsuarioDTO(
            usuario.getIdUsuario(),
            usuario.getNombre(),
            usuario.getApellido(),
            usuario.getDni(),
            usuario.getCorreo(),
            usuario.getEstado(),
            rol
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody RegistroDTO registroDTO) {
        if (registroDTO.getCorreo() == null || registroDTO.getDni() == null || registroDTO.getContrasena() == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Todos los campos obligatorios deben completarse"));
        }

        if (usuarioDao.existsByCorreo(registroDTO.getCorreo().trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", "El correo ya se encuentra registrado"));
        }

        if (usuarioDao.existsByDni(registroDTO.getDni().trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", "El DNI ya se encuentra registrado"));
        }

        Usuario nuevo = new Usuario();
        nuevo.setNombre(registroDTO.getNombre());
        nuevo.setApellido(registroDTO.getApellido());
        nuevo.setDni(registroDTO.getDni().trim());
        nuevo.setCorreo(registroDTO.getCorreo().trim().toLowerCase());
        nuevo.setContrasena(passwordEncoder.encode(registroDTO.getContrasena()));
        nuevo.setEstado("activo");

        Usuario guardado = usuarioDao.save(nuevo);

        try {
            jdbcTemplate.update("INSERT INTO prestatario (id_usuario, tiempos_suspendido) VALUES (?, 0) ON CONFLICT DO NOTHING", guardado.getIdUsuario());
        } catch (Exception ignored) {}

        UsuarioDTO response = new UsuarioDTO(
            guardado.getIdUsuario(),
            guardado.getNombre(),
            guardado.getApellido(),
            guardado.getDni(),
            guardado.getCorreo(),
            guardado.getEstado(),
            "PRESTATARIO"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
