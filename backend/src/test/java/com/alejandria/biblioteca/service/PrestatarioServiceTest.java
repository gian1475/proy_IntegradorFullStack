package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dao.SancionDao;
import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.entity.Incidencia;
import com.alejandria.biblioteca.entity.Prestamo;
import com.alejandria.biblioteca.entity.Sancion;
import com.alejandria.biblioteca.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** RF06: "al cumplir la suspensión se restituye el servicio si no existe otra sanción bloqueante". */
@DisplayName("Fin de la suspensión: el prestatario recupera el servicio (RF06)")
@ExtendWith(MockitoExtension.class)
class PrestatarioServiceTest {

    @Mock private UsuarioDao usuarioDao;
    @Mock private PrestamoDao prestamoDao;
    @Mock private SancionDao sancionDao;

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 3, 10, 0);

    private PrestatarioService servicio;
    private Usuario valeria;

    @BeforeEach
    void preparar() {
        servicio = new PrestatarioService(usuarioDao, prestamoDao, sancionDao, HorarioAtencionTest.horario(AHORA));
        valeria = new Usuario(5, "Valeria", "Flores", "73456783", "valeria@x.pe", "x", "suspendido");
    }

    @Test
    void laSuspensionQueTerminoSeCumpleYElPrestatarioVuelveAActivo() {
        Sancion s = suspensionDe(valeria, AHORA.minusHours(14)); // termino ayer a las 20:00
        when(sancionDao.suspensionesTerminadas(AHORA)).thenReturn(List.of(s));
        when(sancionDao.tieneSuspensionActiva(5)).thenReturn(false);

        servicio.cerrarSuspensionesTerminadas();

        assertEquals("cumplida", s.getEstadoSancion());
        assertEquals("resuelta", s.getIncidencia().getEstadoIncidencia());
        assertEquals("activo", valeria.getEstado());
        verify(usuarioDao).save(valeria);
    }

    @Test
    void siTieneOtraSuspensionActivaSigueSuspendido() {
        when(sancionDao.tieneSuspensionActiva(5)).thenReturn(true);

        servicio.restablecer(valeria);

        assertEquals("suspendido", valeria.getEstado());
        verify(usuarioDao, never()).save(any());
    }

    @Test
    void antesDeValidarAlPrestatarioSeCierranLasSuspensionesTerminadas() {
        Sancion s = suspensionDe(valeria, AHORA.minusHours(14));
        when(sancionDao.suspensionesTerminadas(AHORA)).thenReturn(List.of(s));
        when(usuarioDao.esPrestatario(5)).thenReturn(true);

        assertDoesNotThrow(() -> servicio.validarHabilitado(valeria)); // ya puede pedir libros
        assertEquals("activo", valeria.getEstado());
    }

    private Sancion suspensionDe(Usuario prestatario, LocalDateTime fin) {
        Prestamo p = new Prestamo();
        p.setPrestatario(prestatario);
        Incidencia i = new Incidencia();
        i.setPrestamo(p);
        i.setEstadoIncidencia("pendiente");
        Sancion s = new Sancion();
        s.setIncidencia(i);
        s.setTipoSancion("suspensión");
        s.setFechaInicio(fin.minusDays(7));
        s.setFechaFin(fin);
        s.setEstadoSancion("activa");
        return s;
    }
}
