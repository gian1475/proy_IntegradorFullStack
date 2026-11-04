package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.ReservaDao;
import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.reserva.ActualizarReservaDTO;
import com.alejandria.biblioteca.dto.reserva.ReservaRespuestaDTO;
import com.alejandria.biblioteca.entity.Libro;
import com.alejandria.biblioteca.entity.Reserva;
import com.alejandria.biblioteca.entity.Usuario;
import com.alejandria.biblioteca.exception.NegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** RF15: el prestatario o el admin cambian la fecha y la franja de recojo de una reserva pendiente antes de que venza. */
@DisplayName("Actualización de reservas (RF15)")
@ExtendWith(MockitoExtension.class)
class ReservaActualizacionTest {

    /** Reloj fijo: miércoles 28/10/2026 a las 10:00. */
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

    @Test
    void cambiaLaFechaYLaFranjaDeUnaReservaPendiente() {
        Reserva r = reserva("pendiente", AHORA.plusDays(1).withHour(11).withMinute(30));
        when(reservaDao.findById(7)).thenReturn(Optional.of(r));
        when(reservaDao.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDateTime nueva = LocalDateTime.of(2026, 10, 30, 17, 30); // viernes, turno tarde

        ReservaRespuestaDTO respuesta = servicio.actualizar(7, new ActualizarReservaDTO(nueva));

        assertEquals(nueva, r.getFechaRecojoLimite());
        assertEquals(nueva, respuesta.fechaRecojoLimite());
        assertEquals("pendiente", r.getEstadoReserva());
        verifyNoInteractions(libroService); // el ejemplar sigue apartado: el stock no cambia
    }

    @ParameterizedTest
    @ValueSource(strings = {"en_espera", "atendida", "cancelada", "vencida"})
    void soloSeModificaUnaReservaPendiente(String estado) {
        when(reservaDao.findById(7)).thenReturn(Optional.of(reserva(estado, null)));

        NegocioException e = assertThrows(NegocioException.class,
                () -> servicio.actualizar(7, new ActualizarReservaDTO(AHORA.plusDays(1).withHour(17).withMinute(30))));
        assertEquals(409, e.getEstado().value());
        assertEquals("Solo se pueden modificar reservas en estado pendiente", e.getMessage());
        verify(reservaDao, never()).save(any());
    }

    @Test
    void unaReservaQueYaVencioNoSeModifica() {
        Reserva r = reserva("pendiente", AHORA.minusHours(1)); // tenia que recogerla a las 9:00
        when(reservaDao.findByEstadoReservaAndFechaRecojoLimiteBefore("pendiente", AHORA)).thenReturn(List.of(r));
        when(reservaDao.findById(7)).thenReturn(Optional.of(r));

        NegocioException e = assertThrows(NegocioException.class,
                () -> servicio.actualizar(7, new ActualizarReservaDTO(AHORA.plusDays(1).withHour(17).withMinute(30))));

        assertEquals(409, e.getEstado().value());
        assertEquals("vencida", r.getEstadoReserva());     // primero se libera como vencida
        verify(libroService).reponerStock(10);              // y su ejemplar vuelve al stock
    }

    @Test
    void laNuevaFechaSigueLasReglasDeLaReserva() {
        Reserva r = reserva("pendiente", AHORA.plusDays(1).withHour(11).withMinute(30));
        when(reservaDao.findById(7)).thenReturn(Optional.of(r));

        NegocioException masDe7Dias = assertThrows(NegocioException.class,
                () -> servicio.actualizar(7, new ActualizarReservaDTO(AHORA.plusDays(8))));
        assertEquals("El recojo debe ser en el futuro y dentro de los próximos 7 días", masDe7Dias.getMessage());

        NegocioException deNoche = assertThrows(NegocioException.class,
                () -> servicio.actualizar(7, new ActualizarReservaDTO(AHORA.plusDays(1).withHour(21))));
        assertTrue(deNoche.getMessage().startsWith("La hora de recojo debe estar dentro del horario de atención"));

        assertEquals(AHORA.plusDays(1).withHour(11).withMinute(30), r.getFechaRecojoLimite()); // no cambió
        verify(reservaDao, never()).save(any());
    }

    @Test
    void unaReservaQueNoExisteDa404() {
        when(reservaDao.findById(99)).thenReturn(Optional.empty());

        NegocioException e = assertThrows(NegocioException.class,
                () -> servicio.actualizar(99, new ActualizarReservaDTO(AHORA.plusDays(1))));
        assertEquals(404, e.getEstado().value());
    }

    private Reserva reserva(String estado, LocalDateTime limite) {
        Libro libro = new Libro();
        libro.setIdLibro(10);
        libro.setTitulo("Rayuela");
        Usuario lector = new Usuario(5, "Valeria", "Flores", "73456783", "valeria@x.pe", "x", "activo");
        Reserva r = new Reserva(lector, libro, AHORA.minusDays(1), limite, estado);
        r.setIdReserva(7);
        r.setPosicionCola(1);
        return r;
    }
}
