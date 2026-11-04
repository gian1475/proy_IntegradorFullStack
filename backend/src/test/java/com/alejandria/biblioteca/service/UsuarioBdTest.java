package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.usuario.EncargadoDTO;
import com.alejandria.biblioteca.dto.usuario.ActualizarUsuarioDTO;
import com.alejandria.biblioteca.dto.usuario.RegistroEncargadoDTO;
import com.alejandria.biblioteca.dto.usuario.UsuarioAdminDTO;
import com.alejandria.biblioteca.entity.Usuario;
import com.alejandria.biblioteca.exception.NegocioException;
import com.alejandria.biblioteca.seguridad.Rol;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Registro de un encargado (RF09), edición (RF12) y baja lógica (RF13) contra la BD real (Neon).
 * Corre dentro de una transacción que se deshace al final: no deja datos guardados.
 */
@DisplayName("Usuarios en la BD: registro de encargados (RF09), edición (RF12) y baja lógica (RF13)")
@SpringBootTest
@Transactional
class UsuarioBdTest {

    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioDao usuarioDao;
    @PersistenceContext private EntityManager em;

    @Test
    void elEncargadoQuedaEnLaTablaEncargadoConSuTurno() {
        RegistroEncargadoDTO dto = new RegistroEncargadoDTO("María", "Prueba", "90000099",
                "maria.prueba@alejandria.edu.pe", "Biblioteca#2026", "tarde");

        EncargadoDTO nuevo = usuarioService.registrarEncargado(dto);
        em.flush();

        assertEquals("tarde", em.createNativeQuery("SELECT turno FROM encargado WHERE id_usuario = :id")
                .setParameter("id", nuevo.idUsuario()).getSingleResult());
        assertEquals(1, usuarioDao.contarPerfiles(nuevo.idUsuario())); // perfil unico
        assertTrue(usuarioDao.esEncargado(nuevo.idUsuario()));
        assertTrue(usuarioService.listarEncargados().stream().anyMatch(e -> e.idUsuario().equals(nuevo.idUsuario())));
    }

    @Test
    void laBajaEsLogicaYCancelaLaReservaPendiente() {
        Usuario lector = prestatarioDePrueba("90000098", "lector.prueba@alejandria.edu.pe");
        Integer idLibro = ((Number) em.createNativeQuery(
                "SELECT id_libro FROM libro WHERE estado_libro <> 'baja' ORDER BY id_libro LIMIT 1").getSingleResult()).intValue();
        Integer idReserva = ((Number) em.createNativeQuery("INSERT INTO reserva (id_prestatario, id_libro, fecha_recojo_limite, estado_reserva) "
                + "VALUES (:p, :l, CURRENT_TIMESTAMP + INTERVAL '1 day', 'pendiente') RETURNING id_reserva")
                .setParameter("p", lector.getIdUsuario()).setParameter("l", idLibro).getSingleResult()).intValue();

        usuarioService.darDeBaja(lector.getIdUsuario());
        em.flush();

        // la fila sigue en la bd (eliminación lógica) con el estado 'inactivo'
        assertEquals("inactivo", em.createNativeQuery("SELECT estado FROM usuario WHERE id_usuario = :id")
                .setParameter("id", lector.getIdUsuario()).getSingleResult());
        assertEquals("cancelada", em.createNativeQuery("SELECT estado_reserva FROM reserva WHERE id_reserva = :id")
                .setParameter("id", idReserva).getSingleResult());
        // aparece en el reporte de bajas lógicas
        assertTrue(usuarioDao.findByEstadoOrderByApellidoAscNombreAsc("inactivo").stream()
                .anyMatch(u -> u.getIdUsuario().equals(lector.getIdUsuario())));

        usuarioService.reactivar(lector.getIdUsuario());
        em.flush();
        assertEquals("activo", em.createNativeQuery("SELECT estado FROM usuario WHERE id_usuario = :id")
                .setParameter("id", lector.getIdUsuario()).getSingleResult());
    }

    @Test
    void alEditarNoSePuedeUsarElDniDeOtroUsuario() {
        Usuario uno = prestatarioDePrueba("90000097", "uno.prueba@alejandria.edu.pe");
        prestatarioDePrueba("90000096", "dos.prueba@alejandria.edu.pe");

        NegocioException e = assertThrows(NegocioException.class, () -> usuarioService.actualizar(uno.getIdUsuario(),
                new ActualizarUsuarioDTO("Uno", "Prueba", "90000096", "uno.prueba@alejandria.edu.pe", null)));
        assertEquals(409, e.getEstado().value());

        // su propio dni y correo si los puede volver a guardar
        UsuarioAdminDTO editado = usuarioService.actualizar(uno.getIdUsuario(),
                new ActualizarUsuarioDTO("Uno Editado", "Prueba", "90000097", "uno.prueba@alejandria.edu.pe", null));
        em.flush();
        assertEquals("Uno Editado", em.createNativeQuery("SELECT nombre FROM usuario WHERE id_usuario = :id")
                .setParameter("id", uno.getIdUsuario()).getSingleResult());
        assertEquals(Rol.PRESTATARIO, editado.perfil());
        assertTrue(usuarioService.listarUsuarios().stream().anyMatch(u -> u.idUsuario().equals(uno.getIdUsuario())
                && Rol.PRESTATARIO.equals(u.perfil())));
    }

    @Test
    void alEncargadoSeLeCambiaElTurnoEnLaTablaEncargado() {
        EncargadoDTO nuevo = usuarioService.registrarEncargado(new RegistroEncargadoDTO("Luis", "Prueba", "90000095",
                "luis.prueba@alejandria.edu.pe", "Biblioteca#2026", "mañana"));

        usuarioService.actualizar(nuevo.idUsuario(),
                new ActualizarUsuarioDTO("Luis", "Prueba", "90000095", "luis.prueba@alejandria.edu.pe", "tarde"));
        em.flush();

        assertEquals("tarde", em.createNativeQuery("SELECT turno FROM encargado WHERE id_usuario = :id")
                .setParameter("id", nuevo.idUsuario()).getSingleResult());
    }

    private Usuario prestatarioDePrueba(String dni, String correo) {
        Usuario u = usuarioService.crearCuenta("Lector", "Prueba", dni, correo, "Biblioteca#2026");
        usuarioDao.registrarComoPrestatario(u.getIdUsuario());
        return u;
    }
}
