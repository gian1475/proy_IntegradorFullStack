package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.AuditoriaInventarioDao;
import com.alejandria.biblioteca.dao.PrestamoDao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TAP e IPI (migración 05) contra la BD real. Usa fechas de enero de 2020, donde no hay otros datos,
 * y corre dentro de una transacción que se deshace al final: no deja datos guardados.
 */
@DisplayName("TAP e IPI en la BD (migración 05)")
@SpringBootTest
@Transactional
class AuditoriaBdTest {

    private static final LocalDateTime DESDE = LocalDateTime.of(2020, 1, 1, 0, 0);
    private static final LocalDateTime HASTA = LocalDateTime.of(2020, 2, 1, 0, 0);

    @Autowired private AuditoriaInventarioDao auditoriaDao;
    @Autowired private PrestamoDao prestamoDao;
    @PersistenceContext private EntityManager em;

    @Test
    void elIpiCuentaElUltimoConteoDeCadaTitulo() {
        Integer libroA = idLibro(0);
        Integer libroB = idLibro(1);
        auditoria(libroA, LocalDateTime.of(2020, 1, 10, 9, 0), 3, 2); // primero no coincidia
        auditoria(libroA, LocalDateTime.of(2020, 1, 20, 9, 0), 3, 3); // se volvio a contar y coincide
        auditoria(libroB, LocalDateTime.of(2020, 1, 15, 9, 0), 5, 4); // no coincide

        assertEquals(2, auditoriaDao.contarAuditadosEntre(DESDE, HASTA));   // 2 titulos, no 3 conteos
        assertEquals(1, auditoriaDao.contarCoincidentesEntre(DESDE, HASTA));
        assertEquals(3, auditoriaDao.listarEntre(DESDE, HASTA).size());
    }

    @Test
    void elConteoFisicoNoPuedeSerNegativo() {
        assertThrows(RuntimeException.class, () -> auditoria(idLibro(0), LocalDateTime.of(2020, 1, 10, 9, 0), 3, -1));
    }

    @Test
    void elTapPromediaLosMinutosDeAtencion() {
        prestamoConAtencion(LocalDateTime.of(2020, 1, 15, 10, 0), 120); // 2 min
        prestamoConAtencion(LocalDateTime.of(2020, 1, 15, 11, 0), 240); // 4 min

        assertEquals(3.0, prestamoDao.promedioMinutosAtencionEntre(DESDE, HASTA), 0.001);
        assertEquals(2, prestamoDao.contarConInicioAtencionEntre(DESDE, HASTA));
    }

    @Test
    void elInicioDeLaAtencionNoPuedeSerDespuesDelPrestamo() {
        assertThrows(RuntimeException.class,
                () -> prestamoConAtencion(LocalDateTime.of(2020, 1, 15, 10, 0), -60));
    }

    private Integer idLibro(int posicion) {
        return ((Number) em.createNativeQuery("SELECT id_libro FROM libro ORDER BY id_libro LIMIT 1 OFFSET " + posicion)
                .getSingleResult()).intValue();
    }

    private void auditoria(Integer idLibro, LocalDateTime fecha, int stockSistema, int conteoFisico) {
        em.createNativeQuery("INSERT INTO auditoria_inventario (id_libro, id_administrador, fecha_auditoria, stock_sistema, conteo_fisico) "
                        + "VALUES (:l, (SELECT id_usuario FROM administrador ORDER BY id_usuario LIMIT 1), :f, :s, :c)")
                .setParameter("l", idLibro).setParameter("f", fecha).setParameter("s", stockSistema).setParameter("c", conteoFisico)
                .executeUpdate();
    }

    private void prestamoConAtencion(LocalDateTime confirmacion, int segundos) {
        em.createNativeQuery("INSERT INTO prestamo (id_prestatario, id_encargado, id_libro, fecha_prestamo, inicio_atencion, "
                        + "fecha_devolucion_prevista, estado_prestamo) VALUES ("
                        + "(SELECT id_usuario FROM prestatario ORDER BY id_usuario LIMIT 1), "
                        + "(SELECT id_usuario FROM encargado ORDER BY id_usuario LIMIT 1), "
                        + "(SELECT id_libro FROM libro ORDER BY id_libro LIMIT 1), :p, :i, :lim, 'devuelto')")
                .setParameter("p", confirmacion).setParameter("i", confirmacion.minusSeconds(segundos))
                .setParameter("lim", confirmacion.plusHours(3))
                .executeUpdate();
    }
}
