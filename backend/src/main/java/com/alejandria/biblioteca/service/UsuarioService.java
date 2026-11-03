package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dao.ReservaDao;
import com.alejandria.biblioteca.dao.SancionDao;
import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.usuario.ActualizarUsuarioDTO;
import com.alejandria.biblioteca.dto.usuario.EncargadoDTO;
import com.alejandria.biblioteca.dto.usuario.RegistroEncargadoDTO;
import com.alejandria.biblioteca.dto.usuario.UsuarioAdminDTO;
import com.alejandria.biblioteca.entity.Reserva;
import com.alejandria.biblioteca.entity.Usuario;
import com.alejandria.biblioteca.exception.NegocioException;
import com.alejandria.biblioteca.seguridad.Rol;
import com.alejandria.biblioteca.seguridad.ValidadorContrasena;
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
 * El admin también edita los datos (RF12), da de baja (RF13) y reactiva a prestatarios y encargados.
 */
@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    /** Turnos de trabajo del encargado (diccionario de datos: "mañana" o "tarde"). */
    public static final List<String> TURNOS = List.of("mañana", "tarde");

    /** RF13: préstamos con el libro todavía en sala; con uno de estos no se da de baja al prestatario. */
    private static final List<String> PRESTAMOS_EN_CURSO = List.of("activo", "vencido");
    /** RF13: reservas que se cancelan al dar de baja (liberan el ejemplar o el puesto en la cola). */
    private static final List<String> RESERVAS_VIGENTES = List.of("pendiente", ColaEsperaService.EN_ESPERA);
    private static final String INACTIVO = "inactivo";

    private final UsuarioDao usuarioDao;
    private final PrestamoDao prestamoDao;
    private final ReservaDao reservaDao;
    private final ReservaService reservaService;
    private final SancionDao sancionDao;
    private final SesionService sesionService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UsuarioService(UsuarioDao usuarioDao, PrestamoDao prestamoDao, ReservaDao reservaDao,
                          ReservaService reservaService, SancionDao sancionDao, SesionService sesionService) {
        this.usuarioDao = usuarioDao;
        this.prestamoDao = prestamoDao;
        this.reservaDao = reservaDao;
        this.reservaService = reservaService;
        this.sancionDao = sancionDao;
        this.sesionService = sesionService;
    }

    /**
     * Guarda la fila en la tabla usuario (activo). El perfil se agrega después.
     * El formato de los datos ya lo revisaron las anotaciones del DTO (@Valid en el controller).
     */
    @Transactional
    public Usuario crearCuenta(String nombre, String apellido, String dni, String correo, String contrasena) {
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
        String turno = validarTurno(dto.turno());

        Usuario guardado = crearCuenta(dto.nombre(), dto.apellido(), dto.dni(), dto.correo(), dto.contrasena());
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

    // ---------- RF12 y RF13: gestionar usuarios (solo prestatarios y encargados) ----------

    /** Prestatarios y encargados para la pestaña "Usuarios" del admin. */
    @Transactional(readOnly = true)
    public List<UsuarioAdminDTO> listarUsuarios() {
        return usuarioDao.listarUsuarios().stream()
                .map(f -> new UsuarioAdminDTO(((Number) f[0]).intValue(), (String) f[1], (String) f[2],
                        (String) f[3], (String) f[4], (String) f[5], (String) f[6], (String) f[7]))
                .toList();
    }

    // RF12 editar usuario: cambia nombres, dni, correo y (si es encargado) el turno.
    // criterio de aceptacion: el dni y el correo siguen siendo unicos -> no pueden ser los de OTRO usuario.
    // la contraseña y el estado no se tocan aqui
    @Transactional
    public UsuarioAdminDTO actualizar(Integer idUsuario, ActualizarUsuarioDTO dto) {
        Usuario usuario = buscar(idUsuario);
        String perfil = perfilEditable(usuario);
        String turno = Rol.ENCARGADO.equals(perfil) ? validarTurno(dto.turno()) : null;

        String dniLimpio = dto.dni().trim();
        String correoLimpio = dto.correo().trim().toLowerCase();
        if (usuarioDao.existsByDniAndIdUsuarioNot(dniLimpio, idUsuario)) {
            throw new NegocioException(HttpStatus.CONFLICT, "El DNI ya se encuentra registrado");
        }
        if (usuarioDao.existsByCorreoAndIdUsuarioNot(correoLimpio, idUsuario)) {
            throw new NegocioException(HttpStatus.CONFLICT, "El correo ya se encuentra registrado");
        }

        usuario.setNombre(dto.nombre().trim());
        usuario.setApellido(dto.apellido().trim());
        usuario.setDni(dniLimpio);
        usuario.setCorreo(correoLimpio);
        usuarioDao.save(usuario);
        if (turno != null) {
            usuarioDao.cambiarTurno(idUsuario, turno);
        }

        log.info("Usuario #{} editado por el admin ({})", idUsuario, perfil);
        return aDto(usuario, perfil, turno);
    }

    // RF13 eliminacion logica: el usuario NO se borra, pasa a 'inactivo' y su historial se conserva.
    // prestatario: si tiene un libro en sala (prestamo activo o vencido) no se da de baja hasta q lo devuelva;
    // sus reservas pendientes o en cola se cancelan (el ejemplar vuelve al stock o pasa al siguiente de la cola).
    // encargado: sus prestamos registrados quedan en el historial con su nombre.
    // un inactivo no puede iniciar sesion (AuthService) y se le cierra la sesion q tenga abierta
    @Transactional
    public UsuarioAdminDTO darDeBaja(Integer idUsuario) {
        Usuario usuario = buscar(idUsuario);
        String perfil = perfilEditable(usuario);
        if (INACTIVO.equals(usuario.getEstado())) {
            throw new NegocioException(HttpStatus.CONFLICT, "El usuario ya está inactivo");
        }

        if (Rol.PRESTATARIO.equals(perfil)) {
            if (prestamoDao.existsByPrestatarioIdUsuarioAndEstadoPrestamoIn(idUsuario, PRESTAMOS_EN_CURSO)) {
                throw new NegocioException(HttpStatus.CONFLICT,
                        "El prestatario tiene un libro prestado. Registre la devolución antes de darlo de baja");
            }
            for (Reserva r : reservaDao.findByPrestatarioIdUsuarioAndEstadoReservaIn(idUsuario, RESERVAS_VIGENTES)) {
                reservaService.cancelar(r.getIdReserva());
            }
        }

        usuario.setEstado(INACTIVO);
        usuarioDao.save(usuario);
        sesionService.cerrarSesionesDe(idUsuario);

        log.info("Usuario #{} dado de baja por el admin ({}): estado 'inactivo'", idUsuario, perfil);
        return aDto(usuario, perfil, turnoDe(idUsuario, perfil));
    }

    // reactivar: deshace una baja. si al prestatario le queda una suspension activa (RF06) vuelve a 'suspendido'
    @Transactional
    public UsuarioAdminDTO reactivar(Integer idUsuario) {
        Usuario usuario = buscar(idUsuario);
        String perfil = perfilEditable(usuario);
        if (!INACTIVO.equals(usuario.getEstado())) {
            throw new NegocioException(HttpStatus.CONFLICT, "El usuario no está inactivo");
        }

        boolean suspendido = Rol.PRESTATARIO.equals(perfil) && sancionDao.tieneSuspensionActiva(idUsuario);
        usuario.setEstado(suspendido ? "suspendido" : "activo");
        usuarioDao.save(usuario);

        log.info("Usuario #{} reactivado por el admin: estado '{}'", idUsuario, usuario.getEstado());
        return aDto(usuario, perfil, turnoDe(idUsuario, perfil));
    }

    private Usuario buscar(Integer idUsuario) {
        return usuarioDao.findById(idUsuario)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    /** Solo se gestionan encargados y prestatarios; al administrador no (así no se deja fuera a sí mismo). */
    private String perfilEditable(Usuario usuario) {
        if (usuarioDao.esEncargado(usuario.getIdUsuario())) {
            return Rol.ENCARGADO;
        }
        if (usuarioDao.esPrestatario(usuario.getIdUsuario())) {
            return Rol.PRESTATARIO;
        }
        throw new NegocioException(HttpStatus.FORBIDDEN, "Solo se pueden gestionar prestatarios y encargados");
    }

    private String validarTurno(String turnoEscrito) {
        String turno = turnoEscrito == null ? "" : turnoEscrito.trim().toLowerCase();
        if (!TURNOS.contains(turno)) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "El turno debe ser 'mañana' o 'tarde'");
        }
        return turno;
    }

    private String turnoDe(Integer idUsuario, String perfil) {
        return Rol.ENCARGADO.equals(perfil) ? usuarioDao.turnoDe(idUsuario) : null;
    }

    private UsuarioAdminDTO aDto(Usuario u, String perfil, String turno) {
        return new UsuarioAdminDTO(u.getIdUsuario(), u.getNombre(), u.getApellido(), u.getDni(), u.getCorreo(),
                u.getEstado(), perfil, turno);
    }
}
