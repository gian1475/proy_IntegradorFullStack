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
    @Mock private ColaEsperaService colaEspera;

    private LibroService servicio;

    @BeforeEach
    void preparar() {
        servicio = new LibroService(libroDao, categoriaDao, autorDao, prestamoDao, reservaDao, colaEspera);
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

    @Test
    void noSeDaDeBajaUnLibroConGenteEnLaColaDeEspera() {
        when(libroDao.findById(10)).thenReturn(Optional.of(libro("prestado")));
        when(reservaDao.existsByLibroIdLibroAndEstadoReserva(10, "pendiente")).thenReturn(false);
        when(reservaDao.existsByLibroIdLibroAndEstadoReserva(10, "en_espera")).thenReturn(true);

        NegocioException error = assertThrows(NegocioException.class, () -> servicio.darDeBaja(10));
        assertEquals("El libro tiene prestatarios en la cola de espera", error.getMessage());
        verify(libroDao, never()).save(any());
    }

    // ---------- Cola de espera (RF04) ----------

    @Test
    void siAlguienEstaEnLaColaElEjemplarDevueltoEsParaElYElStockNoSube() {
        Libro libro = libro("prestado");
        libro.setStock(0);
        when(libroDao.buscarParaActualizar(10)).thenReturn(Optional.of(libro));
        when(colaEspera.asignarAlSiguiente(10)).thenReturn(true);

        servicio.reponerStock(10);

        assertEquals(0, libro.getStock());
        assertEquals("reservado", libro.getEstadoLibro()); // no hay ejemplares libres para otros
    }

    @Test
    void siElAdminAgregaEjemplaresPrimeroSeRepartenALaCola() {
        Libro libro = libro("prestado");
        libro.setStock(0);
        when(libroDao.findById(10)).thenReturn(Optional.of(libro));
        when(categoriaDao.findById(4)).thenReturn(Optional.of(new Categoria(4, "Tecnología", null)));
        when(libroDao.save(any(Libro.class))).thenAnswer(inv -> inv.getArgument(0));
        when(colaEspera.asignarAlSiguiente(10)).thenReturn(true, false); // hay 1 persona en la cola

        LibroFormDTO form = formulario(3); // el admin agrega 3 ejemplares

        var resultado = servicio.actualizar(10, form);
        assertEquals(2, resultado.stock());               // 1 se lo lleva la cola
        assertEquals("disponible", resultado.estadoLibro());
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

        LibroFormDTO form = formulario(1); // el ejemplar ya se reparó

        assertEquals("disponible", servicio.actualizar(10, form).estadoLibro());
    }

    private LibroFormDTO formulario(int stock) {
        LibroFormDTO form = new LibroFormDTO();
        form.setTitulo("Clean Code");
        form.setIsbn("9780132350884");
        form.setEditorial("Prentice Hall");
        form.setAnioPublicacion(2008);
        form.setStock(stock);
        form.setIdCategoria(4);
        return form;
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
