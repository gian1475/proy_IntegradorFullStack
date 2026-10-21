package com.backend.demo.service;

import com.backend.demo.dao.PrestamoDao;
import com.backend.demo.dao.UsuarioDao;
import com.backend.demo.entity.Usuario;
import com.backend.demo.exception.NegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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
    public void validarHabilitado(Usuario usuario) {
        if (!usuarioDao.esPrestatario(usuario.getIdUsuario())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "El usuario no está registrado como prestatario");
        }
        if (!"activo".equals(usuario.getEstado())) {
            throw new NegocioException(HttpStatus.CONFLICT, "El prestatario está " + usuario.getEstado());
        }
        if (prestamoDao.tieneSancionActiva(usuario.getIdUsuario())) {
            throw new NegocioException(HttpStatus.CONFLICT, "El prestatario tiene una sanción activa");
        }
    }
}
