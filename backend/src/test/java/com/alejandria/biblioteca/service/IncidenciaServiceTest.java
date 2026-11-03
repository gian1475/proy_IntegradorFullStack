package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.IncidenciaDao;
import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dao.SancionDao;
import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.incidencia.IncidenciaDTO;
import com.alejandria.biblioteca.dto.incidencia.RegistroIncidenciaDTO;
import com.alejandria.biblioteca.dto.prestamo.DevolucionDTO;
import com.alejandria.biblioteca.entity.Incidencia;
import com.alejandria.biblioteca.entity.Libro;
import com.alejandria.biblioteca.entity.Prestamo;
import com.alejandria.biblioteca.entity.Sancion;
import com.alejandria.biblioteca.entity.Usuario;
import com.alejandria.biblioteca.exception.NegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/** RF06: el encargado registra una incidencia (retraso, daño o pérdida) con su sanción (advertencia, suspensión o multa). */
@DisplayName("Incidencias y sanciones (RF06)")
@ExtendWith(MockitoExtension.class)
class IncidenciaServiceTest {

    @Mock private PrestamoDao prestamoDao;
    @Mock private IncidenciaDao incidenciaDao;
    @Mock private SancionDao sancionDao;
    @Mock private UsuarioDao usuarioDao;
    @Mock private PrestamoService prestamoService;
    @Mock private PrestatarioService prestatarioService;

