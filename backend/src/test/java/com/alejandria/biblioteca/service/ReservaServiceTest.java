package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.ReservaDao;
import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.entity.Libro;
import com.alejandria.biblioteca.entity.Reserva;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Eliminación física de reservas (RF17)")
@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock private ReservaDao reservaDao;
    @Mock private UsuarioDao usuarioDao;
    @Mock private LibroService libroService;
    @Mock private PrestatarioService prestatarioService;
    @Mock private ColaEsperaService colaEspera;

    private ReservaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ReservaService(reservaDao, usuarioDao, libroService, prestatarioService, colaEspera,
                HorarioAtencionTest.horario(LocalDateTime.of(2026, 10, 28, 10, 0)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"cancelada", "vencida"})
    void seBorraDeVerdadUnaReservaCanceladaOVencida(String estado) {
        Reserva r = reserva(estado);
        when(reservaDao.findById(7)).thenReturn(Optional.of(r));

        servicio.eliminar(7);

        verify(reservaDao).delete(r);              // DELETE en la BD, no un cambio de estado
        verify(reservaDao, never()).save(any());
        verifyNoInteractions(libroService);        // el ejemplar ya se había devuelto al stock al cancelar o vencer
    }

    @ParameterizedTest
    @ValueSource(strings = {"pendiente", "atendida"})
    void noSeBorraUnaReservaPendienteNiAtendida(String estado) {
        when(reservaDao.findById(7)).thenReturn(Optional.of(reserva(estado)));

        NegocioException error = assertThrows(NegocioException.class, () -> servicio.eliminar(7));
        assertEquals("Solo se pueden eliminar reservas canceladas o vencidas", error.getMessage());
        verify(reservaDao, never()).delete(any());
    }

    @Test
    void unaReservaQueNoExisteDa404() {
        when(reservaDao.findById(99)).thenReturn(Optional.empty());

        NegocioException error = assertThrows(NegocioException.class, () -> servicio.eliminar(99));
        assertEquals(404, error.getEstado().value());
    }

    private Reserva reserva(String estado) {
        Libro libro = new Libro();
        libro.setIdLibro(10);
        Reserva r = new Reserva();
        r.setIdReserva(7);
        r.setLibro(libro);
        r.setEstadoReserva(estado);
        return r;
    }
}
