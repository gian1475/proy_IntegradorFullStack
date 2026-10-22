package com.alejandria.biblioteca.dto.prestamo;

import java.time.LocalDateTime;

/** Préstamo con el nombre del prestatario y el título del libro (resultado del JOIN). */
public class PrestamoDetalleDTO {
    private Integer idPrestamo;
    private String nombrePrestatario;
    private String dniPrestatario;
    private String tituloLibro;
    private LocalDateTime fechaPrestamo;
    private LocalDateTime fechaDevolucionPrevista;
    private Boolean ampliado;
    private String estadoPrestamo;

    public PrestamoDetalleDTO(Integer idPrestamo, String nombre, String apellido, String dniPrestatario,
                              String tituloLibro, LocalDateTime fechaPrestamo,
                              LocalDateTime fechaDevolucionPrevista, Boolean ampliado, String estadoPrestamo) {
        this.idPrestamo = idPrestamo;
        this.nombrePrestatario = nombre + " " + apellido;
        this.dniPrestatario = dniPrestatario;
        this.tituloLibro = tituloLibro;
        this.fechaPrestamo = fechaPrestamo;
        this.fechaDevolucionPrevista = fechaDevolucionPrevista;
        this.ampliado = ampliado;
        this.estadoPrestamo = estadoPrestamo;
    }

    public Integer getIdPrestamo() { return idPrestamo; }
    public String getNombrePrestatario() { return nombrePrestatario; }
    public String getDniPrestatario() { return dniPrestatario; }
    public String getTituloLibro() { return tituloLibro; }
    public LocalDateTime getFechaPrestamo() { return fechaPrestamo; }
    public LocalDateTime getFechaDevolucionPrevista() { return fechaDevolucionPrevista; }
    public Boolean getAmpliado() { return ampliado; }
    public String getEstadoPrestamo() { return estadoPrestamo; }
}
