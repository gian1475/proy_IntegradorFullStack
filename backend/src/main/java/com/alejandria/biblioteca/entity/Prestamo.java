package com.alejandria.biblioteca.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "prestamo")
// lombok: @Getter y @Setter crean al compilar el get y el set de cada campo; @NoArgsConstructor, el constructor vacio q pide JPA
@Getter
@Setter
@NoArgsConstructor
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
}
