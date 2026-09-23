package com.backend.demo;

import com.backend.demo.dao.LibroDao;
import com.backend.demo.entity.Libro;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
class DaoQueryTest {

    @Autowired
    private LibroDao libroDao;

    @Test
    void testLibroDaoQueries() {
        List<Libro> todos = libroDao.findAll();
        System.out.println(">>> LIBROS ENCONTRADOS VIA DAO EN NEON: " + todos.size());
        for (Libro l : todos) {
            System.out.println(" - [" + l.getIdLibro() + "] " + l.getTitulo() + " (" + l.getEditorial() + ", " + l.getAnioPublicacion() + ")");
        }

        // Probar búsqueda por autor Mario Vargas Llosa
        List<Libro> vargasLlosa = libroDao.findByAutorNombreOrApellido("Mario", "Vargas Llosa");
        System.out.println(">>> LIBROS DE MARIO VARGAS LLOSA ENCONTRADOS: " + vargasLlosa.size());
        for (Libro l : vargasLlosa) {
            System.out.println(" - " + l.getTitulo());
        }

        assertFalse(todos.isEmpty());
    }
}
