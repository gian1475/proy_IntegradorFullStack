package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dto.reserva.ReservaDTO;
import com.alejandria.biblioteca.dto.reserva.ReservaRespuestaDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cola de espera contra la BD real (Neon). Necesita la migracion_03 (estado 'en_espera').
 * Corre dentro de una transacción que se deshace al final: no deja datos guardados.
 */
@DisplayName("Cola de espera en la BD: reservar sin stock, cancelar y avanzar (RF04 y RF16)")
@SpringBootTest
@Transactional
class ColaEsperaBdTest {

    @Autowired private ReservaService reservaService;
    @PersistenceContext private EntityManager em;

    @Test
    void elEjemplarQueSeLiberaPasaAlPrimeroDeLaColaYLosDemasAvanzan() {
        Integer idLibro = idDe("INSERT INTO libro (id_categoria, titulo, isbn, anio_publicacion, editorial, stock, estado_libro) "
                + "VALUES (4, 'Libro de prueba cola', '0000000000001', 2020, 'Prueba', 1, 'disponible') RETURNING id_libro");
        Integer ana = prestatario("90000001");
        Integer beto = prestatario("90000002");
        Integer caro = prestatario("90000003");

        ReservaRespuestaDTO deAna = reservaService.crear(reserva(ana, idLibro, true));   // se lleva el unico ejemplar
        ReservaRespuestaDTO deBeto = reservaService.crear(reserva(beto, idLibro, false)); // sin stock: cola, puesto 1
        ReservaRespuestaDTO deCaro = reservaService.crear(reserva(caro, idLibro, false)); // cola, puesto 2
        em.flush();

        assertEquals("pendiente", deAna.estadoReserva());
        assertEquals("en_espera", estado(deBeto.idReserva()));
        assertEquals(1, posicion(deBeto.idReserva()));
        assertEquals(2, posicion(deCaro.idReserva()));
        assertEquals(0, stock(idLibro));

        reservaService.cancelar(deAna.idReserva()); // el ejemplar no vuelve al stock: es para Beto
        em.flush();

        assertEquals("pendiente", estado(deBeto.idReserva()));
        assertNotNull(em.createNativeQuery("SELECT fecha_recojo_limite FROM reserva WHERE id_reserva = :id")
                .setParameter("id", deBeto.idReserva()).getSingleResult());
        assertEquals(1, posicion(deCaro.idReserva())); // Caro avanza al puesto 1
        assertEquals(0, stock(idLibro));
        assertEquals("reservado", estadoLibro(idLibro));

        reservaService.cancelar(deCaro.idReserva()); // sale de la cola: el stock no cambia
        reservaService.cancelar(deBeto.idReserva()); // ya nadie espera: el ejemplar vuelve al stock
        em.flush();

        assertEquals(1, stock(idLibro));
        assertEquals("disponible", estadoLibro(idLibro));
    }

    private ReservaDTO reserva(Integer idUsuario, Integer idLibro, boolean conFecha) {
        ReservaDTO dto = new ReservaDTO();
        dto.setIdUsuario(idUsuario);
        dto.setIdLibro(idLibro);
        // mañana al final de la franja de la mañana: siempre es futuro y dentro del horario
        dto.setFechaRecojoLimite(conFecha ? LocalDate.now().plusDays(1).atTime(LocalTime.of(11, 30)) : null);
        return dto;
    }

    private Integer prestatario(String dni) {
        Integer id = idDe("INSERT INTO usuario (nombre, apellido, dni, correo, contrasena, estado) VALUES "
                + "('Prueba', 'Cola', '" + dni + "', 'cola" + dni + "@prueba.pe', 'x', 'activo') RETURNING id_usuario");
        em.createNativeQuery("INSERT INTO prestatario (id_usuario, tiempos_suspendido) VALUES (" + id + ", 0)").executeUpdate();
        return id;
    }

    private String estado(Integer idReserva) {
        return (String) em.createNativeQuery("SELECT estado_reserva FROM reserva WHERE id_reserva = :id")
                .setParameter("id", idReserva).getSingleResult();
    }

    private int posicion(Integer idReserva) {
        return idDe("SELECT posicion_cola FROM reserva WHERE id_reserva = " + idReserva);
    }

    private int stock(Integer idLibro) {
        return idDe("SELECT stock FROM libro WHERE id_libro = " + idLibro);
    }

    private String estadoLibro(Integer idLibro) {
        return (String) em.createNativeQuery("SELECT estado_libro FROM libro WHERE id_libro = " + idLibro).getSingleResult();
    }

    private Integer idDe(String sql) {
        return ((Number) em.createNativeQuery(sql).getSingleResult()).intValue();
    }
}
