package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.entity.Libro;
import com.alejandria.biblioteca.exception.NegocioException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B4 (RNF05): dos encargados prestan a la vez el ÚLTIMO ejemplar del mismo libro.
 * LibroDao.buscarParaActualizar bloquea la fila del libro (SELECT ... FOR UPDATE): el segundo espera a que el
 * primero termine, ve stock 0 y no presta. Sin el bloqueo los dos leerían stock 1 y los dos prestarían.
 * Esta prueba usa la BD real y datos confirmados (dos transacciones), por eso borra su libro al terminar.
 */
@DisplayName("Concurrencia: dos préstamos a la vez no descuadran el stock (RNF05)")
@SpringBootTest
class ConcurrenciaStockTest {

    private static final String ISBN = "999-CONCURRENCIA";

    @Autowired private LibroService libroService;
    @Autowired private TransactionTemplate transaccion;
    @Autowired private JdbcTemplate jdbc;

    private Integer idLibro;

    @BeforeEach
    void crearLibroConUnEjemplar() {
        jdbc.update("DELETE FROM libro WHERE isbn = ?", ISBN);
        idLibro = jdbc.queryForObject("INSERT INTO libro (id_categoria, titulo, isbn, anio_publicacion, editorial, stock, estado_libro) "
                + "VALUES ((SELECT MIN(id_categoria) FROM categoria), 'Prueba de concurrencia', ?, 2020, 'Prueba', 1, 'disponible') "
                + "RETURNING id_libro", Integer.class, ISBN);
    }

    @AfterEach
    void borrarElLibro() {
        jdbc.update("DELETE FROM libro WHERE isbn = ?", ISBN);
    }

    @Test
    void elUltimoEjemplarSoloSePrestaUnaVez() throws Exception {
        CountDownLatch primeroTieneElLibro = new CountDownLatch(1);
        AtomicInteger prestados = new AtomicInteger();
        AtomicInteger rechazados = new AtomicInteger();

        Callable<Void> primero = () -> {
            transaccion.executeWithoutResult(t -> {
                Libro libro = libroService.bloquearParaSalida(idLibro);
                primeroTieneElLibro.countDown();            // ya bloqueo la fila
                dormir(500);                                // el segundo intenta leer mientras tanto
                libroService.descontarStock(libro, "prestado");
            });
            prestados.incrementAndGet();
            return null;
        };
        Callable<Void> segundo = () -> {
            primeroTieneElLibro.await(5, TimeUnit.SECONDS);
            try {
                transaccion.executeWithoutResult(t -> {
                    Libro libro = libroService.bloquearParaSalida(idLibro); // espera al primero
                    libroService.descontarStock(libro, "prestado");
                });
                prestados.incrementAndGet();
            } catch (NegocioException e) {
                rechazados.incrementAndGet();
                assertEquals("No hay ejemplares disponibles de este libro", e.getMessage());
            }
            return null;
        };

        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            for (Future<Void> f : hilos.invokeAll(List.of(primero, segundo), 20, TimeUnit.SECONDS)) {
                f.get();
            }
        } finally {
            hilos.shutdownNow();
        }

        assertEquals(1, prestados.get(), "el último ejemplar se prestó dos veces");
        assertEquals(1, rechazados.get());
        assertEquals(0, jdbc.queryForObject("SELECT stock FROM libro WHERE id_libro = ?", Integer.class, idLibro));
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
