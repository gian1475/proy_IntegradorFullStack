package com.backend.demo.dto;

import com.backend.demo.entity.Prestamo;

import java.time.LocalDateTime;

/** Datos de un préstamo tal como están en la tabla prestamo (solo ids, sin nombres). */
public class PrestamoDTO {
    private Integer idPrestamo;
    private Integer idPrestatario;
    private Integer idEncargado;
    private Integer idLibro;
    private LocalDateTime fechaPrestamo;
    private LocalDateTime fechaDevolucionPrevista;
    private LocalDateTime fechaDevolucionReal;
    private Boolean ampliado;
    private String estadoFisicoDevolucion;
    private String estadoPrestamo;

    public PrestamoDTO() {}

    public static PrestamoDTO desde(Prestamo p) {
        PrestamoDTO dto = new PrestamoDTO();
        dto.idPrestamo = p.getIdPrestamo();
        dto.idPrestatario = p.getPrestatario().getIdUsuario();
        dto.idEncargado = p.getEncargado().getIdUsuario();
        dto.idLibro = p.getLibro().getIdLibro();
        dto.fechaPrestamo = p.getFechaPrestamo();
        dto.fechaDevolucionPrevista = p.getFechaDevolucionPrevista();
        dto.fechaDevolucionReal = p.getFechaDevolucionReal();
        dto.ampliado = p.getAmpliado();
        dto.estadoFisicoDevolucion = p.getEstadoFisicoDevolucion();
        dto.estadoPrestamo = p.getEstadoPrestamo();
        return dto;
    }

    public Integer getIdPrestamo() { return idPrestamo; }
    public Integer getIdPrestatario() { return idPrestatario; }
    public Integer getIdEncargado() { return idEncargado; }
    public Integer getIdLibro() { return idLibro; }
    public LocalDateTime getFechaPrestamo() { return fechaPrestamo; }
    public LocalDateTime getFechaDevolucionPrevista() { return fechaDevolucionPrevista; }
    public LocalDateTime getFechaDevolucionReal() { return fechaDevolucionReal; }
    public Boolean getAmpliado() { return ampliado; }
    public String getEstadoFisicoDevolucion() { return estadoFisicoDevolucion; }
    public String getEstadoPrestamo() { return estadoPrestamo; }
}
