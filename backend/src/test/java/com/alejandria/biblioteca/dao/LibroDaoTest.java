package com.alejandria.biblioteca.dao;

import com.alejandria.biblioteca.entity.Autor;
import com.alejandria.biblioteca.entity.Libro;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

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
    void consultaDeDosTablasLibroYCategoria() {
        List<Object[]> todas = libroDao.consultarConCategoria("");
        assertFalse(todas.isEmpty());
        List<Object[]> tecnologia = libroDao.consultarConCategoria("Tecnología");
        assertTrue(tecnologia.stream().allMatch(f -> "Tecnología".equals(f[5])));
        assertTrue(todas.stream().noneMatch(f -> "baja".equals(f[4])));
    }

    // D24: estas tres pruebas buscaban "Clean Code", pero ese libro se dio de baja en Neon (en una demo) y
    // fallaban. Ahora eligen de la BD un libro que siga en el catalogo: no dependen de un titulo fijo

    @Test
    void buscaPorTitulo() {
        Libro libro = unLibroDelCatalogo();
        // solo una parte del titulo y en minusculas: "La Ciudad y los Perros" se encuentra con "la ciu"
        String parte = libro.getTitulo().toLowerCase().substring(0, Math.min(6, libro.getTitulo().length()));
        assertTrue(titulos(libroDao.buscarEnCatalogo(parte)).contains(libro.getTitulo()));
    }

    @Test
    void buscaPorCodigoIsbnConOSinGuiones() {
        Libro libro = unLibroDelCatalogo();
        String isbn = libro.getIsbn().replace("-", "");
        String conGuion = isbn.substring(0, 3) + "-" + isbn.substring(3); // 9788432221026 -> 978-8432221026
        assertTrue(titulos(libroDao.buscarEnCatalogo(isbn)).contains(libro.getTitulo()));
        assertTrue(titulos(libroDao.buscarEnCatalogo(conGuion)).contains(libro.getTitulo()));
    }

    @Test
    void buscaPorAutor() {
        Libro libro = unLibroDelCatalogo();
        Autor autor = libro.getAutores().iterator().next();
        String nombreYApellido = (autor.getNombre() + " " + autor.getApellido()).toLowerCase(); // ej. "mario vargas llosa"
        assertTrue(titulos(libroDao.buscarEnCatalogo(nombreYApellido)).contains(libro.getTitulo()));
    }

    // eliminacion logica: el libro dado de baja sigue en la bd, pero la busqueda del catalogo no lo devuelve
    // (es lo que paso con "Clean Code")
    @Test
    void unLibroDadoDeBajaNoSaleAunqueSeBusqueSuIsbn() {
        List<Libro> deBaja = libroDao.findByEstadoLibroOrderByTitulo("baja");
        assumeFalse(deBaja.isEmpty(), "La BD no tiene libros dados de baja");
        Libro libro = deBaja.get(0);
        assertTrue(libroDao.buscarEnCatalogo(libro.getIsbn()).stream()
                .noneMatch(l -> l.getIdLibro().equals(libro.getIdLibro())));
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

    /** El primer libro del catálogo (no dado de baja) que tenga autor e ISBN. Se elige en cada corrida. */
    private Libro unLibroDelCatalogo() {
        return libroDao.findByEstadoLibroNotOrderByTitulo("baja").stream()
                .filter(l -> !l.getAutores().isEmpty() && l.getIsbn() != null && l.getIsbn().length() > 3)
                .findFirst()
                .orElseThrow(() -> new AssertionError("La BD no tiene libros con autor en el catálogo"));
    }
}
