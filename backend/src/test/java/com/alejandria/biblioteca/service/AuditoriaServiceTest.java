package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.AuditoriaInventarioDao;
import com.alejandria.biblioteca.dao.LibroDao;
import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.auditoria.AuditoriaDTO;
import com.alejandria.biblioteca.dto.auditoria.AuditoriaMesDTO;
import com.alejandria.biblioteca.dto.auditoria.RegistroAuditoriaDTO;
import com.alejandria.biblioteca.entity.AuditoriaInventario;
import com.alejandria.biblioteca.entity.Libro;
import com.alejandria.biblioteca.entity.Usuario;
import com.alejandria.biblioteca.exception.NegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** IPI (1.2.2): el admin registra el conteo físico de un título y el sistema guarda su stock de ese momento. */
@DisplayName("Auditoría física del inventario (IPI)")
@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    /** Reloj fijo: martes 03/11/2026 a las 10:00. */
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 3, 10, 0);

    @Mock private AuditoriaInventarioDao auditoriaDao;
    @Mock private LibroDao libroDao;
    @Mock private UsuarioDao usuarioDao;

    private AuditoriaService servicio;
    private Libro rayuela;

    @BeforeEach
    void preparar() {
        servicio = new AuditoriaService(auditoriaDao, libroDao, usuarioDao, HorarioAtencionTest.horario(AHORA));
        rayuela = new Libro(16, null, "Rayuela", "9789681902001", 1963, "Sudamericana", 3, "disponible");
    }

    private void prepararRegistro() {
        when(libroDao.findById(16)).thenReturn(Optional.of(rayuela));
        when(usuarioDao.getReferenceById(1)).thenReturn(new Usuario(1, "Admin", "Principal", "11111111", "a@x.pe", "x", "activo"));
        when(auditoriaDao.save(any(AuditoriaInventario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void guardaElStockDelSistemaYElConteoFisico() {
        prepararRegistro();

        AuditoriaDTO r = servicio.registrar(new RegistroAuditoriaDTO(16, 3), 1);

        ArgumentCaptor<AuditoriaInventario> guardada = ArgumentCaptor.forClass(AuditoriaInventario.class);
        verify(auditoriaDao).save(guardada.capture());
        assertEquals(3, guardada.getValue().getStockSistema());   // lo que decía el sistema en ese momento
        assertEquals(3, guardada.getValue().getConteoFisico());
        assertEquals(AHORA, guardada.getValue().getFechaAuditoria());
        assertTrue(r.coincide());
        assertEquals("Rayuela", r.tituloLibro());
    }

    @Test
    void siNoCoincideSoloSeRegistraYElStockNoCambia() {
        prepararRegistro();

        AuditoriaDTO r = servicio.registrar(new RegistroAuditoriaDTO(16, 2), 1);

        assertFalse(r.coincide());
        assertEquals(3, rayuela.getStock());       // la auditoria mide; para corregir el stock se edita el libro
        verify(libroDao, never()).save(any());
    }

    @Test
    void noSeAuditaUnLibroDadoDeBaja() {
        rayuela.setEstadoLibro("baja");
        when(libroDao.findById(16)).thenReturn(Optional.of(rayuela));

        NegocioException e = assertThrows(NegocioException.class, () -> servicio.registrar(new RegistroAuditoriaDTO(16, 0), 1));
        assertEquals(409, e.getEstado().value());
        verify(auditoriaDao, never()).save(any());
    }

    @Test
    void unLibroQueNoExisteDa404() {
        when(libroDao.findById(99)).thenReturn(Optional.empty());

        NegocioException e = assertThrows(NegocioException.class, () -> servicio.registrar(new RegistroAuditoriaDTO(99, 1), 1));
        assertEquals(404, e.getEstado().value());
    }

    @Test
    void elResumenDelMesTraeElIpiYLasAuditorias() {
        when(auditoriaDao.contarAuditadosEntre(LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 12, 1, 0, 0)))
                .thenReturn(20L);
        when(auditoriaDao.contarCoincidentesEntre(any(), any())).thenReturn(19L);
        AuditoriaInventario a = new AuditoriaInventario();
        a.setIdAuditoria(1);
        a.setLibro(rayuela);
        a.setAdministrador(new Usuario(1, "Admin", "Principal", "11111111", "a@x.pe", "x", "activo"));
        a.setFechaAuditoria(AHORA);
        a.setStockSistema(3);
        a.setConteoFisico(3);
        when(auditoriaDao.listarEntre(any(), any())).thenReturn(List.of(a));

        AuditoriaMesDTO mes = servicio.resumenDelMes();

        assertEquals(95.0, mes.ipi());
        assertEquals(20, mes.auditados());
        assertEquals(19, mes.coinciden());
        assertEquals(1, mes.auditorias().size());
        assertEquals("Admin Principal", mes.auditorias().get(0).administrador());
    }
}
