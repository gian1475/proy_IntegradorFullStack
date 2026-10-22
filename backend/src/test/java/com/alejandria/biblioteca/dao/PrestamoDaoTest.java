package com.alejandria.biblioteca.dao;

import com.alejandria.biblioteca.entity.Prestamo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba las consultas de préstamos contra la BD real (Neon).
 * Cada prueba corre dentro de una transacción que se deshace al final: no deja datos guardados.
 */
@DisplayName("Consultas de préstamos en la BD (hora de Lima vs hora de la BD)")
@SpringBootTest
@Transactional
class PrestamoDaoTest {

    @Autowired private PrestamoDao prestamoDao;
    @Autowired private UsuarioDao usuarioDao;
    @Autowired private LibroDao libroDao;
    @PersistenceContext private EntityManager em;

    @Test
    void unPrestamoRecienRegistradoNoSaleVencido() {
        LocalDateTime ahora = LocalDateTime.now();
        Prestamo p = prestamoDePrueba(ahora, ahora.plusHours(3));

        prestamoDao.marcarVencidos(LocalDateTime.now());

        assertEquals("activo", estadoEnLaBd(p));
    }

    @Test
    void unPrestamoConLaHoraLimitePasadaSaleVencido() {
        LocalDateTime ahora = LocalDateTime.now();
        Prestamo p = prestamoDePrueba(ahora.minusHours(3), ahora.minusMinutes(1));

        prestamoDao.marcarVencidos(LocalDateTime.now());

        assertEquals("vencido", estadoEnLaBd(p));
    }

    @Test
    void laConsultaDeUnaTablaSoloTraeLosPrestamosDeEsePrestatario() {
        LocalDateTime ahora = LocalDateTime.now();
        Prestamo p = prestamoDePrueba(ahora, ahora.plusHours(3));
        Integer idPrestatario = p.getPrestatario().getIdUsuario();

        var historial = prestamoDao.buscarPorPrestatario(idPrestatario);

        assertTrue(historial.stream().anyMatch(x -> x.getIdPrestamo().equals(p.getIdPrestamo())));
        assertTrue(historial.stream().allMatch(x -> x.getPrestatario().getIdUsuario().equals(idPrestatario)));
    }

    @Test
    void unaSancionQueTerminaHoySigueActivaTodoElDia() {
        LocalDateTime ahora = LocalDateTime.now();
        Prestamo p = prestamoDePrueba(ahora.minusDays(2), ahora.minusDays(2).plusHours(3));
        Integer idPrestatario = p.getPrestatario().getIdUsuario();
        Integer idIncidencia = idDe("INSERT INTO incidencia (id_prestamo, tipo_incidencia, descripcion, estado_incidencia) "
                + "VALUES (" + p.getIdPrestamo() + ", 'retraso', 'prueba', 'pendiente') RETURNING id_incidencia");
        em.createNativeQuery("INSERT INTO sancion (id_incidencia, tipo_sancion, fecha_inicio, fecha_fin, estado_sancion) "
                + "VALUES (:inc, 'advertencia', :inicio, :fin, 'activa')")
                .setParameter("inc", idIncidencia)
                .setParameter("inicio", ahora.minusDays(1))
                .setParameter("fin", ahora.toLocalDate().atStartOfDay()) // termina hoy
                .executeUpdate();

        assertTrue(prestamoDao.tieneSancionActiva(idPrestatario, ahora.toLocalDate()));
    }

    @Test
    void lasConsultasDelDashboardCuentanBienEnLaBd() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicioMes = ahora.toLocalDate().withDayOfMonth(1).atStartOfDay();
        long antesPrestamos = prestamoDao.contarPrestamosEntre(inicioMes, inicioMes.plusMonths(1));
        long antesRetraso = prestamoDao.contarDevueltosConRetrasoEntre(inicioMes, inicioMes.plusMonths(1));
        long antesDanados = prestamoDao.contarDevueltosDanadosEntre(inicioMes, inicioMes.plusMonths(1));

        // un préstamo de hoy, devuelto 10 minutos tarde y dañado
        Prestamo p = prestamoDePrueba(ahora.minusHours(4), ahora.minusHours(1));
        p.setFechaDevolucionReal(ahora.minusMinutes(50));
        p.setEstadoFisicoDevolucion("dañado");
        p.setEstadoPrestamo("devuelto");
        prestamoDao.saveAndFlush(p);

