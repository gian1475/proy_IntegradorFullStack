package com.backend.demo.service;

import com.backend.demo.dao.LibroDao;
import com.backend.demo.dao.PrestamoDao;
import com.backend.demo.dao.UsuarioDao;
import com.backend.demo.dto.DevolucionDTO;
import com.backend.demo.dto.PrestamoDTO;
import com.backend.demo.entity.Libro;
import com.backend.demo.entity.Prestamo;
import com.backend.demo.entity.Usuario;
import com.backend.demo.exception.NegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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

    private PrestamoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new PrestamoService(prestamoDao, libroDao, usuarioDao, libroService, prestatarioService, reservaService);
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
    void ampliarSumaTresHorasYMarcaAmpliado() {
        Prestamo p = prestamoActivo(LocalDateTime.now().plusHours(1));
        LocalDateTime limiteOriginal = p.getFechaDevolucionPrevista();
        when(prestamoDao.findById(1)).thenReturn(Optional.of(p));
        when(prestamoDao.save(any(Prestamo.class))).thenAnswer(inv -> inv.getArgument(0));

        PrestamoDTO resultado = servicio.ampliar(1);

        assertTrue(resultado.getAmpliado());
        assertEquals(limiteOriginal.plusHours(PrestamoService.HORAS_AMPLIACION), resultado.getFechaDevolucionPrevista());
    }

    @Test
    void noSePuedeAmpliarDosVeces() {
        Prestamo p = prestamoActivo(LocalDateTime.now().plusHours(1));
        p.setAmpliado(true);
        when(prestamoDao.findById(1)).thenReturn(Optional.of(p));

        NegocioException error = assertThrows(NegocioException.class, () -> servicio.ampliar(1));
        assertEquals("El préstamo ya fue ampliado una vez", error.getMessage());
        verify(prestamoDao, never()).save(any());
    }

    @Test
    void noSePuedeAmpliarUnPrestamoVencido() {
        Prestamo p = prestamoActivo(LocalDateTime.now().minusMinutes(5)); // la hora límite ya pasó
        when(prestamoDao.findById(1)).thenReturn(Optional.of(p));

        assertThrows(NegocioException.class, () -> servicio.ampliar(1));
        verify(prestamoDao, never()).save(any());
    }

    // ---------- Devolución ----------

    @Test
    void devolverRegistraEstadoFisicoYReponeStock() {
        Prestamo p = prestamoActivo(LocalDateTime.now().plusHours(1));
        when(prestamoDao.findById(1)).thenReturn(Optional.of(p));
        when(prestamoDao.save(any(Prestamo.class))).thenAnswer(inv -> inv.getArgument(0));

        PrestamoDTO resultado = servicio.devolver(1, devolucion("dañado"));

        assertEquals("devuelto", resultado.getEstadoPrestamo());
        assertEquals("dañado", resultado.getEstadoFisicoDevolucion());
        assertNotNull(resultado.getFechaDevolucionReal());
        verify(libroService).reponerStock(10);
    }

    @Test
    void devolverRechazaEstadoFisicoInvalido() {
        assertThrows(NegocioException.class, () -> servicio.devolver(1, devolucion("regular")));
        verifyNoInteractions(libroService);
    }

    @Test
    void noSePuedeDevolverDosVeces() {
        Prestamo p = prestamoActivo(LocalDateTime.now().plusHours(1));
        p.setEstadoPrestamo("devuelto");
        when(prestamoDao.findById(1)).thenReturn(Optional.of(p));

        assertThrows(NegocioException.class, () -> servicio.devolver(1, devolucion("bueno")));
        verifyNoInteractions(libroService);
    }

    // ---------- Eliminación lógica ----------

    @Test
    void anularNoBorraLaFilaYReponeStock() {
        Prestamo p = prestamoActivo(LocalDateTime.now().plusHours(1));
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

    private DevolucionDTO devolucion(String estadoFisico) {
        DevolucionDTO dto = new DevolucionDTO();
        dto.setEstadoFisico(estadoFisico);
        return dto;
    }
}
