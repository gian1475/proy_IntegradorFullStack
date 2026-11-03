package com.alejandria.biblioteca.dto.incidencia;

import com.alejandria.biblioteca.entity.Incidencia;
import com.alejandria.biblioteca.entity.Prestamo;
import com.alejandria.biblioteca.entity.Sancion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Una incidencia con el préstamo (prestatario y libro) y su sanción, tal como la ve el encargado. */
public record IncidenciaDTO(
        Integer idIncidencia,
        Integer idPrestamo,
        String prestatario,
        String dniPrestatario,
        String tituloLibro,
        String tipoIncidencia,
        String descripcion,
        LocalDateTime fechaIncidencia,
        String estadoIncidencia,
        Integer idSancion,
        String tipoSancion,
        BigDecimal monto,
        String descripcionSancion,
        LocalDateTime inicioSancion,
        LocalDateTime finSancion,
        String estadoSancion
) {
    /** Debe llamarse dentro de una transacción, porque lee el préstamo, el prestatario y el libro (LAZY). */
    public static IncidenciaDTO desde(Sancion s) {
        Incidencia i = s.getIncidencia();
        Prestamo p = i.getPrestamo();
        return new IncidenciaDTO(i.getIdIncidencia(), p.getIdPrestamo(),
                p.getPrestatario().getNombre() + " " + p.getPrestatario().getApellido(), p.getPrestatario().getDni(),
                p.getLibro().getTitulo(), i.getTipoIncidencia(), i.getDescripcion(), i.getFechaIncidencia(),
                i.getEstadoIncidencia(), s.getIdSancion(), s.getTipoSancion(), s.getMonto(), s.getDescripcion(),
                s.getFechaInicio(), s.getFechaFin(), s.getEstadoSancion());
    }
}
