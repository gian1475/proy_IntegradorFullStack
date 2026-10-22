package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.AutorDao;
import com.alejandria.biblioteca.dao.CategoriaDao;
import com.alejandria.biblioteca.dao.LibroDao;
import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dao.ReservaDao;
import com.alejandria.biblioteca.dto.libro.LibroFormDTO;
import com.alejandria.biblioteca.entity.Categoria;
import com.alejandria.biblioteca.entity.Libro;
import com.alejandria.biblioteca.exception.NegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Baja lógica de libros (RF14)")
@ExtendWith(MockitoExtension.class)
class LibroServiceTest {

    @Mock private LibroDao libroDao;
    @Mock private CategoriaDao categoriaDao;
    @Mock private AutorDao autorDao;
    @Mock private PrestamoDao prestamoDao;
    @Mock private ReservaDao reservaDao;

    private LibroService servicio;

    @BeforeEach
    void preparar() {
        servicio = new LibroService(libroDao, categoriaDao, autorDao, prestamoDao, reservaDao);
    }

    @Test
    void darDeBajaCambiaElEstadoYNoBorraLaFila() {
        Libro libro = libro("disponible");
        when(libroDao.findById(10)).thenReturn(Optional.of(libro));

        servicio.darDeBaja(10);

        assertEquals("baja", libro.getEstadoLibro());
        verify(libroDao).save(libro);
        verify(libroDao, never()).delete(any());
    }

    @Test
    void noSeDaDeBajaUnLibroConPrestamosSinDevolver() {
        when(libroDao.findById(10)).thenReturn(Optional.of(libro("prestado")));
        when(prestamoDao.existsByLibroIdLibroAndEstadoPrestamoIn(eq(10), anyList())).thenReturn(true);

        assertThrows(NegocioException.class, () -> servicio.darDeBaja(10));
        verify(libroDao, never()).save(any());
    }

    @Test
    void noSeDaDeBajaUnLibroConReservasPendientes() {
        when(libroDao.findById(10)).thenReturn(Optional.of(libro("reservado")));
        when(reservaDao.existsByLibroIdLibroAndEstadoReserva(10, "pendiente")).thenReturn(true);

        NegocioException error = assertThrows(NegocioException.class, () -> servicio.darDeBaja(10));
        assertEquals("El libro tiene reservas pendientes", error.getMessage());
        verify(libroDao, never()).save(any());
    }

    // ---------- Ejemplar dañado y mantenimiento (RF02) ----------

    @Test
    void siVuelveDanadoElUltimoEjemplarElLibroQuedaEnMantenimiento() {
        Libro libro = libro("prestado");
        libro.setStock(0);
        when(libroDao.buscarParaActualizar(10)).thenReturn(Optional.of(libro));

        servicio.registrarEjemplarDanado(10);

        assertEquals(0, libro.getStock());              // el ejemplar dañado no vuelve al stock
        assertEquals("mantenimiento", libro.getEstadoLibro());
        verify(libroDao).save(libro);
    }

    @Test
    void siQuedanOtrosEjemplaresElLibroSigueDisponible() {
        Libro libro = libro("disponible");
        libro.setStock(2);
        when(libroDao.buscarParaActualizar(10)).thenReturn(Optional.of(libro));

        servicio.registrarEjemplarDanado(10);

        assertEquals(2, libro.getStock());
        assertEquals("disponible", libro.getEstadoLibro());
    }

    @Test
    void alDevolverseOtroEjemplarElLibroSaleDeMantenimiento() {
        Libro libro = libro("mantenimiento");
        libro.setStock(0);
        when(libroDao.buscarParaActualizar(10)).thenReturn(Optional.of(libro));

        servicio.reponerStock(10);

        assertEquals(1, libro.getStock());
        assertEquals("disponible", libro.getEstadoLibro());
    }

    @Test
    void cuandoElAdminReponeElStockElLibroSaleDeMantenimiento() {
        Libro libro = libro("mantenimiento");
        libro.setStock(0);
        when(libroDao.findById(10)).thenReturn(Optional.of(libro));
        when(categoriaDao.findById(4)).thenReturn(Optional.of(new Categoria(4, "Tecnología", null)));
        when(libroDao.save(any(Libro.class))).thenAnswer(inv -> inv.getArgument(0));

        LibroFormDTO form = new LibroFormDTO();
        form.setTitulo("Clean Code");
        form.setIsbn("9780132350884");
        form.setEditorial("Prentice Hall");
        form.setAnioPublicacion(2008);
        form.setStock(1); // el ejemplar ya se reparó
        form.setIdCategoria(4);

        assertEquals("disponible", servicio.actualizar(10, form).estadoLibro());
    }

    private Libro libro(String estado) {
        Libro libro = new Libro();
        libro.setIdLibro(10);
        libro.setTitulo("Clean Code");
        libro.setStock(1);
        libro.setEstadoLibro(estado);
        return libro;
    }
}