    /** Reloj fijo: martes 03/11/2026 a las 10:00. */
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 3, 10, 0);

    private IncidenciaService servicio;
    private Usuario valeria;

    @BeforeEach
    void preparar() {
        servicio = new IncidenciaService(prestamoDao, incidenciaDao, sancionDao, usuarioDao, prestamoService,
                prestatarioService, HorarioAtencionTest.horario(AHORA));
        valeria = new Usuario(5, "Valeria", "Flores", "73456783", "valeria@x.pe", "x", "activo");
    }

    // ---------- Las tres sanciones ----------

    @Test
    void laAdvertenciaEsInformativaQuedaCumplidaYNoSuspende() {
        prestamoConRetraso();

        IncidenciaDTO r = servicio.registrar(registro("retraso", "advertencia", null, null));

        assertEquals("cumplida", r.estadoSancion());
        assertEquals("resuelta", r.estadoIncidencia());
        assertEquals("activo", valeria.getEstado());
        verify(usuarioDao, never()).sumarSuspension(any());
    }

    @Test
    void laSuspensionDuraLosDiasElegidosHastaElCierreYSuspendeAlPrestatario() {
        prestamoConRetraso();

        IncidenciaDTO r = servicio.registrar(registro("retraso", "suspensión", 7, null));

        assertEquals("activa", r.estadoSancion());
        assertEquals("pendiente", r.estadoIncidencia());
        assertEquals(LocalDateTime.of(2026, 11, 10, 20, 0), r.finSancion()); // 7 dias, a las 20:00
        assertEquals("suspendido", valeria.getEstado());   // la suspension del usuario corresponde a su sancion
        verify(usuarioDao).sumarSuspension(5);             // prestatario.tiempos_suspendido + 1
    }

    @Test
    void unaSuspensionNoReactivaAUnPrestatarioDadoDeBaja() {
        // RF13: dado de baja (inactivo) por el admin; el daño se registra despues de la devolucion
        valeria.setEstado("inactivo");
        prestamoDevuelto("dañado");

        servicio.registrar(registro("daño", "suspensión", 3, null));

        assertEquals("inactivo", valeria.getEstado()); // sigue de baja: solo el admin lo reactiva
        verify(usuarioDao).sumarSuspension(5);         // la suspension igual se cuenta
    }

    @Test
    void laMultaQuedaPendienteConSuMonto() {
        prestamoDevuelto("dañado");

        IncidenciaDTO r = servicio.registrar(registro("daño", "multa", null, new BigDecimal("25.00")));

        assertEquals("activa", r.estadoSancion());
        assertEquals(new BigDecimal("25.00"), r.monto());
        assertNull(r.finSancion()); // termina cuando se registra el pago
        assertEquals("activo", valeria.getEstado()); // no suspende: la bloquea tieneSancionActiva
    }

    @Test
    void laSuspensionNecesitaLosDias() {
        prestamoConRetraso();
        NegocioException e = assertThrows(NegocioException.class,
                () -> servicio.registrar(registro("retraso", "suspensión", null, null)));
        assertEquals(400, e.getEstado().value());
        verify(sancionDao, never()).save(any());
    }

    @Test
    void laMultaNecesitaElMonto() {
        prestamoDevuelto("dañado");
        NegocioException e = assertThrows(NegocioException.class,
                () -> servicio.registrar(registro("daño", "multa", null, null)));
        assertEquals(400, e.getEstado().value());
    }

    // ---------- Qué incidencia corresponde a cada préstamo ----------

    @Test
    void noHayRetrasoSiSeDevolvioATiempo() {
        Prestamo p = prestamoDevuelto("bueno");
        p.setFechaDevolucionReal(p.getFechaDevolucionPrevista().minusMinutes(10));

        NegocioException e = assertThrows(NegocioException.class,
                () -> servicio.registrar(registro("retraso", "advertencia", null, null)));
        assertEquals(409, e.getEstado().value());
    }

    @Test
    void elDanioSeRegistraDespuesDeLaDevolucion() {
        prestamoEnCurso();
        NegocioException e = assertThrows(NegocioException.class,
                () -> servicio.registrar(registro("daño", "advertencia", null, null)));
        assertEquals(409, e.getEstado().value());
    }

    @Test
    void laPerdidaCierraElPrestamoComoDevolucionDaniada() {
        prestamoEnCurso();

        servicio.registrar(registro("pérdida", "multa", null, new BigDecimal("80.00")));

        // el ejemplar no vuelve al stock (si era el ultimo, el libro pasa a mantenimiento): lo hace devolver()
        verify(prestamoService).devolver(eq(115), argThat((DevolucionDTO d) -> "dañado".equals(d.estadoFisico())));
    }

    @Test
    void laPerdidaNoSeRegistraSobreUnPrestamoYaDevuelto() {
        prestamoDevuelto("bueno");
        assertThrows(NegocioException.class, () -> servicio.registrar(registro("pérdida", "advertencia", null, null)));
        verify(prestamoService, never()).devolver(any(), any());
    }

    @Test
    void noSeRegistraSobreUnPrestamoAnulado() {
        Prestamo p = prestamoEnCurso();
        p.setEstadoPrestamo("anulado");
        assertThrows(NegocioException.class, () -> servicio.registrar(registro("pérdida", "advertencia", null, null)));
    }

    @Test
    void noSeRepiteElMismoTipoDeIncidenciaEnUnPrestamo() {
        prestamoConRetraso();
        when(incidenciaDao.existsByPrestamoIdPrestamoAndTipoIncidencia(115, "retraso")).thenReturn(true);

        NegocioException e = assertThrows(NegocioException.class,
                () -> servicio.registrar(registro("retraso", "advertencia", null, null)));
        assertEquals(409, e.getEstado().value());
    }

    // ---------- Cumplir o cancelar la sanción ----------

    @Test
    void alPagarLaMultaSeCumpleYSeResuelveLaIncidencia() {
        Sancion multa = sancionGuardada("multa", "activa");

        IncidenciaDTO r = servicio.registrarPago(30);

        assertEquals("cumplida", r.estadoSancion());
        assertEquals(AHORA, multa.getFechaFin());
        assertEquals("resuelta", multa.getIncidencia().getEstadoIncidencia());
        verify(prestatarioService).restablecer(valeria); // vuelve a 'activo' si no le queda otra sancion
    }

    @Test
    void soloSePaganLasMultas() {
        sancionGuardada("suspensión", "activa");
        assertThrows(NegocioException.class, () -> servicio.registrarPago(30));
    }

    @Test
    void unaMultaYaPagadaNoSePagaOtraVez() {
        sancionGuardada("multa", "cumplida");
        assertThrows(NegocioException.class, () -> servicio.registrarPago(30));
    }

    @Test
    void alCancelarUnaSuspensionElPrestatarioRecuperaElServicio() {
        Sancion suspension = sancionGuardada("suspensión", "activa");
        suspension.setFechaFin(AHORA.plusDays(5));

        IncidenciaDTO r = servicio.cancelarSancion(30);

        assertEquals("cancelada", r.estadoSancion());
        assertEquals(AHORA, suspension.getFechaFin());
        verify(prestatarioService).restablecer(valeria);
    }

    @Test
    void noSeCancelaUnaSancionQueYaTermino() {
        sancionGuardada("suspensión", "cumplida");
        assertThrows(NegocioException.class, () -> servicio.cancelarSancion(30));
    }

    // ---------- Auxiliares ----------

    private RegistroIncidenciaDTO registro(String tipo, String sancion, Integer dias, BigDecimal monto) {
        return new RegistroIncidenciaDTO(115, tipo, "Detalle de prueba", sancion, dias, monto);
    }

    private Prestamo prestamo(String estado) {
        Prestamo p = new Prestamo();
        p.setIdPrestamo(115);
        p.setPrestatario(valeria);
        p.setLibro(new Libro(16, null, "Rayuela", "9789681902001", 1963, "Sudamericana", 0, "prestado"));
        p.setFechaPrestamo(AHORA.minusDays(1).withHour(15));
        p.setFechaDevolucionPrevista(AHORA.minusDays(1).withHour(18));
        p.setEstadoPrestamo(estado);
        when(prestamoDao.findById(115)).thenReturn(Optional.of(p));
        lenient().when(incidenciaDao.save(any(Incidencia.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(sancionDao.save(any(Sancion.class))).thenAnswer(inv -> inv.getArgument(0));
        return p;
    }

    /** Vencido: pasó su hora límite y el libro no volvió. */
    private Prestamo prestamoConRetraso() {
        return prestamo("vencido");
    }

    private Prestamo prestamoEnCurso() {
        Prestamo p = prestamo("activo");
        p.setFechaDevolucionPrevista(AHORA.plusHours(2));
        return p;
    }

    private Prestamo prestamoDevuelto(String estadoFisico) {
        Prestamo p = prestamo("devuelto");
        p.setEstadoFisicoDevolucion(estadoFisico);
        p.setFechaDevolucionReal(p.getFechaDevolucionPrevista().minusMinutes(5));
        return p;
    }

    private Sancion sancionGuardada(String tipo, String estado) {
        Prestamo p = new Prestamo();
        p.setIdPrestamo(115);
        p.setPrestatario(valeria);
        p.setLibro(new Libro(16, null, "Rayuela", "9789681902001", 1963, "Sudamericana", 0, "prestado"));

        Incidencia i = new Incidencia();
        i.setIdIncidencia(20);
        i.setPrestamo(p);
        i.setTipoIncidencia("retraso");
        i.setFechaIncidencia(AHORA.minusDays(1));
        i.setEstadoIncidencia("pendiente");

        Sancion s = new Sancion();
        s.setIdSancion(30);
        s.setIncidencia(i);
        s.setTipoSancion(tipo);
        s.setMonto("multa".equals(tipo) ? new BigDecimal("25.00") : null);
        s.setFechaInicio(AHORA.minusDays(1));
        s.setEstadoSancion(estado);
        when(sancionDao.findById(30)).thenReturn(Optional.of(s));
        lenient().when(sancionDao.save(any(Sancion.class))).thenAnswer(inv -> inv.getArgument(0));
        return s;
    }
}
