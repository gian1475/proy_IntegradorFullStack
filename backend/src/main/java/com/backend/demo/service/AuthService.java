package com.backend.demo.service;

import com.backend.demo.dao.UsuarioDao;
import com.backend.demo.dto.LoginDTO;
import com.backend.demo.dto.LoginRespuestaDTO;
import com.backend.demo.dto.RegistroDTO;
import com.backend.demo.dto.UsuarioDTO;
import com.backend.demo.entity.Usuario;
import com.backend.demo.exception.NegocioException;
import com.backend.demo.seguridad.Rol;
import com.backend.demo.seguridad.Sesion;
import com.backend.demo.seguridad.ValidadorContrasena;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioDao usuarioDao;
    private final SesionService sesionService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UsuarioDao usuarioDao, SesionService sesionService) {
        this.usuarioDao = usuarioDao;
        this.sesionService = sesionService;
    }

    public LoginRespuestaDTO login(LoginDTO loginDTO) {
        if (loginDTO.getCorreo() == null || loginDTO.getContrasena() == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Datos incompletos");
        }

        Usuario usuario = usuarioDao.findByCorreo(loginDTO.getCorreo().trim().toLowerCase())
                .orElseThrow(() -> new NegocioException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"));

        if (!passwordEncoder.matches(loginDTO.getContrasena(), usuario.getContrasena())) {
            throw new NegocioException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        if ("inactivo".equals(usuario.getEstado())) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "El usuario se encuentra inactivo");
        }

        return iniciarSesion(usuario, obtenerRol(usuario.getIdUsuario()));
    }

    @Transactional
    public LoginRespuestaDTO registro(RegistroDTO registroDTO) {
        if (registroDTO.getNombre() == null || registroDTO.getApellido() == null
                || registroDTO.getCorreo() == null || registroDTO.getDni() == null
                || registroDTO.getContrasena() == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Todos los campos obligatorios deben completarse");
        }

        if (!ValidadorContrasena.esValida(registroDTO.getContrasena())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, ValidadorContrasena.MENSAJE_REGLA);
        }

        String correo = registroDTO.getCorreo().trim().toLowerCase();
        String dni = registroDTO.getDni().trim();

        if (usuarioDao.existsByCorreo(correo)) {
            throw new NegocioException(HttpStatus.CONFLICT, "El correo ya se encuentra registrado");
        }
        if (usuarioDao.existsByDni(dni)) {
            throw new NegocioException(HttpStatus.CONFLICT, "El DNI ya se encuentra registrado");
        }

        Usuario nuevo = new Usuario();
        nuevo.setNombre(registroDTO.getNombre());
        nuevo.setApellido(registroDTO.getApellido());
        nuevo.setDni(dni);
        nuevo.setCorreo(correo);
        nuevo.setContrasena(passwordEncoder.encode(registroDTO.getContrasena()));
        nuevo.setEstado("activo");

        Usuario guardado = usuarioDao.save(nuevo);
        usuarioDao.registrarComoPrestatario(guardado.getIdUsuario());

        return iniciarSesion(guardado, Rol.PRESTATARIO);
    }

    public void logout(String token) {
        sesionService.invalidar(token);
    }

    /** El rol se deduce de la tabla de subtipo en la que está el usuario. */
    private String obtenerRol(Integer idUsuario) {
        if (usuarioDao.esAdministrador(idUsuario)) {
            return Rol.ADMINISTRADOR;
        }
        if (usuarioDao.esEncargado(idUsuario)) {
            return Rol.ENCARGADO;
        }
        return Rol.PRESTATARIO;
    }

    private LoginRespuestaDTO iniciarSesion(Usuario usuario, String rol) {
        Sesion sesion = sesionService.crear(usuario.getIdUsuario(), rol);
        UsuarioDTO usuarioDTO = new UsuarioDTO(
            usuario.getIdUsuario(),
            usuario.getNombre(),
            usuario.getApellido(),
            usuario.getDni(),
            usuario.getCorreo(),
            usuario.getEstado(),
            rol
        );
        return new LoginRespuestaDTO(sesion.token(), sesion.expiraEn(), usuarioDTO);
    }
}
