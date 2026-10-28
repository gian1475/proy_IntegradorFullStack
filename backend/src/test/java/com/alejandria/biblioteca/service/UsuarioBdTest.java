package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.usuario.EncargadoDTO;
import com.alejandria.biblioteca.dto.usuario.RegistroEncargadoDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Registro de un encargado contra la BD real (Neon): queda en usuario y en encargado, con un solo perfil.
 * Corre dentro de una transacción que se deshace al final: no deja datos guardados.
 */
@DisplayName("Registro de encargados en la BD (RF09)")
@SpringBootTest
@Transactional
class UsuarioBdTest {

    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioDao usuarioDao;
    @PersistenceContext private EntityManager em;

    @Test
    void elEncargadoQuedaEnLaTablaEncargadoConSuTurno() {
        RegistroEncargadoDTO dto = new RegistroEncargadoDTO();
        dto.setNombre("María");
        dto.setApellido("Prueba");
        dto.setDni("90000099");
        dto.setCorreo("maria.prueba@alejandria.edu.pe");
        dto.setContrasena("Biblioteca#2026");
        dto.setTurno("tarde");

        EncargadoDTO nuevo = usuarioService.registrarEncargado(dto);
        em.flush();

        assertEquals("tarde", em.createNativeQuery("SELECT turno FROM encargado WHERE id_usuario = :id")
                .setParameter("id", nuevo.idUsuario()).getSingleResult());
        assertEquals(1, usuarioDao.contarPerfiles(nuevo.idUsuario())); // perfil unico
        assertTrue(usuarioDao.esEncargado(nuevo.idUsuario()));
        assertTrue(usuarioService.listarEncargados().stream().anyMatch(e -> e.idUsuario().equals(nuevo.idUsuario())));
    }
}
