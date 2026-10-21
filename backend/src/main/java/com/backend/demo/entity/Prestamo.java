package com.backend.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "prestamo")
public class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prestamo")
    private Integer idPrestamo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_prestatario", nullable = false)
    private Usuario prestatario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_encargado", nullable = false)
    private Usuario encargado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_libro", nullable = false)
    private Libro libro;

    @Column(name = "fecha_prestamo", nullable = false)
    private LocalDateTime fechaPrestamo;

    @Column(name = "fecha_devolucion_prevista", nullable = false)
    private LocalDateTime fechaDevolucionPrevista;

    @Column(name = "fecha_devolucion_real")
    private LocalDateTime fechaDevolucionReal;

    @Column(name = "ampliado", nullable = false)
    private Boolean ampliado = false;

    @Column(name = "estado_fisico_devolucion", length = 20)
    private String estadoFisicoDevolucion;

    @Column(name = "estado_prestamo", nullable = false, length = 20)
    private String estadoPrestamo = "activo";

    public Prestamo() {}

    public Integer getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Integer idPrestamo) { this.idPrestamo = idPrestamo; }

    public Usuario getPrestatario() { return prestatario; }
    public void setPrestatario(Usuario prestatario) { this.prestatario = prestatario; }

    public Usuario getEncargado() { return encargado; }
    public void setEncargado(Usuario encargado) { this.encargado = encargado; }

    public Libro getLibro() { return libro; }
    public void setLibro(Libro libro) { this.libro = libro; }

    public LocalDateTime getFechaPrestamo() { return fechaPrestamo; }
    public void setFechaPrestamo(LocalDateTime fechaPrestamo) { this.fechaPrestamo = fechaPrestamo; }

    public LocalDateTime getFechaDevolucionPrevista() { return fechaDevolucionPrevista; }
    public void setFechaDevolucionPrevista(LocalDateTime fechaDevolucionPrevista) { this.fechaDevolucionPrevista = fechaDevolucionPrevista; }

    public LocalDateTime getFechaDevolucionReal() { return fechaDevolucionReal; }
    public void setFechaDevolucionReal(LocalDateTime fechaDevolucionReal) { this.fechaDevolucionReal = fechaDevolucionReal; }

    public Boolean getAmpliado() { return ampliado; }
    public void setAmpliado(Boolean ampliado) { this.ampliado = ampliado; }

    public String getEstadoFisicoDevolucion() { return estadoFisicoDevolucion; }
    public void setEstadoFisicoDevolucion(String estadoFisicoDevolucion) { this.estadoFisicoDevolucion = estadoFisicoDevolucion; }

    public String getEstadoPrestamo() { return estadoPrestamo; }
    public void setEstadoPrestamo(String estadoPrestamo) { this.estadoPrestamo = estadoPrestamo; }
}
