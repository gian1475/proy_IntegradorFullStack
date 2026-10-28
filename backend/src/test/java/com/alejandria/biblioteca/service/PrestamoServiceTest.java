package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.LibroDao;
import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.prestamo.DevolucionDTO;
import com.alejandria.biblioteca.dto.prestamo.PrestamoDTO;
import com.alejandria.biblioteca.dto.prestamo.RegistroPrestamoDTO;
import com.alejandria.biblioteca.entity.Libro;
import com.alejandria.biblioteca.entity.Prestamo;
import com.alejandria.biblioteca.entity.Usuario;
import com.alejandria.biblioteca.exception.NegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Reglas de préstamos en sala")
@ExtendWith(MockitoExtension.class)
class PrestamoServiceTest {

    @Mock private PrestamoDao prestamoDao;
    @Mock private LibroDao libroDao;
    @Mock private UsuarioDao usuarioDao;
    @Mock private LibroService libroService;
    @Mock private PrestatarioService prestatarioService;
    @Mock private ReservaService reservaService;

    /** Reloj fijo a las 10:00: las pruebas dan lo mismo aunque se corran de noche. */
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 28, 10, 0);

    private PrestamoService servicio;

    @BeforeEach
    void preparar() {
        servicio = nuevoServicio(AHORA);
    }

    private PrestamoService nuevoServicio(LocalDateTime ahora) {
        return new PrestamoService(prestamoDao, libroDao, usuarioDao, libroService, prestatarioService, reservaService,
                HorarioAtencionTest.horario(ahora));
    }

    // ---------- Cálculo de las 3 horas ----------

    @Test
    void laHoraLimiteEsTresHorasDespues() {
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 28, 9, 30);
        assertEquals(LocalDateTime.of(2026, 10, 28, 12, 30), PrestamoService.calcularHoraLimite(inicio));
    }

    @Test
    void laHoraLimitePuedePasarAlDiaSiguiente() {
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 28, 22, 0);
        assertEquals(LocalDateTime.of(2026, 10, 29, 1, 0), PrestamoService.calcularHoraLimite(inicio));
    }

    // ---------- Ampliación (solo una vez) ----------

    @Test
    void ampliarSumaUnaHoraYMarcaAmpliado() {
        Prestamo p = prestamoActivo(AHORA.plusHours(1));
        LocalDateTime limiteOriginal = p.getFechaDevolucionPrevista();
        when(prestamoDao.findById(1)).thenReturn(Optional.of(p));
        when(prestamoDao.save(any(Prestamo.class))).thenAnswer(inv -> inv.getArgument(0));

        PrestamoDTO resultado = servicio.ampliar(1);

        assertTrue(resultado.getAmpliado());
        assertEquals(limiteOriginal.plusHours(1), resultado.getFechaDevolucionPrevista()); // RF05: hasta 1 hora
    }

    @Test
    void noSePuedeAmpliarSiElLibroTieneReservasPendientes() {
        Prestamo p = prestamoActivo(AHORA.plusHours(1));
        when(prestamoDao.findById(1)).thenReturn(Optional.of(p));
        when(reservaService.hayReservasPendientes(10)).thenReturn(true);

        NegocioException error = assertThrows(NegocioException.class, () -> servicio.ampliar(1));
        assertEquals("No se puede ampliar: el libro tiene reservas pendientes", error.getMessage());
        verify(prestamoDao, never()).save(any());
    }

    @Test
    void noSePuedeAmpliarDosVeces() {
        Prestamo p = prestamoActivo(AHORA.plusHours(1));
        p.setAmpliado(true);
        when(prestamoDao.findById(1)).thenReturn(Optional.of(p));

        NegocioException error = assertThrows(NegocioException.class, () -> servicio.ampliar(1));
        assertEquals("El préstamo ya fue ampliado una vez", error.getMessage());
        verify(prestamoDao, never()).save(any());
    }

    @Test
    void noSePuedeAmpliarUnPrestamoVencido() {
        Prestamo p = prestamoActivo(AHORA.minusMinutes(5)); // la hora límite ya pasó
        when(prestamoDao.findById(1)).thenReturn(Optional.of(p));

        assertThrows(NegocioException.class, () -> servicio.ampliar(1));
        verify(prestamoDao, never()).save(any());
    }

    // ---------- Horario de atención: 8:00 a 20:00 (RF01 y RF05) ----------

    @Test
    void unPrestamoALas10TerminaALas13() {
        prepararRegistro();

        PrestamoDTO resultado = servicio.registrar(registro(), 2);

        assertEquals(AHORA.withHour(13), resultado.getFechaDevolucionPrevista());
    }

    @Test
    void unPrestamoALas1830TerminaALas20YNoALas2130() {
        servicio = nuevoServicio(AHORA.withHour(18).withMinute(30));
        prepararRegistro();

        PrestamoDTO resultado = servicio.registrar(registro(), 2);

        assertEquals(AHORA.withHour(20), resultado.getFechaDevolucionPrevista()); // no pasa del cierre
    }

    @Test
    void fueraDelHorarioNoSePresta() {
        servicio = nuevoServicio(AHORA.withHour(21));

        NegocioException error = assertThrows(NegocioException.class, () -> servicio.registrar(registro(), 2));
        assertEquals(409, error.getEstado().value());
        verifyNoInteractions(libroService);
        verify(prestamoDao, never()).save(any());
    }

    @Test
    void laAmpliacionNoPasaDelCierre() {
        servicio = nuevoServicio(AHORA.withHour(18));
        Prestamo p = prestamoActivo(AHORA.withHour(19).withMinute(30));
        when(prestamoDao.findById(1)).thenReturn(Optional.of(p));
        when(prestamoDao.save(any(Prestamo.class))).thenAnswer(inv -> inv.getArgument(0));

        PrestamoDTO resultado = servicio.ampliar(1);

        assertEquals(AHORA.withHour(20), resultado.getFechaDevolucionPrevista()); // media hora, no una
        assertTrue(resultado.getAmpliado());
    }

    @Test
    void noSeAmpliaSiYaTerminaALaHoraDeCierre() {
        servicio = nuevoServicio(AHORA.withHour(18));
        when(prestamoDao.findById(1)).thenReturn(Optional.of(prestamoActivo(AHORA.withHour(20))));

        NegocioException error = assertThrows(NegocioException.class, () -> servicio.ampliar(1));
        assertEquals("No se puede ampliar: el préstamo ya termina a la hora de cierre (20:00)", error.getMessage());
        verify(prestamoDao, never()).save(any());
    }

    // ---------- Historial con filtros (RF07) ----------

    @Test
    void elHistorialUsaLosFiltrosYLosDiasCompletos() {
        servicio.historial(" 33333333 ", "", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 27));

        // el filtro vacio no se aplica (null) y "hasta el 27" incluye todo ese dia
        verify(prestamoDao).buscarHistorial("33333333", null,
                LocalDate.of(2026, 10, 1).atStartOfDay(), LocalDate.of(2026, 10, 28).atStartOfDay());
    }

    @Test
    void elHistorialRechazaFechasAlReves() {
        NegocioException error = assertThrows(NegocioException.class,
                () -> servicio.historial(null, null, LocalDate.of(2026, 10, 27), LocalDate.of(2026, 10, 1)));
        assertEquals(400, error.getEstado().value());
        verify(prestamoDao, never()).buscarHistorial(any(), any(), any(), any());
    }

    // ---------- Consulta de una tabla (Mis préstamos) ----------

    @Test
    void elHistorialDelPrestatarioPrimeroMarcaLosVencidos() {
        when(prestamoDao.buscarPorPrestatario(3)).thenReturn(List.of(prestamoActivo(AHORA.plusHours(1))));

        List<PrestamoDTO> resultado = servicio.listarPorPrestatario(3);

        // primero se actualizan los vencidos, para que el prestatario no vea "activo" un préstamo que ya pasó la hora
        InOrder orden = inOrder(prestamoDao);
        orden.verify(prestamoDao).marcarVencidos(any(LocalDateTime.class));
        orden.verify(prestamoDao).buscarPorPrestatario(3);
        assertEquals(1, resultado.size());
    }

    // ---------- Devolución ----------

    @Test
    void devolverEnBuenEstadoReponeElStock() {
        Prestamo p = prestamoActivo(AHORA.plusHours(1));
        when(prestamoDao.findById(1)).thenReturn(Optional.of(p));
        when(prestamoDao.save(any(Prestamo.class))).thenAnswer(inv -> inv.getArgument(0));

        PrestamoDTO resultado = servicio.devolver(1, devolucion("bueno"));

        assertEquals("devuelto", resultado.getEstadoPrestamo());
        assertEquals("bueno", resultado.getEstadoFisicoDevolucion());
        assertNotNull(resultado.getFechaDevolucionReal());
        verify(libroService).reponerStock(10);
        verify(libroService, never()).registrarEjemplarDanado(any());
    }

    @Test
    void devolverDanadoNoReponeElStockYMandaElEjemplarAMantenimiento() { // RF02
        Prestamo p = prestamoActivo(AHORA.plusHours(1));
        when(prestamoDao.findById(1)).thenReturn(Optional.of(p));
        when(prestamoDao.save(any(Prestamo.class))).thenAnswer(inv -> inv.getArgument(0));

        PrestamoDTO resultado = servicio.devolver(1, devolucion("dañado"));

        assertEquals("devuelto", resultado.getEstadoPrestamo());
        assertEquals("dañado", resultado.getEstadoFisicoDevolucion());
        verify(libroService).registrarEjemplarDanado(10);
        verify(libroService, never()).reponerStock(any());
    }

    @Test
    void devolverRechazaEstadoFisicoInvalido() {
        assertThrows(NegocioException.class, () -> servicio.devolver(1, devolucion("regular")));
        verifyNoInteractions(libroService);
    }

    @Test
    void noSePuedeDevolverDosVeces() {
        Prestamo p = prestamoActivo(AHORA.plusHours(1));
        p.setEstadoPrestamo("devuelto");
        when(prestamoDao.findById(1)).thenReturn(Optional.of(p));

        assertThrows(NegocioException.class, () -> servicio.devolver(1, devolucion("bueno")));
        verifyNoInteractions(libroService);
    }

    // ---------- Eliminación lógica ----------

    @Test
    void anularNoBorraLaFilaYReponeStock() {
        Prestamo p = prestamoActivo(AHORA.plusHours(1));
        when(prestamoDao.findById(1)).thenReturn(Optional.of(p));

        servicio.anular(1);

        assertEquals("anulado", p.getEstadoPrestamo());
        verify(prestamoDao, never()).delete(any());
        verify(libroService).reponerStock(10);
    }

    // ---------- Auxiliares ----------

    private Prestamo prestamoActivo(LocalDateTime horaLimite) {
        Usuario prestatario = new Usuario();
        prestatario.setIdUsuario(3);
        Usuario encargado = new Usuario();
        encargado.setIdUsuario(2);
        Libro libro = new Libro();
        libro.setIdLibro(10);

        Prestamo p = new Prestamo();
        p.setIdPrestamo(1);
        p.setPrestatario(prestatario);
        p.setEncargado(encargado);
        p.setLibro(libro);
        p.setFechaPrestamo(horaLimite.minusHours(3));
        p.setFechaDevolucionPrevista(horaLimite);
        p.setAmpliado(false);
        p.setEstadoPrestamo("activo");
        return p;
    }

    /** Lo que necesita registrar(): prestatario habilitado, libro con stock y sin reserva previa. */
    private void prepararRegistro() {
        Usuario prestatario = new Usuario();
        prestatario.setIdUsuario(3);
        prestatario.setDni("33333333");
        Libro libro = new Libro();
        libro.setIdLibro(10);
        libro.setTitulo("Clean Code");
        libro.setStock(2);
        when(usuarioDao.findByDni("33333333")).thenReturn(Optional.of(prestatario));
        when(libroService.bloquearParaSalida(10)).thenReturn(libro);
        when(usuarioDao.getReferenceById(2)).thenReturn(new Usuario());
        when(prestamoDao.save(any(Prestamo.class))).thenAnswer(inv -> {
            Prestamo guardado = inv.getArgument(0);
            guardado.getEncargado().setIdUsuario(2);
            return guardado;
        });
    }

    private RegistroPrestamoDTO registro() {
        RegistroPrestamoDTO dto = new RegistroPrestamoDTO();
        dto.setDniPrestatario("33333333");
        dto.setIdLibro(10);
        return dto;
    }

    private DevolucionDTO devolucion(String estadoFisico) {
        DevolucionDTO dto = new DevolucionDTO();
        dto.setEstadoFisico(estadoFisico);
        return dto;
    }
}
