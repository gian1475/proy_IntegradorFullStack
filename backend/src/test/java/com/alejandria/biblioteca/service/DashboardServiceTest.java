package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.AuditoriaInventarioDao;
import com.alejandria.biblioteca.dao.LibroDao;
import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dao.ReservaDao;
import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.reporte.DashboardDTO;
import com.alejandria.biblioteca.dto.reporte.IndicadorDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Dashboard del administrador")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

    @Mock private PrestamoDao prestamoDao;
    @Mock private LibroDao libroDao;
    @Mock private ReservaDao reservaDao;
    @Mock private UsuarioDao usuarioDao;
    @Mock private AuditoriaInventarioDao auditoriaDao;

    private DashboardService servicio;

    @BeforeEach
    void preparar() {
        servicio = new DashboardService(prestamoDao, libroDao, reservaDao, usuarioDao, auditoriaDao);
        when(libroDao.contarPorEstado()).thenReturn(List.of(
                new Object[] {"disponible", 20L, 45L},
                new Object[] {"prestado", 3L, 0L},
                new Object[] {"mantenimiento", 1L, 0L}));
        when(prestamoDao.prestamosPorCategoriaEntre(any(), any())).thenReturn(List.<Object[]>of(new Object[] {"Tecnología", 7L}));
        when(prestamoDao.prestamosPorDiaDesde(any())).thenReturn(List.of());
    }

    @Test
    void calculaLosIndicadoresDelMesComoElInforme() {
        when(prestamoDao.contarPrestamosEntre(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(400L);
        when(prestamoDao.contarDevueltosConRetrasoEntre(any(), any())).thenReturn(20L);       // 5 %
        when(prestamoDao.contarPrestamosConIncidenciaEntre(any(), any())).thenReturn(4L);     // 1 %

        DashboardDTO d = servicio.obtener();

        IndicadorDTO pdf = indicador(d, "PDF");
        assertEquals(5.0, pdf.valor());
        assertTrue(pdf.cumpleMeta());
        IndicadorDTO tim = indicador(d, "TIM");
        assertEquals(1.0, tim.valor());
        assertTrue(tim.cumpleMeta());
    }

    @Test
    void sinHoraDeInicioNiAuditoriasTapEIpiNoInventanUnValor() {
        DashboardDTO d = servicio.obtener();

        assertNull(indicador(d, "TAP").valor()); // ningun prestamo del mes con la hora de inicio de la atencion
        assertNull(indicador(d, "IPI").valor()); // ninguna auditoria fisica en el mes
    }

    @Test
    void calculaElTapYElIpiDelMes() {
        when(prestamoDao.promedioMinutosAtencionEntre(any(), any())).thenReturn(1.5);
        when(prestamoDao.contarConInicioAtencionEntre(any(), any())).thenReturn(8L);
        when(auditoriaDao.contarAuditadosEntre(any(), any())).thenReturn(20L);
        when(auditoriaDao.contarCoincidentesEntre(any(), any())).thenReturn(19L);

        DashboardDTO d = servicio.obtener();

        assertEquals(1.5, indicador(d, "TAP").valor());
        assertTrue(indicador(d, "TAP").cumpleMeta());   // meta: 2 min o menos
        assertEquals(95.0, indicador(d, "IPI").valor());
        assertTrue(indicador(d, "IPI").cumpleMeta());   // meta: 95 % o mas
    }

    @Test
    void cuentaLosLibrosYEjemplaresSinLosDadosDeBaja() {
        DashboardDTO d = servicio.obtener();

        assertEquals(24, d.titulos());               // 20 + 3 + 1
        assertEquals(45, d.ejemplaresDisponibles());
    }

    @Test
    void antesDeContarActualizaLosPrestamosVencidos() {
        servicio.obtener();
        verify(prestamoDao).marcarVencidos(any(LocalDateTime.class));
    }

    @Test
    void muestraLosUltimosSieteDiasAunqueNoHayaPrestamos() {
        assertEquals(7, servicio.obtener().prestamosUltimos7Dias().size());
    }

    private IndicadorDTO indicador(DashboardDTO d, String codigo) {
        return d.indicadores().stream().filter(i -> i.codigo().equals(codigo)).findFirst().orElseThrow();
    }
}
