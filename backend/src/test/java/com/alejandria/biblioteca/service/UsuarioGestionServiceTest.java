package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dao.ReservaDao;
import com.alejandria.biblioteca.dao.SancionDao;
import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.usuario.ActualizarUsuarioDTO;
import com.alejandria.biblioteca.dto.usuario.UsuarioAdminDTO;
import com.alejandria.biblioteca.entity.Reserva;
import com.alejandria.biblioteca.entity.Usuario;
import com.alejandria.biblioteca.exception.NegocioException;
import com.alejandria.biblioteca.seguridad.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RF12 (editar los datos de un usuario) y RF13 (darlo de baja: eliminación lógica, estado 'inactivo').
 * Solo prestatarios y encargados: al administrador no se le edita ni se le da de baja desde aquí.
 */
@DisplayName("Gestión de usuarios por el admin (RF12 y RF13)")
@ExtendWith(MockitoExtension.class)
class UsuarioGestionServiceTest {

    @Mock private UsuarioDao usuarioDao;
    @Mock private PrestamoDao prestamoDao;
    @Mock private ReservaDao reservaDao;
    @Mock private ReservaService reservaService;
    @Mock private SancionDao sancionDao;
    @Mock private SesionService sesionService;

    private UsuarioService servicio;

    @BeforeEach
    void preparar() {
        servicio = new UsuarioService(usuarioDao, prestamoDao, reservaDao, reservaService, sancionDao, sesionService);
    }

    private Usuario usuario(int id, String estado) {
        return new Usuario(id, "Ana", "Torres", "71234567", "ana@alejandria.edu.pe", "$2a$hash", estado);
    }

    private Usuario prestatario(int id, String estado) {
        Usuario u = usuario(id, estado);
        when(usuarioDao.findById(id)).thenReturn(Optional.of(u));
        when(usuarioDao.esPrestatario(id)).thenReturn(true);
        return u;
    }

    private Usuario encargado(int id, String estado) {
        Usuario u = usuario(id, estado);
        when(usuarioDao.findById(id)).thenReturn(Optional.of(u));
        when(usuarioDao.esEncargado(id)).thenReturn(true);
        return u;
    }

    private ActualizarUsuarioDTO datos(String turno) {
        return new ActualizarUsuarioDTO(" Ana María ", "Torres", "71234568", " Ana.Torres@Alejandria.edu.pe ", turno);
    }

    @Nested
    @DisplayName("RF12: editar datos")
    class Editar {

