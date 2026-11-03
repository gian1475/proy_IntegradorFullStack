package com.alejandria.biblioteca.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** RF06: la sanción de una incidencia (advertencia, suspensión o multa). Cada incidencia tiene como máximo una. */
@Entity
@Table(name = "sancion")
// lombok: @Getter y @Setter crean al compilar el get y el set de cada campo; @NoArgsConstructor, el constructor vacio q pide JPA
@Getter
@Setter
@NoArgsConstructor
public class Sancion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sancion")
    private Integer idSancion;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_incidencia", nullable = false, unique = true)
    private Incidencia incidencia;

    @Column(name = "tipo_sancion", nullable = false, length = 50)
    private String tipoSancion;

    /** Solo las multas tienen monto (en soles). */
    @Column(name = "monto", precision = 8, scale = 2)
    private BigDecimal monto;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    /** Suspensión: cuándo termina. Multa: cuándo se pagó (null mientras está pendiente). */
    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "estado_sancion", nullable = false, length = 20)
    private String estadoSancion = "activa";
}
