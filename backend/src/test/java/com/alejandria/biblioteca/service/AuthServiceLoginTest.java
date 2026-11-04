package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.auth.LoginDTO;
import com.alejandria.biblioteca.dto.auth.LoginRespuestaDTO;
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

/**
 * Prueba unitaria del inicio de sesión (lo pidió el profe en la semana 13): con la contraseña correcta entra,
 * con una incorrecta o un correo que no existe no entra, y al tercer intento fallido queda bloqueado.
 * La BD se reemplaza con Mockito: el usuario de prueba tiene su contraseña guardada como hash BCrypt.
 */
// proceso login: pruebas del login completo (credenciales, bcrypt y bloqueo con guava)
@DisplayName("Inicio de sesión: contraseña correcta, incorrecta y bloqueo (RF10)")
@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

    private static final String CORREO = "carlos@alejandria.edu.pe";
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
        carlos.setCorreo(CORREO);
        carlos.setContrasena(new BCryptPasswordEncoder().encode(CONTRASENA)); // en la BD solo está el hash
        carlos.setEstado("activo");
    }

    @Test
    void conLaContrasenaCorrectaEntraYRecibeUnToken() {
        when(usuarioDao.findByCorreo(CORREO)).thenReturn(Optional.of(carlos));
        when(usuarioDao.contarPerfiles(5)).thenReturn(1L);
        when(usuarioDao.esEncargado(5)).thenReturn(true);

        LoginRespuestaDTO respuesta = servicio.login(new LoginDTO(CORREO, CONTRASENA));

        assertFalse(respuesta.token().isBlank());
        assertEquals("ENCARGADO", respuesta.usuario().rol());
        assertNotEquals(CONTRASENA, carlos.getContrasena(), "la contraseña nunca se guarda en texto plano");
    }

    @Test
    void elCorreoSeAceptaConMayusculasYEspacios() {
        when(usuarioDao.findByCorreo(CORREO)).thenReturn(Optional.of(carlos));
        when(usuarioDao.contarPerfiles(5)).thenReturn(1L);
        when(usuarioDao.esEncargado(5)).thenReturn(true);

        assertDoesNotThrow(() -> servicio.login(new LoginDTO("  Carlos@Alejandria.edu.pe ", CONTRASENA)));
    }

    @Test
    void conLaContrasenaIncorrectaNoEntra() {
        when(usuarioDao.findByCorreo(CORREO)).thenReturn(Optional.of(carlos));

        NegocioException error = assertThrows(NegocioException.class,
                () -> servicio.login(new LoginDTO(CORREO, "Otra#Clave99")));

        assertEquals(401, error.getEstado().value());
        assertEquals("Credenciales incorrectas", error.getMessage());
    }

    @Test
    void unCorreoQueNoExisteRecibeElMismoMensaje() {
        // el mismo mensaje que con la contraseña mal: asi no se puede averiguar que correos estan registrados
        when(usuarioDao.findByCorreo("nadie@alejandria.edu.pe")).thenReturn(Optional.empty());

        NegocioException error = assertThrows(NegocioException.class,
                () -> servicio.login(new LoginDTO("nadie@alejandria.edu.pe", CONTRASENA)));

        assertEquals(401, error.getEstado().value());
        assertEquals("Credenciales incorrectas", error.getMessage());
    }

    @Test
    void alTercerIntentoFallidoQuedaBloqueadoAunqueDespuesPongaLaCorrecta() {
        when(usuarioDao.findByCorreo(CORREO)).thenReturn(Optional.of(carlos));
        for (int i = 0; i < LimitadorIntentosLogin.MAX_INTENTOS; i++) {
            assertThrows(NegocioException.class, () -> servicio.login(new LoginDTO(CORREO, "Otra#Clave99")));
        }

        NegocioException error = assertThrows(NegocioException.class,
                () -> servicio.login(new LoginDTO(CORREO, CONTRASENA)));

        assertEquals(429, error.getEstado().value());
        assertTrue(error.getMessage().startsWith("Demasiados intentos fallidos"));
        verify(usuarioDao, never()).contarPerfiles(anyInt()); // bloqueado: ni siquiera llega a revisar el perfil
    }
}
