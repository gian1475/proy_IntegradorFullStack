package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.auth.LoginDTO;
import com.alejandria.biblioteca.dto.auth.LoginRespuestaDTO;
import com.alejandria.biblioteca.dto.auth.RegistroDTO;
import com.alejandria.biblioteca.dto.usuario.UsuarioDTO;
import com.alejandria.biblioteca.entity.Usuario;
import com.alejandria.biblioteca.exception.NegocioException;
import com.alejandria.biblioteca.seguridad.LimitadorIntentosLogin;
import com.alejandria.biblioteca.seguridad.Rol;
import com.alejandria.biblioteca.seguridad.Sesion;
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
    private final UsuarioService usuarioService;
    private final SesionService sesionService;
    private final LimitadorIntentosLogin limitador;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UsuarioDao usuarioDao, UsuarioService usuarioService, SesionService sesionService,
                       LimitadorIntentosLogin limitador) {
        this.usuarioDao = usuarioDao;
        this.usuarioService = usuarioService;
        this.sesionService = sesionService;
        this.limitador = limitador;
    }

    // proceso login: 1) ve si el correo esta bloqueado (guava) 2) busca al usuario por correo
    // 3) compara la contra con bcrypt 4) revisa q no este inactivo 5) detecta su rol y le crea la sesion (token)
    public LoginRespuestaDTO login(LoginDTO loginDTO) {
        if (loginDTO.getCorreo() == null || loginDTO.getContrasena() == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Datos incompletos");
        }
        String correo = loginDTO.getCorreo().trim().toLowerCase();

        // guava: si ya fallo 3 veces el correo queda bloqueado 15 min
        if (limitador.estaBloqueado(correo)) {
            log.warn("Login bloqueado por intentos fallidos: {}", correo);
            throw new NegocioException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Intente de nuevo en "
                    + LimitadorIntentosLogin.TIEMPO_BLOQUEO.toMinutes() + " minutos");
        }

        // bcrypt: matches() compara lo q escribio con el hash guardado en la bd. nunca se compara en texto plano
        Usuario usuario = usuarioDao.findByCorreo(correo).orElse(null);
        if (usuario == null || !passwordEncoder.matches(loginDTO.getContrasena(), usuario.getContrasena())) {
            int intentos = limitador.registrarFallo(correo);
            // logback: el login fallido queda guardado en logs/biblioteca.log (sin la contra)
            log.warn("Login fallido ({}/{}): {}", intentos, LimitadorIntentosLogin.MAX_INTENTOS, correo);
            throw new NegocioException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        // un usuario dado de baja (inactivo) ya no puede entrar
        if ("inactivo".equals(usuario.getEstado())) {
            log.warn("Login rechazado, usuario inactivo: {}", correo);
            throw new NegocioException(HttpStatus.FORBIDDEN, "El usuario se encuentra inactivo");
        }

        limitador.registrarExito(correo);
        String rol = obtenerRol(usuario.getIdUsuario());
        log.info("Login correcto: {} ({})", correo, rol);
        return iniciarSesion(usuario, rol);
    }

    // crear perfil 3 (prestatario): todo el q se registra desde la web queda como prestatario.
    // datos q se piden: nombre, apellido, dni, correo y contraseña
    @Transactional
    public LoginRespuestaDTO registro(RegistroDTO registroDTO) {
        // validar datos, regla contra y bcrypt: los hace UsuarioService.crearCuenta (lo mismo q al registrar encargados)
        // crear perfil 3: primero se guarda en la tabla usuario y despues en la tabla prestatario
        Usuario guardado = usuarioService.crearCuenta(registroDTO.getNombre(), registroDTO.getApellido(),
                registroDTO.getDni(), registroDTO.getCorreo(), registroDTO.getContrasena());
        usuarioDao.registrarComoPrestatario(guardado.getIdUsuario());

        log.info("Nuevo prestatario registrado: {}", guardado.getCorreo());
        return iniciarSesion(guardado, Rol.PRESTATARIO);
    }

    public void logout(String token) {
        sesionService.invalidar(token);
    }

    /** El rol se deduce de la tabla de subtipo en la que está el usuario (tiene que estar en una sola). */
    // detectar rol: aqui se sabe q perfil es cada usuario segun la tabla donde esta registrado
    private String obtenerRol(Integer idUsuario) {
        // perfil unico: si el usuario no esta en ninguna tabla de perfil, o esta en dos, la cuenta esta mal armada
        if (usuarioDao.contarPerfiles(idUsuario) != 1) {
            log.warn("Login rechazado, el usuario {} no tiene un único perfil", idUsuario);
            throw new NegocioException(HttpStatus.FORBIDDEN, "La cuenta no tiene un perfil válido. Comuníquese con el administrador");
        }
        if (usuarioDao.esAdministrador(idUsuario)) {
            return Rol.ADMINISTRADOR; // perfil 1: esta en la tabla administrador
        }
        if (usuarioDao.esEncargado(idUsuario)) {
            return Rol.ENCARGADO; // perfil 2: esta en la tabla encargado
        }
        return Rol.PRESTATARIO; // perfil 3: tiene un solo perfil y no es ninguno de los dos, es prestatario
    }

    // token: se crea la sesion y se le devuelve al front el token + los datos del usuario con su rol
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
