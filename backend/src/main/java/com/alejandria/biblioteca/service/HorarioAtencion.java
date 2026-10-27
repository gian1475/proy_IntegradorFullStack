package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.exception.NegocioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Horario de atención de la biblioteca (lunes a sábado, de 8:00 a 20:00).
 * Lo usan préstamos y reservas. Las horas se configuran en application.properties.
 */
// horario de atencion (RF01 y RF05): no se presta fuera de 8:00-20:00 y la hora limite nunca pasa del cierre.
// para probar de noche se puede cambiar app.horario.cierre en application.properties (sin tocar el codigo)
@Component
public class HorarioAtencion {

    private final LocalTime apertura;
    private final LocalTime cierre;
    private final Clock reloj;

    @Autowired
    public HorarioAtencion(@Value("${app.horario.apertura:08:00}") String apertura,
                           @Value("${app.horario.cierre:20:00}") String cierre) {
        this(LocalTime.parse(apertura), LocalTime.parse(cierre), Clock.systemDefaultZone());
    }

    /** Permite usar un reloj falso en las pruebas JUnit. */
    HorarioAtencion(LocalTime apertura, LocalTime cierre, Clock reloj) {
        this.apertura = apertura;
        this.cierre = cierre;
        this.reloj = reloj;
    }

    /** Fecha y hora actual (hora de Lima). Los services la piden aquí para que las pruebas puedan fijarla. */
    public LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    /** No se registran préstamos antes de abrir ni desde la hora de cierre. */
    public void validarAbierto(LocalDateTime momento) {
        LocalTime hora = momento.toLocalTime();
        if (hora.isBefore(apertura) || !hora.isBefore(cierre)) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "La biblioteca atiende de " + apertura + " a " + cierre + ". Fuera de ese horario no se registran préstamos");
        }
    }

    /** Si la hora límite pasa del cierre de ese día, queda en la hora de cierre. */
    // horario de atencion: 3 horas o 1 hora de ampliacion, pero nunca despues de las 20:00
    public LocalDateTime limitarAlCierre(LocalDateTime limite) {
        LocalDateTime cierreDelDia = limite.toLocalDate().atTime(cierre);
        return limite.isAfter(cierreDelDia) ? cierreDelDia : limite;
    }

    /** La hora límite de recojo de una reserva tiene que estar dentro del horario (después de abrir y hasta el cierre). */
    public boolean esHoraDeRecojo(LocalTime hora) {
        return hora.isAfter(apertura) && !hora.isAfter(cierre);
    }

    /** Cierre del día siguiente: hasta ahí tiene para recoger el libro el primero de la cola de espera. */
    public LocalDateTime cierreDeManana() {
        return ahora().toLocalDate().plusDays(1).atTime(cierre);
    }

    public LocalTime getApertura() { return apertura; }
    public LocalTime getCierre() { return cierre; }
}