        assertEquals(antesPrestamos + 1, prestamoDao.contarPrestamosEntre(inicioMes, inicioMes.plusMonths(1)));
        assertEquals(antesRetraso + 1, prestamoDao.contarDevueltosConRetrasoEntre(inicioMes, inicioMes.plusMonths(1)));
        assertEquals(antesDanados + 1, prestamoDao.contarDevueltosDanadosEntre(inicioMes, inicioMes.plusMonths(1)));
        // las demás consultas del dashboard también corren sin error en la BD
        assertTrue(prestamoDao.contarPrestamosConIncidenciaEntre(inicioMes, inicioMes.plusMonths(1)) >= 0);
        assertTrue(prestamoDao.prestamosPorCategoriaEntre(inicioMes, inicioMes.plusMonths(1)).size() >= 5);
        assertTrue(prestamoDao.prestamosPorDiaDesde(ahora.toLocalDate().atStartOfDay()).size() >= 1);
    }

    @Test
    void unaMultaGuardaSuMonto() { // RF06: multa con monto (migración 02)
        Integer idIncidencia = incidenciaDePrueba();
        em.createNativeQuery("INSERT INTO sancion (id_incidencia, tipo_sancion, monto, fecha_inicio, estado_sancion) "
                + "VALUES (:inc, 'multa', 15.50, :inicio, 'activa')")
                .setParameter("inc", idIncidencia)
                .setParameter("inicio", LocalDateTime.now())
                .executeUpdate();

        Object monto = em.createNativeQuery("SELECT monto FROM sancion WHERE id_incidencia = :inc")
                .setParameter("inc", idIncidencia).getSingleResult();
        assertEquals(0, new java.math.BigDecimal("15.50").compareTo((java.math.BigDecimal) monto));
    }

    @Test
    void elMontoDeUnaMultaNoPuedeSerNegativo() {
        Integer idIncidencia = incidenciaDePrueba();
        assertThrows(RuntimeException.class, () ->
                em.createNativeQuery("INSERT INTO sancion (id_incidencia, tipo_sancion, monto, fecha_inicio, estado_sancion) "
                        + "VALUES (:inc, 'multa', -5, :inicio, 'activa')")
                        .setParameter("inc", idIncidencia)
                        .setParameter("inicio", LocalDateTime.now())
                        .executeUpdate());
    }

    // ---------- Auxiliares ----------

    private Integer incidenciaDePrueba() {
        LocalDateTime ahora = LocalDateTime.now();
        Prestamo p = prestamoDePrueba(ahora.minusDays(1), ahora.minusDays(1).plusHours(3));
        return idDe("INSERT INTO incidencia (id_prestamo, tipo_incidencia, descripcion, estado_incidencia) "
                + "VALUES (" + p.getIdPrestamo() + ", 'retraso', 'prueba', 'pendiente') RETURNING id_incidencia");
    }

    /** Guarda un préstamo con un prestatario, un encargado y un libro que ya existen en la BD. */
    private Prestamo prestamoDePrueba(LocalDateTime inicio, LocalDateTime limite) {
        Integer idPrestatario = idDe("SELECT id_usuario FROM prestatario LIMIT 1");
        Integer idEncargado = idDe("SELECT id_usuario FROM encargado LIMIT 1");
        Integer idLibro = idDe("SELECT id_libro FROM libro LIMIT 1");

        Prestamo p = new Prestamo();
        p.setPrestatario(usuarioDao.getReferenceById(idPrestatario));
        p.setEncargado(usuarioDao.getReferenceById(idEncargado));
        p.setLibro(libroDao.getReferenceById(idLibro));
        p.setFechaPrestamo(inicio);
        p.setFechaDevolucionPrevista(limite);
        p.setAmpliado(false);
        p.setEstadoPrestamo("activo");
        return prestamoDao.saveAndFlush(p);
    }

    /** Vuelve a leer el estado desde la BD (no desde la memoria de Hibernate). */
    private String estadoEnLaBd(Prestamo p) {
        em.clear();
        return prestamoDao.findById(p.getIdPrestamo()).orElseThrow().getEstadoPrestamo();
    }

    private Integer idDe(String sql) {
        return ((Number) em.createNativeQuery(sql).getSingleResult()).intValue();
    }
}
