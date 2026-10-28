package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.ReservaDao;
import com.alejandria.biblioteca.entity.Reserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Cola de espera: el ejemplar que se libera es para el primero (RF04 y RF16)")
@ExtendWith(MockitoExtension.class)
class ColaEsperaServiceTest {

    @Mock private ReservaDao reservaDao;

    private ColaEsperaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ColaEsperaService(reservaDao, HorarioAtencionTest.horario(LocalDateTime.of(2026, 10, 28, 10, 0)));
    }

    @Test
    void elPrimeroRecibeElEjemplarYTieneHastaElCierreDeManana() {
        Reserva primero = enCola(1, 1);
        Reserva segundo = enCola(2, 2);
        Reserva tercero = enCola(3, 3);
        cola(primero, segundo, tercero);

        assertTrue(servicio.asignarAlSiguiente(10));

        assertEquals("pendiente", primero.getEstadoReserva());
        assertEquals(LocalDateTime.of(2026, 10, 29, 20, 0), primero.getFechaRecojoLimite());
        assertEquals(1, segundo.getPosicionCola()); // los demas avanzan un puesto
        assertEquals(2, tercero.getPosicionCola());
        assertEquals("en_espera", segundo.getEstadoReserva());
    }

    @Test
    void sinNadieEnLaColaElEjemplarVuelveAlStock() {
        cola();

        assertFalse(servicio.asignarAlSiguiente(10));
        verify(reservaDao, never()).save(any());
    }

    @Test
    void siAlguienDelMedioSaleLosDeAtrasAvanzan() {
        Reserva primero = enCola(1, 1);
        Reserva tercero = enCola(3, 3); // el 2 cancelo
        cola(primero, tercero);

        servicio.reordenar(10);

        assertEquals(1, primero.getPosicionCola());
        assertEquals(2, tercero.getPosicionCola());
        verify(reservaDao).save(tercero);
        verify(reservaDao, never()).save(primero); // ya estaba bien
    }

    @Test
    void elQueEntraSeVaAlFinal() {
        cola(enCola(1, 1), enCola(2, 2));

        assertEquals(3, servicio.siguientePosicion(10));
    }

    private void cola(Reserva... reservas) {
        when(reservaDao.findByLibroIdLibroAndEstadoReservaOrderByPosicionColaAscFechaReservaAsc(10, "en_espera"))
                .thenReturn(new ArrayList<>(List.of(reservas)));
    }

    private Reserva enCola(int id, int posicion) {
        Reserva r = new Reserva();
        r.setIdReserva(id);
        r.setEstadoReserva("en_espera");
        r.setPosicionCola(posicion);
        r.setFechaRecojoLimite(null);
        return r;
    }
}
