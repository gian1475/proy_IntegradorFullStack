package com.backend.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Integer idReserva;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_prestatario", nullable = false)
    private Usuario prestatario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_libro", nullable = false)
    private Libro libro;

    @Column(name = "fecha_reserva", nullable = false)
    private LocalDateTime fechaReserva = LocalDateTime.now();

    @Column(name = "posicion_cola", nullable = false)
    private Integer posicionCola = 1;

    @Column(name = "fecha_recojo_limite")
    private LocalDateTime fechaRecojoLimite;

    @Column(name = "estado_reserva", nullable = false, length = 20)
    private String estadoReserva = "pendiente";

    public Reserva() {}

    public Reserva(Usuario prestatario, Libro libro, LocalDateTime fechaReserva, LocalDateTime fechaRecojoLimite, String estadoReserva) {
        this.prestatario = prestatario;
        this.libro = libro;
        this.fechaReserva = fechaReserva;
        this.fechaRecojoLimite = fechaRecojoLimite;
        this.estadoReserva = estadoReserva;
    }

    public Integer getIdReserva() { return idReserva; }
    public void setIdReserva(Integer idReserva) { this.idReserva = idReserva; }

    public Usuario getPrestatario() { return prestatario; }
    public void setPrestatario(Usuario prestatario) { this.prestatario = prestatario; }

    public Libro getLibro() { return libro; }
    public void setLibro(Libro libro) { this.libro = libro; }

    public LocalDateTime getFechaReserva() { return fechaReserva; }
    public void setFechaReserva(LocalDateTime fechaReserva) { this.fechaReserva = fechaReserva; }

    public Integer getPosicionCola() { return posicionCola; }
    public void setPosicionCola(Integer posicionCola) { this.posicionCola = posicionCola; }

    public LocalDateTime getFechaRecojoLimite() { return fechaRecojoLimite; }
    public void setFechaRecojoLimite(LocalDateTime fechaRecojoLimite) { this.fechaRecojoLimite = fechaRecojoLimite; }

    public String getEstadoReserva() { return estadoReserva; }
    public void setEstadoReserva(String estadoReserva) { this.estadoReserva = estadoReserva; }
}
