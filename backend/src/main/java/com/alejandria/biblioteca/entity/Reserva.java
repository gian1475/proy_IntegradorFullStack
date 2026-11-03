package com.alejandria.biblioteca.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reserva")
// lombok: @Getter y @Setter crean al compilar el get y el set de cada campo; @NoArgsConstructor, el constructor vacio q pide JPA
@Getter
@Setter
@NoArgsConstructor
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

    public Reserva(Usuario prestatario, Libro libro, LocalDateTime fechaReserva, LocalDateTime fechaRecojoLimite, String estadoReserva) {
        this.prestatario = prestatario;
        this.libro = libro;
        this.fechaReserva = fechaReserva;
        this.fechaRecojoLimite = fechaRecojoLimite;
        this.estadoReserva = estadoReserva;
    }
}
