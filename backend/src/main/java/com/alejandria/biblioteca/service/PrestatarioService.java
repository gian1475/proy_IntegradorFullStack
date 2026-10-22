package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.entity.Usuario;
import com.alejandria.biblioteca.exception.NegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/** Reglas sobre quién puede pedir libros (la usan préstamos y reservas). */
@Service
public class PrestatarioService {

    private final UsuarioDao usuarioDao;
    private final PrestamoDao prestamoDao;

    public PrestatarioService(UsuarioDao usuarioDao, PrestamoDao prestamoDao) {
        this.usuarioDao = usuarioDao;
        this.prestamoDao = prestamoDao;
    }

    /** El usuario debe ser prestatario, estar activo y no tener sanciones activas. */
    // sancion activa: antes de prestar o reservar se revisa q sea prestatario, activo y sin sancion
    public void validarHabilitado(Usuario usuario) {
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
}
