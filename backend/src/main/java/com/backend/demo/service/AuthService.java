package com.backend.demo.service;

import com.backend.demo.dao.UsuarioDao;
import com.backend.demo.dto.LoginDTO;
import com.backend.demo.dto.LoginRespuestaDTO;
import com.backend.demo.dto.RegistroDTO;
import com.backend.demo.dto.UsuarioDTO;
import com.backend.demo.entity.Usuario;
import com.backend.demo.exception.NegocioException;
import com.backend.demo.seguridad.LimitadorIntentosLogin;
import com.backend.demo.seguridad.Rol;
import com.backend.demo.seguridad.Sesion;
import com.backend.demo.seguridad.ValidadorContrasena;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioDao usuarioDao;
    private final SesionService sesionService;
    private final LimitadorIntentosLogin limitador;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UsuarioDao usuarioDao, SesionService sesionService, LimitadorIntentosLogin limitador) {
        this.usuarioDao = usuarioDao;
        this.sesionService = sesionService;
        this.limitador = limitador;
    }

    public LoginRespuestaDTO login(LoginDTO loginDTO) {
        if (loginDTO.getCorreo() == null || loginDTO.getContrasena() == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Datos incompletos");
        }
        String correo = loginDTO.getCorreo().trim().toLowerCase();

        if (limitador.estaBloqueado(correo)) {
            log.warn("Login bloqueado por intentos fallidos: {}", correo);
            throw new NegocioException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Intente de nuevo en "
                    + LimitadorIntentosLogin.TIEMPO_BLOQUEO.toMinutes() + " minutos");
        }

        Usuario usuario = usuarioDao.findByCorreo(correo).orElse(null);
        if (usuario == null || !passwordEncoder.matches(loginDTO.getContrasena(), usuario.getContrasena())) {
            int intentos = limitador.registrarFallo(correo);
            log.warn("Login fallido ({}/{}): {}", intentos, LimitadorIntentosLogin.MAX_INTENTOS, correo);
            throw new NegocioException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        if ("inactivo".equals(usuario.getEstado())) {
            log.warn("Login rechazado, usuario inactivo: {}", correo);
            throw new NegocioException(HttpStatus.FORBIDDEN, "El usuario se encuentra inactivo");
        }

        limitador.registrarExito(correo);
        String rol = obtenerRol(usuario.getIdUsuario());
        log.info("Login correcto: {} ({})", correo, rol);
        return iniciarSesion(usuario, rol);
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

        log.info("Nuevo prestatario registrado: {}", correo);
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
