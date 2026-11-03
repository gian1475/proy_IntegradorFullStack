package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dto.incidencia.IncidenciaDTO;
import com.alejandria.biblioteca.dto.incidencia.RegistroIncidenciaDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RF06 contra la BD real (Neon): la sanción queda en las tablas incidencia y sancion, y bloquea (o no) al prestatario.
 * Corre dentro de una transacción que se deshace al final: no deja datos guardados.
 */
@DisplayName("Incidencias y sanciones en la BD (RF06)")
@SpringBootTest
@Transactional
class IncidenciaBdTest {

    @Autowired private IncidenciaService incidenciaService;
    @Autowired private PrestamoDao prestamoDao;
    @PersistenceContext private EntityManager em;

    @Test
    void laSuspensionBloqueaAlPrestatarioYAlCancelarlaRecuperaElServicio() {
        Integer prestatario = prestatario("90000061");
        Integer prestamo = prestamoVencido(prestatario, libro("0000000000061", 1));

        IncidenciaDTO r = incidenciaService.registrar(
                new RegistroIncidenciaDTO(prestamo, "retraso", "Devuelto tarde", "suspensión", 3, null));
        em.flush();

        assertTrue(prestamoDao.tieneSancionActiva(prestatario, LocalDate.now()));
        assertEquals("suspendido", texto("SELECT estado FROM usuario WHERE id_usuario = " + prestatario));
        assertEquals(1, numero("SELECT tiempos_suspendido FROM prestatario WHERE id_usuario = " + prestatario));
        assertTrue(incidenciaService.listar().stream().anyMatch(i -> i.idIncidencia().equals(r.idIncidencia())));

        incidenciaService.cancelarSancion(r.idSancion());
        em.flush();

        assertFalse(prestamoDao.tieneSancionActiva(prestatario, LocalDate.now()));
        assertEquals("activo", texto("SELECT estado FROM usuario WHERE id_usuario = " + prestatario));
        assertEquals("resuelta", texto("SELECT estado_incidencia FROM incidencia WHERE id_incidencia = " + r.idIncidencia()));
    }

    @Test
    void laAdvertenciaNoBloqueaYLaMultaBloqueaHastaQueSePaga() {
        Integer prestatario = prestatario("90000062");
        Integer prestamo = prestamoVencido(prestatario, libro("0000000000062", 1));

        incidenciaService.registrar(new RegistroIncidenciaDTO(prestamo, "retraso", null, "advertencia", null, null));
        em.flush();
        assertFalse(prestamoDao.tieneSancionActiva(prestatario, LocalDate.now()), "la advertencia es informativa");

        IncidenciaDTO multa = incidenciaService.registrar(
                new RegistroIncidenciaDTO(prestamo, "pérdida", "No lo devolvió", "multa", null, new BigDecimal("80.00")));
        em.flush();
        assertTrue(prestamoDao.tieneSancionActiva(prestatario, LocalDate.now()), "la multa pendiente bloquea");

        incidenciaService.registrarPago(multa.idSancion());
        em.flush();
        assertFalse(prestamoDao.tieneSancionActiva(prestatario, LocalDate.now()));
    }

    @Test
    void laPerdidaCierraElPrestamoYElUltimoEjemplarPasaAMantenimiento() {
        Integer prestatario = prestatario("90000063");
        Integer idLibro = libro("0000000000063", 0); // su unico ejemplar esta prestado
        Integer prestamo = prestamoVencido(prestatario, idLibro);

        incidenciaService.registrar(new RegistroIncidenciaDTO(prestamo, "pérdida", null, "advertencia", null, null));
        em.flush();

        assertEquals("devuelto", texto("SELECT estado_prestamo FROM prestamo WHERE id_prestamo = " + prestamo));
        assertEquals("dañado", texto("SELECT estado_fisico_devolucion FROM prestamo WHERE id_prestamo = " + prestamo));
        assertEquals(0, numero("SELECT stock FROM libro WHERE id_libro = " + idLibro)); // el ejemplar no vuelve
        assertEquals("mantenimiento", texto("SELECT estado_libro FROM libro WHERE id_libro = " + idLibro));
    }

    // ---------- Auxiliares ----------

    private Integer prestatario(String dni) {
        Integer id = numero("INSERT INTO usuario (nombre, apellido, dni, correo, contrasena, estado) VALUES "
                + "('Prueba', 'Sancion', '" + dni + "', 'sancion" + dni + "@prueba.pe', 'x', 'activo') RETURNING id_usuario");
        em.createNativeQuery("INSERT INTO prestatario (id_usuario, tiempos_suspendido) VALUES (" + id + ", 0)").executeUpdate();
        return id;
    }

    private Integer libro(String isbn, int stock) {
        return numero("INSERT INTO libro (id_categoria, titulo, isbn, anio_publicacion, editorial, stock, estado_libro) "
                + "VALUES (4, 'Libro de prueba RF06', '" + isbn + "', 2020, 'Prueba', " + stock + ", "
                + (stock == 0 ? "'prestado'" : "'disponible'") + ") RETURNING id_libro");
    }

    /** Préstamo que pasó su hora límite y el libro no volvió. */
    private Integer prestamoVencido(Integer idPrestatario, Integer idLibro) {
        Integer encargado = numero("SELECT id_usuario FROM encargado ORDER BY id_usuario LIMIT 1");
        LocalDateTime inicio = LocalDateTime.now().minusHours(5);
        return ((Number) em.createNativeQuery("INSERT INTO prestamo (id_prestatario, id_encargado, id_libro, fecha_prestamo, "
                        + "fecha_devolucion_prevista, ampliado, estado_prestamo) "
                        + "VALUES (:pre, :enc, :libro, :inicio, :limite, false, 'vencido') RETURNING id_prestamo")
                .setParameter("pre", idPrestatario).setParameter("enc", encargado).setParameter("libro", idLibro)
                .setParameter("inicio", inicio).setParameter("limite", inicio.plusHours(3))
                .getSingleResult()).intValue();
    }

    private String texto(String sql) {
        return (String) em.createNativeQuery(sql).getSingleResult();
    }

    private Integer numero(String sql) {
        return ((Number) em.createNativeQuery(sql).getSingleResult()).intValue();
    }
}
