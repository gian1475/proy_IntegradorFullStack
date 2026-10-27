package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.ReservaDao;
import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.reserva.ReservaDTO;
import com.alejandria.biblioteca.dto.reserva.ReservaRespuestaDTO;
import com.alejandria.biblioteca.entity.Libro;
import com.alejandria.biblioteca.entity.Reserva;
import com.alejandria.biblioteca.entity.Usuario;
import com.alejandria.biblioteca.exception.NegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Reservas con cola de espera (RF04 y RF16)")
@ExtendWith(MockitoExtension.class)
class ReservaColaEsperaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 28, 10, 0);

    @Mock private ReservaDao reservaDao;
    @Mock private UsuarioDao usuarioDao;
    @Mock private LibroService libroService;
    @Mock private PrestatarioService prestatarioService;
    @Mock private ColaEsperaService colaEspera;

    private ReservaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ReservaService(reservaDao, usuarioDao, libroService, prestatarioService, colaEspera,
                HorarioAtencionTest.horario(AHORA));
    }

    // ---------- Crear ----------

    @Test
    void conStockLaReservaApartaUnEjemplar() {
        Libro libro = prepararCrear(2);

        ReservaRespuestaDTO r = servicio.crear(reserva(AHORA.withHour(14).withMinute(30)));

        assertEquals("pendiente", r.estadoReserva());
        verify(libroService).descontarStock(libro, "reservado");
    }

    @Test
    void sinStockLaReservaEntraALaColaConSuPuesto() {
        prepararCrear(0);
        when(colaEspera.siguientePosicion(10)).thenReturn(3); // ya hay 2 esperando

        ReservaRespuestaDTO r = servicio.crear(reserva(null)); // en la cola no se elige fecha

        assertEquals("en_espera", r.estadoReserva());
        assertEquals(3, r.posicionCola());
        assertNull(r.fechaRecojoLimite());
        verify(libroService, never()).descontarStock(any(), anyString()); // no aparta ningun ejemplar
    }

    @Test
    void noSeReservaDosVecesElMismoLibro() {
        when(usuarioDao.findById(3)).thenReturn(Optional.of(prestatario()));
        when(reservaDao.existsByPrestatarioIdUsuarioAndLibroIdLibroAndEstadoReservaIn(eq(3), eq(10), anyList()))
                .thenReturn(true);

        NegocioException error = assertThrows(NegocioException.class, () -> servicio.crear(reserva(null)));
        assertEquals("Ya tiene una reserva pendiente o está en la cola de este libro", error.getMessage());
        verify(reservaDao, never()).save(any());
    }

    @Test
    void conStockLaFechaDeRecojoEsObligatoria() {
        prepararCrear(2);

        NegocioException error = assertThrows(NegocioException.class, () -> servicio.crear(reserva(null)));
        assertEquals("Elija la fecha y la franja de recojo", error.getMessage());
    }

    @Test
    void elRecojoTieneQueSerEnHorarioDeAtencion() { // horario de atencion: 8:00 a 20:00
        prepararCrear(2);

        NegocioException error = assertThrows(NegocioException.class,
                () -> servicio.crear(reserva(AHORA.withHour(21))));
        assertEquals("La hora de recojo debe estar dentro del horario de atención (08:00 a 20:00)", error.getMessage());
        verify(libroService, never()).descontarStock(any(), anyString());
    }

    // ---------- Cancelar ----------

    @Test
    void salirDeLaColaNoTocaElStockYLosDeAtrasAvanzan() { // RF16
        Reserva r = reservaGuardada("en_espera");
        when(reservaDao.findById(7)).thenReturn(Optional.of(r));

        servicio.cancelar(7);

        assertEquals("cancelada", r.getEstadoReserva());
        verify(colaEspera).reordenar(10);
        verify(libroService, never()).reponerStock(any());
    }

    @Test
    void cancelarUnaPendienteDevuelveElEjemplar() {
        Reserva r = reservaGuardada("pendiente");
        when(reservaDao.findById(7)).thenReturn(Optional.of(r));

        servicio.cancelar(7);

        assertEquals("cancelada", r.getEstadoReserva());
        verify(libroService).reponerStock(10); // si hay cola, LibroService se lo pasa al primero
    }

    @Test
    void conGenteEnLaColaNoSeAmpliaElPrestamo() { // RF05
        when(reservaDao.existsByLibroIdLibroAndEstadoReservaIn(eq(10), anyList())).thenReturn(true);

        assertTrue(servicio.hayReservasPendientes(10));
        verify(reservaDao).existsByLibroIdLibroAndEstadoReservaIn(10, java.util.List.of("pendiente", "en_espera"));
    }

    // ---------- Auxiliares ----------

    private Libro prepararCrear(int stock) {
        Libro libro = libro();
        libro.setStock(stock);
        when(usuarioDao.findById(3)).thenReturn(Optional.of(prestatario()));
        when(libroService.bloquearParaSalida(10)).thenReturn(libro);
        lenient().when(reservaDao.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva guardada = inv.getArgument(0);
            guardada.setIdReserva(7);
            return guardada;
        });
        return libro;
    }

    private ReservaDTO reserva(LocalDateTime fechaRecojo) {
        ReservaDTO dto = new ReservaDTO();
        dto.setIdLibro(10);
        dto.setIdUsuario(3);
        dto.setFechaRecojoLimite(fechaRecojo);
        return dto;
    }

    private Reserva reservaGuardada(String estado) {
        Reserva r = new Reserva();
        r.setIdReserva(7);
        r.setLibro(libro());
        r.setPrestatario(prestatario());
        r.setEstadoReserva(estado);
        return r;
    }

    private Usuario prestatario() {
        Usuario u = new Usuario();
        u.setIdUsuario(3);
        u.setNombre("Juan");
        u.setApellido("Estudiante");
        return u;
    }

    private Libro libro() {
        Libro libro = new Libro();
        libro.setIdLibro(10);
        libro.setTitulo("Clean Code");
        libro.setIsbn("9780132350884");
        libro.setAutores(new HashSet<>());
        return libro;
    }
}
