package com.alejandria.biblioteca.dao;

import com.alejandria.biblioteca.entity.Libro;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Consulta de disponibilidad (RF03) contra la BD real (Neon), con los libros de la carga inicial.
 * Solo lee: no cambia nada.
 */
@DisplayName("Consulta de disponibilidad: título, autor, categoría o código (RF03)")
@SpringBootTest
@Transactional(readOnly = true)
class LibroDaoTest {

    @Autowired private LibroDao libroDao;

    @Test
    void buscaPorTitulo() {
        assertTrue(titulos(libroDao.buscarEnCatalogo("clean")).contains("Clean Code"));
    }

    @Test
    void buscaPorCodigoIsbnConOSinGuiones() {
        assertTrue(titulos(libroDao.buscarEnCatalogo("9780132350884")).contains("Clean Code"));
        assertTrue(titulos(libroDao.buscarEnCatalogo("978-0132350884")).contains("Clean Code"));
    }

    @Test
    void buscaPorAutor() {
        assertTrue(titulos(libroDao.buscarEnCatalogo("robert martin")).contains("Clean Code"));
        assertTrue(titulos(libroDao.buscarEnCatalogo("vargas llosa")).contains("La Ciudad y los Perros"));
    }

    @Test
    void buscaPorCategoria() {
        List<Libro> tecnologia = libroDao.buscarEnCatalogo("tecnología");
        assertFalse(tecnologia.isEmpty());
        assertTrue(tecnologia.stream().allMatch(l -> "Tecnología".equals(l.getCategoria().getNombre())
                || l.getTitulo().toLowerCase().contains("tecnología")));
    }

    @Test
    void noTraeLibrosDadosDeBajaNiRepetidos() {
        List<Libro> todos = libroDao.buscarEnCatalogo("");
        assertTrue(todos.stream().noneMatch(l -> "baja".equals(l.getEstadoLibro())));
        assertEquals(todos.size(), todos.stream().map(Libro::getIdLibro).distinct().count()); // un libro con 2 autores sale 1 vez
    }

    // D15: antes cada libro pedia sus autores con otra consulta (N+1) y la lista tardaba unos 4 s contra Neon
    @Test
    void elCatalogoTraeLosAutoresEnLaMismaConsulta() {
        List<Libro> libros = libroDao.findByEstadoLibroNotOrderByTitulo("baja");
        assertFalse(libros.isEmpty());
        assertTrue(libros.stream().allMatch(l -> Hibernate.isInitialized(l.getAutores())),
                "los autores tienen que venir en la misma consulta, no uno por uno");
        // con el JOIN, un libro con varios autores (Design Patterns tiene 4) igual sale una sola vez
        assertEquals(libros.size(), libros.stream().map(Libro::getIdLibro).distinct().count());
    }

    @Test
    void laBusquedaTambienTraeLosAutoresEnLaMismaConsulta() {
        List<Libro> libros = libroDao.buscarEnCatalogo("");
        assertFalse(libros.isEmpty());
        assertTrue(libros.stream().allMatch(l -> Hibernate.isInitialized(l.getAutores())));
    }

    private List<String> titulos(List<Libro> libros) {
        return libros.stream().map(Libro::getTitulo).toList();
    }
}
