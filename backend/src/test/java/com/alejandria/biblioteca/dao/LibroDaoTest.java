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

    // D14: buscarPorAutor recibe un solo texto y lo busca en el nombre o en el apellido del autor
    @Test
    void buscaPorAutorConUnSoloTextoEnNombreOApellido() {
        assertTrue(titulos(libroDao.buscarPorAutor("martin")).contains("Clean Code")); // apellido de Robert Martin
        assertTrue(titulos(libroDao.buscarPorAutor("erich")).contains("Design Patterns")); // nombre de Erich Gamma
    }

    // D14: "john" coincide con el nombre de John Vlissides y con el apellido de Ralph Johnson,
    // los dos autores de Design Patterns: el libro tiene que salir una sola vez, y con sus autores
    // en la misma consulta (antes llegaban con otra consulta por libro: el N+1 de D15)
    @Test
    void buscarPorAutorNoRepiteLibrosYTraeLosAutoresEnLaMismaConsulta() {
        List<Libro> libros = libroDao.buscarPorAutor("john");
        assertEquals(1, libros.stream().filter(l -> "Design Patterns".equals(l.getTitulo())).count());
        assertTrue(libros.stream().allMatch(l -> Hibernate.isInitialized(l.getAutores())));
    }

    private List<String> titulos(List<Libro> libros) {
        return libros.stream().map(Libro::getTitulo).toList();
    }
}
