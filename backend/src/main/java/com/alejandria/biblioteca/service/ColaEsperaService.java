package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.ReservaDao;
import com.alejandria.biblioteca.entity.Reserva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cola de espera de reservas (RF04 y RF16): decide quién recibe el próximo ejemplar que se libere.
 * La usan LibroService (cuando vuelve un ejemplar) y ReservaService (al entrar o salir de la cola).
 */
// cola de espera: si un libro no tiene stock, la reserva queda 'en_espera' con su posicion (1, 2, 3...).
// cuando vuelve un ejemplar NO se suma al stock: se lo lleva el primero de la cola, q pasa a 'pendiente'
// y tiene hasta el cierre del dia siguiente para recogerlo. los demas avanzan un puesto
@Service
public class ColaEsperaService {

    private static final Logger log = LoggerFactory.getLogger(ColaEsperaService.class);

    public static final String EN_ESPERA = "en_espera";

    private final ReservaDao reservaDao;
    private final HorarioAtencion horario;

    public ColaEsperaService(ReservaDao reservaDao, HorarioAtencion horario) {
        this.reservaDao = reservaDao;
        this.horario = horario;
    }

    /** Puesto que le toca al que entra ahora a la cola de ese libro. */
    @Transactional(readOnly = true)
    public int siguientePosicion(Integer idLibro) {
        return cola(idLibro).size() + 1;
    }

    /**
     * Si alguien espera ese libro, le pasa el ejemplar que se liberó: su reserva pasa a 'pendiente'
     * con fecha límite de recojo. Devuelve false si la cola está vacía (entonces el ejemplar vuelve al stock).
     */
    @Transactional
    public boolean asignarAlSiguiente(Integer idLibro) {
        List<Reserva> cola = cola(idLibro);
        if (cola.isEmpty()) {
            return false;
        }
        Reserva primera = cola.get(0);
        primera.setEstadoReserva("pendiente");
        primera.setPosicionCola(1);
        primera.setFechaRecojoLimite(horario.cierreDeManana());
        reservaDao.save(primera);
        renumerar(cola.subList(1, cola.size())); // los demas avanzan un puesto
        log.info("Cola de espera: el ejemplar del libro {} pasó a la reserva #{} (recojo hasta {})",
                idLibro, primera.getIdReserva(), primera.getFechaRecojoLimite());
        return true;
    }

    /** RF16: cuando alguien sale de la cola (canceló), los que estaban detrás avanzan un puesto. */
    @Transactional
    public void reordenar(Integer idLibro) {
        renumerar(cola(idLibro));
    }

    private List<Reserva> cola(Integer idLibro) {
        return reservaDao.findByLibroIdLibroAndEstadoReservaOrderByPosicionColaAscFechaReservaAsc(idLibro, EN_ESPERA);
    }

    /** Deja las posiciones seguidas: 1, 2, 3... en el orden en que llegaron. */
    private void renumerar(List<Reserva> enEspera) {
        for (int i = 0; i < enEspera.size(); i++) {
            Reserva r = enEspera.get(i);
            if (r.getPosicionCola() != i + 1) {
                r.setPosicionCola(i + 1);
                reservaDao.save(r);
            }
        }
    }
}
