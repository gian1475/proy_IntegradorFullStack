package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.usuario.EncargadoDTO;
import com.alejandria.biblioteca.dto.usuario.RegistroEncargadoDTO;
import com.alejandria.biblioteca.entity.Usuario;
import com.alejandria.biblioteca.exception.NegocioException;
import com.alejandria.biblioteca.seguridad.ValidadorContrasena;
import com.alejandria.biblioteca.seguridad.ValidadorUsuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cuentas de usuario. crearCuenta la usan el registro del prestatario (AuthService) y el registro
 * de encargados que hace el admin (RF09), para validar y guardar igual en los dos casos.
 */
@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    /** Turnos de trabajo del encargado (diccionario de datos: "mañana" o "tarde"). */
    public static final List<String> TURNOS = List.of("mañana", "tarde");

    private final UsuarioDao usuarioDao;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UsuarioService(UsuarioDao usuarioDao) {
        this.usuarioDao = usuarioDao;
    }

    /** Valida los datos y guarda la fila en la tabla usuario (activo). El perfil se agrega después. */
    @Transactional
    public Usuario crearCuenta(String nombre, String apellido, String dni, String correo, String contrasena) {
        if (nombre == null || apellido == null || correo == null || dni == null || contrasena == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Todos los campos obligatorios deben completarse");
        }

        // validar datos: dni de 8 digitos, correo bien escrito y largos q entren en la bd
        ValidadorUsuario.validar(nombre, apellido, dni, correo);

        // regla contra: el backend vuelve a validar la contraseña aunque el front ya lo hizo
        if (!ValidadorContrasena.esValida(contrasena)) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, ValidadorContrasena.MENSAJE_REGLA);
        }

        String correoLimpio = correo.trim().toLowerCase();
        String dniLimpio = dni.trim();
        if (usuarioDao.existsByCorreo(correoLimpio)) {
            throw new NegocioException(HttpStatus.CONFLICT, "El correo ya se encuentra registrado");
        }
        if (usuarioDao.existsByDni(dniLimpio)) {
            throw new NegocioException(HttpStatus.CONFLICT, "El DNI ya se encuentra registrado");
        }

        Usuario nuevo = new Usuario();
        nuevo.setNombre(nombre.trim());
        nuevo.setApellido(apellido.trim());
        nuevo.setDni(dniLimpio);
        nuevo.setCorreo(correoLimpio);
        // bcrypt: la contra se guarda encriptada (hash), en la bd nunca queda la contra real
        nuevo.setContrasena(passwordEncoder.encode(contrasena));
        nuevo.setEstado("activo");
        return usuarioDao.save(nuevo);
    }

    // crear perfil 2 (encargado): el admin lo registra desde su panel (RF09). antes solo se podia con el script sql.
    // primero se guarda en la tabla usuario y despues en la tabla encargado con su turno
    @Transactional
    public EncargadoDTO registrarEncargado(RegistroEncargadoDTO dto) {
        String turno = dto.getTurno() == null ? "" : dto.getTurno().trim().toLowerCase();
        if (!TURNOS.contains(turno)) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "El turno debe ser 'mañana' o 'tarde'");
        }

        Usuario guardado = crearCuenta(dto.getNombre(), dto.getApellido(), dto.getDni(), dto.getCorreo(), dto.getContrasena());
        usuarioDao.registrarComoEncargado(guardado.getIdUsuario(), turno);

        log.info("Nuevo encargado registrado por el admin: {} (turno {})", guardado.getCorreo(), turno);
        return new EncargadoDTO(guardado.getIdUsuario(), guardado.getNombre(), guardado.getApellido(),
                guardado.getDni(), guardado.getCorreo(), guardado.getEstado(), turno);
    }

    @Transactional(readOnly = true)
    public List<EncargadoDTO> listarEncargados() {
        return usuarioDao.listarEncargados().stream()
                .map(f -> new EncargadoDTO(((Number) f[0]).intValue(), (String) f[1], (String) f[2],
                        (String) f[3], (String) f[4], (String) f[5], (String) f[6]))
                .toList();
    }
}
