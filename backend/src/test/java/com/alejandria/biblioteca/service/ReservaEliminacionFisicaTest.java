package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.LibroDao;
import com.alejandria.biblioteca.dao.ReservaDao;
import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.entity.Reserva;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Eliminación física contra la BD real (Neon): la fila desaparece de la tabla reserva.
 * Corre dentro de una transacción que se deshace al final: no deja datos guardados.
 */
@DisplayName("Eliminación física de una reserva en la BD (DELETE real)")
@SpringBootTest
@Transactional
class ReservaEliminacionFisicaTest {

    @Autowired private ReservaService reservaService;
    @Autowired private ReservaDao reservaDao;
    @Autowired private UsuarioDao usuarioDao;
    @Autowired private LibroDao libroDao;
    @PersistenceContext private EntityManager em;

    @Test
    void laFilaDeLaReservaCanceladaDesapareceDeLaTabla() {
        Reserva r = new Reserva();
        r.setPrestatario(usuarioDao.getReferenceById(idDe("SELECT id_usuario FROM prestatario LIMIT 1")));
        r.setLibro(libroDao.getReferenceById(idDe("SELECT id_libro FROM libro LIMIT 1")));
        r.setFechaReserva(LocalDateTime.now().minusDays(1));
        r.setFechaRecojoLimite(LocalDateTime.now().minusHours(20));
        r.setEstadoReserva("cancelada");
        Integer id = reservaDao.saveAndFlush(r).getIdReserva();
        assertEquals(1, filasConId(id));

        reservaService.eliminar(id);
        em.flush();

        assertEquals(0, filasConId(id)); // ya no está en la tabla: no es un cambio de estado
    }

    private long filasConId(Integer idReserva) {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM reserva WHERE id_reserva = :id")
                .setParameter("id", idReserva).getSingleResult()).longValue();
    }

    private Integer idDe(String sql) {
        return ((Number) em.createNativeQuery(sql).getSingleResult()).intValue();
    }
}