        @Test
        void cambiaLosDatosDelPrestatarioLimpiandoEspaciosYMayusculas() {
            Usuario u = prestatario(5, "activo");
            when(usuarioDao.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            UsuarioAdminDTO r = servicio.actualizar(5, datos(null));

            assertEquals("Ana María", u.getNombre());
            assertEquals("71234568", u.getDni());
            assertEquals("ana.torres@alejandria.edu.pe", u.getCorreo());
            assertEquals("$2a$hash", u.getContrasena()); // la contraseña no se toca
            assertEquals("activo", u.getEstado());       // el estado tampoco
            assertEquals(Rol.PRESTATARIO, r.perfil());
            verify(usuarioDao, never()).cambiarTurno(any(), any());
        }

        @Test
        void alEncargadoTambienLeCambiaElTurno() {
            encargado(7, "activo");
            when(usuarioDao.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            UsuarioAdminDTO r = servicio.actualizar(7, datos("Tarde"));

            verify(usuarioDao).cambiarTurno(7, "tarde");
            assertEquals("tarde", r.turno());
            assertEquals(Rol.ENCARGADO, r.perfil());
        }

        @Test
        void elTurnoDelEncargadoSoloPuedeSerMananaOTarde() {
            encargado(7, "activo");

            NegocioException e = assertThrows(NegocioException.class, () -> servicio.actualizar(7, datos("noche")));
            assertEquals(400, e.getEstado().value());
            verify(usuarioDao, never()).save(any());
        }

        @Test
        void elDniNoPuedeSerElDeOtroUsuario() {
            prestatario(5, "activo");
            when(usuarioDao.existsByDniAndIdUsuarioNot("71234568", 5)).thenReturn(true);

            NegocioException e = assertThrows(NegocioException.class, () -> servicio.actualizar(5, datos(null)));
            assertEquals(409, e.getEstado().value());
            assertEquals("El DNI ya se encuentra registrado", e.getMessage());
            verify(usuarioDao, never()).save(any());
        }

        @Test
        void elCorreoNoPuedeSerElDeOtroUsuario() {
            prestatario(5, "activo");
            when(usuarioDao.existsByCorreoAndIdUsuarioNot("ana.torres@alejandria.edu.pe", 5)).thenReturn(true);

            NegocioException e = assertThrows(NegocioException.class, () -> servicio.actualizar(5, datos(null)));
            assertEquals(409, e.getEstado().value());
            assertEquals("El correo ya se encuentra registrado", e.getMessage());
        }

        @Test
        void alAdministradorNoSeLeEditaDesdeAqui() {
            when(usuarioDao.findById(1)).thenReturn(Optional.of(usuario(1, "activo")));

            NegocioException e = assertThrows(NegocioException.class, () -> servicio.actualizar(1, datos(null)));
            assertEquals(403, e.getEstado().value());
            verify(usuarioDao, never()).save(any());
        }

        @Test
        void unUsuarioQueNoExisteDa404() {
            when(usuarioDao.findById(99)).thenReturn(Optional.empty());

            NegocioException e = assertThrows(NegocioException.class, () -> servicio.actualizar(99, datos(null)));
            assertEquals(404, e.getEstado().value());
        }
    }

    @Nested
    @DisplayName("RF13: dar de baja (eliminación lógica)")
    class DarDeBaja {

        @Test
        void elPrestatarioQuedaInactivoYSeLeCierraLaSesion() {
            Usuario u = prestatario(5, "activo");
            when(usuarioDao.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            UsuarioAdminDTO r = servicio.darDeBaja(5);

            assertEquals("inactivo", u.getEstado());
            assertEquals("inactivo", r.estado());
            verify(sesionService).cerrarSesionesDe(5);
            verify(usuarioDao, never()).delete(any()); // la fila sigue en la bd
            verify(usuarioDao, never()).deleteById(any());
        }

        @Test
        void noSeDaDeBajaSiTieneUnLibroPrestado() {
            Usuario u = prestatario(5, "activo");
            when(prestamoDao.existsByPrestatarioIdUsuarioAndEstadoPrestamoIn(5, List.of("activo", "vencido"))).thenReturn(true);

            NegocioException e = assertThrows(NegocioException.class, () -> servicio.darDeBaja(5));
            assertEquals(409, e.getEstado().value());
            assertEquals("activo", u.getEstado());
            verify(usuarioDao, never()).save(any());
            verify(reservaService, never()).cancelar(any());
        }

        @Test
        void susReservasPendientesYEnColaSeCancelan() {
            prestatario(5, "activo");
            Reserva pendiente = new Reserva();
            pendiente.setIdReserva(30);
            Reserva enCola = new Reserva();
            enCola.setIdReserva(31);
            when(reservaDao.findByPrestatarioIdUsuarioAndEstadoReservaIn(5, List.of("pendiente", "en_espera")))
                    .thenReturn(List.of(pendiente, enCola));
            when(usuarioDao.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            servicio.darDeBaja(5);

            // cancelar devuelve el ejemplar al stock o pasa el turno al siguiente de la cola
            verify(reservaService).cancelar(30);
            verify(reservaService).cancelar(31);
        }

        @Test
        void unPrestatarioSuspendidoTambienSePuedeDarDeBaja() {
            Usuario u = prestatario(5, "suspendido");
            when(usuarioDao.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            servicio.darDeBaja(5);

            assertEquals("inactivo", u.getEstado());
        }

        @Test
        void elEncargadoQuedaInactivoYSusPrestamosSiguenEnElHistorial() {
            Usuario u = encargado(7, "activo");
            when(usuarioDao.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            servicio.darDeBaja(7);

            assertEquals("inactivo", u.getEstado());
            verify(sesionService).cerrarSesionesDe(7);
            verifyNoInteractions(prestamoDao, reservaService); // no presta para sí: no hay nada que revisar
        }

        @Test
        void unUsuarioYaInactivoDa409() {
            prestatario(5, "inactivo");

            NegocioException e = assertThrows(NegocioException.class, () -> servicio.darDeBaja(5));
            assertEquals(409, e.getEstado().value());
            assertEquals("El usuario ya está inactivo", e.getMessage());
        }

        @Test
        void alAdministradorNoSeLeDaDeBaja() {
            when(usuarioDao.findById(1)).thenReturn(Optional.of(usuario(1, "activo")));

            NegocioException e = assertThrows(NegocioException.class, () -> servicio.darDeBaja(1));
            assertEquals(403, e.getEstado().value());
            verify(sesionService, never()).cerrarSesionesDe(any());
        }
    }

    @Nested
    @DisplayName("Reactivar a un usuario dado de baja")
    class Reactivar {

        @Test
        void vuelveAActivo() {
            Usuario u = prestatario(5, "inactivo");
            when(usuarioDao.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            servicio.reactivar(5);

            assertEquals("activo", u.getEstado());
        }

        @Test
        void siTodaviaTieneUnaSuspensionVuelveASuspendido() {
            Usuario u = prestatario(5, "inactivo");
            when(sancionDao.tieneSuspensionActiva(5)).thenReturn(true);
            when(usuarioDao.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            servicio.reactivar(5);

            assertEquals("suspendido", u.getEstado());
        }

        @Test
        void soloSeReactivaAUnUsuarioInactivo() {
            prestatario(5, "activo");

            NegocioException e = assertThrows(NegocioException.class, () -> servicio.reactivar(5));
            assertEquals(409, e.getEstado().value());
            assertEquals("El usuario no está inactivo", e.getMessage());
        }
    }

    @Test
    void listaPrestatariosYEncargadosConSuPerfil() {
        when(usuarioDao.listarUsuarios()).thenReturn(List.of(
                new Object[] {7, "María", "López", "44444444", "maria@alejandria.edu.pe", "activo", "ENCARGADO", "tarde"},
                new Object[] {5, "Ana", "Torres", "71234567", "ana@alejandria.edu.pe", "inactivo", "PRESTATARIO", null}));

        List<UsuarioAdminDTO> lista = servicio.listarUsuarios();

        assertEquals(2, lista.size());
        assertEquals(new UsuarioAdminDTO(7, "María", "López", "44444444", "maria@alejandria.edu.pe", "activo", Rol.ENCARGADO, "tarde"), lista.get(0));
        assertNull(lista.get(1).turno());
    }
}
