package com.alejandria.biblioteca.dao;

import com.alejandria.biblioteca.dto.reporte.ReportePrestamoDTO;
import com.alejandria.biblioteca.entity.Prestamo;
import com.alejandria.biblioteca.service.RangoFechas;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    // ---------- Historial (RF07) y reportes (RF08) ----------

    @Test
    void elHistorialFiltraPorDniLibroYFechas() {
        LocalDateTime inicio = LocalDateTime.of(2020, 1, 15, 10, 0); // una fecha sin otros prestamos
        Prestamo p = prestamoDePrueba(inicio, inicio.plusHours(3));
        String dni = (String) em.createNativeQuery("SELECT dni FROM usuario WHERE id_usuario = "
                + p.getPrestatario().getIdUsuario()).getSingleResult();
        String titulo = (String) em.createNativeQuery("SELECT titulo FROM libro WHERE id_libro = "
                + p.getLibro().getIdLibro()).getSingleResult();
        LocalDateTime dia = inicio.toLocalDate().atStartOfDay();

        RangoFechas todas = new RangoFechas(null, null);
        assertTrue(contiene(prestamoDao.buscarHistorial(null, null, todas.inicio(), todas.fin()), p));
        assertTrue(contiene(prestamoDao.buscarHistorial(dni, null, dia, dia.plusDays(1)), p));
        assertTrue(contiene(prestamoDao.buscarHistorial(null, titulo.substring(0, 4).toLowerCase(), dia, dia.plusDays(1)), p));
        assertFalse(contiene(prestamoDao.buscarHistorial("00000000", null, todas.inicio(), todas.fin()), p)); // otro prestatario
        assertFalse(contiene(prestamoDao.buscarHistorial(null, null, dia.plusDays(1), todas.fin()), p));    // fuera del rango
    }

    @Test
    void lasConsultasDeLosReportesTraenLosDatosDelPeriodo() {
        LocalDateTime inicio = LocalDateTime.of(2020, 2, 10, 9, 0);
        Prestamo p = prestamoDePrueba(inicio, inicio.plusHours(3));
        p.setFechaDevolucionReal(inicio.plusMinutes(90)); // estuvo 1 h 30 min en sala
        p.setEstadoFisicoDevolucion("bueno");
        p.setEstadoPrestamo("devuelto");
        prestamoDao.saveAndFlush(p);
        Integer idIncidencia = idDe("INSERT INTO incidencia (id_prestamo, tipo_incidencia, descripcion, fecha_incidencia, estado_incidencia) "
                + "VALUES (" + p.getIdPrestamo() + ", 'retraso', 'prueba', '2020-02-10 12:30', 'pendiente') RETURNING id_incidencia");
        em.createNativeQuery("INSERT INTO sancion (id_incidencia, tipo_sancion, monto, fecha_inicio, estado_sancion) "
                + "VALUES (" + idIncidencia + ", 'multa', 10, '2020-02-10 12:30', 'activa')").executeUpdate();
        LocalDateTime dia = inicio.toLocalDate().atStartOfDay();

        assertTrue(prestamoDao.listarDevoluciones(dia, dia.plusDays(1)).stream()
                .anyMatch(d -> d.idPrestamo().equals(p.getIdPrestamo())));

        List<Object[]> incidencias = prestamoDao.listarIncidenciasConSancion(dia, dia.plusDays(1));
        assertEquals(1, incidencias.size());
        assertEquals("multa", incidencias.get(0)[9]);

        List<Object[]> tiempos = prestamoDao.tiemposPorCategoria(dia, dia.plusDays(1));
        assertEquals(1, tiempos.size());
        assertEquals(90, ((Number) tiempos.get(0)[2]).intValue()); // minimo
        assertEquals(90, ((Number) tiempos.get(0)[4]).intValue()); // maximo
    }

    private boolean contiene(List<ReportePrestamoDTO> filas, Prestamo p) {
        return filas.stream().anyMatch(f -> f.idPrestamo().equals(p.getIdPrestamo()));
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
