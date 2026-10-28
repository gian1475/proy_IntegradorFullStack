package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.usuario.EncargadoDTO;
import com.alejandria.biblioteca.dto.usuario.RegistroEncargadoDTO;
import com.alejandria.biblioteca.entity.Usuario;
import com.alejandria.biblioteca.exception.NegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Registro de encargados por el admin (RF09)")
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioDao usuarioDao;

    private UsuarioService servicio;

    @BeforeEach
    void preparar() {
        servicio = new UsuarioService(usuarioDao);
    }

    @Test
    void registraAlEncargadoConSuTurnoYLaContrasenaEncriptada() {
        when(usuarioDao.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setIdUsuario(20);
            return u;
        });

        EncargadoDTO encargado = servicio.registrarEncargado(formulario("Tarde"));

        assertEquals("tarde", encargado.turno());
        assertEquals("activo", encargado.estado());
        verify(usuarioDao).registrarComoEncargado(20, "tarde"); // perfil 2: tabla encargado
        verify(usuarioDao, never()).registrarComoPrestatario(any());

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioDao).save(guardado.capture());
        assertTrue(guardado.getValue().getContrasena().startsWith("$2a$")); // bcrypt, nunca texto plano
        assertEquals("maria.lopez@alejandria.edu.pe", guardado.getValue().getCorreo());
    }

    @Test
    void elTurnoSoloPuedeSerMananaOTarde() {
        NegocioException error = assertThrows(NegocioException.class, () -> servicio.registrarEncargado(formulario("noche")));
        assertEquals("El turno debe ser 'mañana' o 'tarde'", error.getMessage());
        verify(usuarioDao, never()).save(any());
    }

    @Test
    void noSeRepiteElCorreo() {
        when(usuarioDao.existsByCorreo("maria.lopez@alejandria.edu.pe")).thenReturn(true);

        NegocioException error = assertThrows(NegocioException.class, () -> servicio.registrarEncargado(formulario("mañana")));
        assertEquals(409, error.getEstado().value());
        verify(usuarioDao, never()).save(any());
    }

    @Test
    void laContrasenaDebeCumplirLaRegla() {
        RegistroEncargadoDTO dto = formulario("mañana");
        dto.setContrasena("123456");

        NegocioException error = assertThrows(NegocioException.class, () -> servicio.registrarEncargado(dto));
        assertEquals(400, error.getEstado().value());
        verify(usuarioDao, never()).save(any());
    }

    private RegistroEncargadoDTO formulario(String turno) {
        RegistroEncargadoDTO dto = new RegistroEncargadoDTO();
        dto.setNombre("María");
        dto.setApellido("López");
        dto.setDni("44444444");
        dto.setCorreo(" Maria.Lopez@alejandria.edu.pe ");
        dto.setContrasena("Biblioteca#2026");
        dto.setTurno(turno);
        return dto;
    }
}
