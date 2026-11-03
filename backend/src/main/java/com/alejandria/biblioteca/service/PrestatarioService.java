package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dao.SancionDao;
import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.entity.Sancion;
import com.alejandria.biblioteca.entity.Usuario;
import com.alejandria.biblioteca.exception.NegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Reglas sobre quién puede pedir libros (la usan préstamos, reservas e incidencias). */
@Service
public class PrestatarioService {

    private static final Logger log = LoggerFactory.getLogger(PrestatarioService.class);

    private final UsuarioDao usuarioDao;
    private final PrestamoDao prestamoDao;
    private final SancionDao sancionDao;
    private final HorarioAtencion horario;

    public PrestatarioService(UsuarioDao usuarioDao, PrestamoDao prestamoDao, SancionDao sancionDao, HorarioAtencion horario) {
        this.usuarioDao = usuarioDao;
        this.prestamoDao = prestamoDao;
        this.sancionDao = sancionDao;
        this.horario = horario;
    }

    /** El usuario debe ser prestatario, estar activo y no tener sanciones activas. */
    // sancion activa: antes de prestar o reservar se revisa q sea prestatario, activo y sin sancion.
    // primero se cierran las suspensiones q ya terminaron, para q no siga 'suspendido' de mas
    public void validarHabilitado(Usuario usuario) {
        cerrarSuspensionesTerminadas();
        if (!usuarioDao.esPrestatario(usuario.getIdUsuario())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "El usuario no está registrado como prestatario");
        }
        if (!"activo".equals(usuario.getEstado())) {
            throw new NegocioException(HttpStatus.CONFLICT, "El prestatario está " + usuario.getEstado());
        }
        if (prestamoDao.tieneSancionActiva(usuario.getIdUsuario(), LocalDate.now())) {
            throw new NegocioException(HttpStatus.CONFLICT, "El prestatario tiene una sanción activa");
        }
    }

    /** Las suspensiones cuya fecha de fin ya pasó quedan 'cumplida' y el prestatario recupera el servicio. */
    // fin de la suspension (RF06): "al cumplirla se restituye el servicio si no existe otra sancion bloqueante".
    // no hay un proceso q corra solo: se revisa antes de prestar, reservar y listar las incidencias
    @Transactional
    public void cerrarSuspensionesTerminadas() {
        for (Sancion s : sancionDao.suspensionesTerminadas(horario.ahora())) {
            s.setEstadoSancion("cumplida");
            s.getIncidencia().setEstadoIncidencia("resuelta");
            sancionDao.save(s);
            restablecer(s.getIncidencia().getPrestamo().getPrestatario());
            log.info("Sanción #{} cumplida: terminó la suspensión", s.getIdSancion());
        }
    }

    /** Si al prestatario suspendido ya no le queda ninguna suspensión activa, vuelve a 'activo'. */
    // la suspension del usuario corresponde a su sancion: sin suspension activa, deja de estar 'suspendido'.
    // una multa pendiente no lo suspende, pero igual lo bloquea (tieneSancionActiva)
    public void restablecer(Usuario prestatario) {
        if ("suspendido".equals(prestatario.getEstado()) && !sancionDao.tieneSuspensionActiva(prestatario.getIdUsuario())) {
            prestatario.setEstado("activo");
            usuarioDao.save(prestatario);
        }
    }
}
