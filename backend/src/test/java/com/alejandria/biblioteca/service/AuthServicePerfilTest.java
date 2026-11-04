package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.auth.LoginDTO;
import com.alejandria.biblioteca.entity.Usuario;
import com.alejandria.biblioteca.exception.NegocioException;
import com.alejandria.biblioteca.seguridad.LimitadorIntentosLogin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Perfil único al iniciar sesión")
@ExtendWith(MockitoExtension.class)
class AuthServicePerfilTest {

    private static final String CONTRASENA = "Biblioteca#2026";

    @Mock private UsuarioDao usuarioDao;
    @Mock private UsuarioService usuarioService;

    private AuthService servicio;
    private Usuario carlos;

    @BeforeEach
    void preparar() {
        servicio = new AuthService(usuarioDao, usuarioService, new SesionService(15), new LimitadorIntentosLogin());
        carlos = new Usuario();
        carlos.setIdUsuario(5);
        carlos.setCorreo("carlos@alejandria.edu.pe");
        carlos.setContrasena(new BCryptPasswordEncoder().encode(CONTRASENA));
        carlos.setEstado("activo");
        when(usuarioDao.findByCorreo("carlos@alejandria.edu.pe")).thenReturn(Optional.of(carlos));
    }

    @Test
    void conUnSoloPerfilEntraConSuRol() {
        when(usuarioDao.contarPerfiles(5)).thenReturn(1L);
        when(usuarioDao.esEncargado(5)).thenReturn(true);

        assertEquals("ENCARGADO", servicio.login(login()).usuario().rol());
    }

    @Test
    void enDosTablasDePerfilNoEntra() {
        when(usuarioDao.contarPerfiles(5)).thenReturn(2L); // por ejemplo, encargado y prestatario a la vez

        NegocioException error = assertThrows(NegocioException.class, () -> servicio.login(login()));
        assertEquals(403, error.getEstado().value());
    }

    @Test
    void sinNingunPerfilNoEntraComoPrestatario() {
        when(usuarioDao.contarPerfiles(5)).thenReturn(0L); // antes salia "prestatario" por defecto

        assertThrows(NegocioException.class, () -> servicio.login(login()));
    }

    @Test
    void unUsuarioDadoDeBajaNoPuedeIniciarSesion() {
        // RF13, criterio de aceptación: un usuario inactivo no puede iniciar sesión
        carlos.setEstado("inactivo"); // el admin lo dio de baja

        NegocioException error = assertThrows(NegocioException.class, () -> servicio.login(login()));
        assertEquals(403, error.getEstado().value());
        assertEquals("El usuario se encuentra inactivo", error.getMessage());
        verify(usuarioDao, never()).contarPerfiles(5);
    }

    private LoginDTO login() {
        return new LoginDTO("carlos@alejandria.edu.pe", CONTRASENA);
    }
}
