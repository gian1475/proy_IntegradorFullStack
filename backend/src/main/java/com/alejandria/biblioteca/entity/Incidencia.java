package com.alejandria.biblioteca.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** RF06: algo que pasó con un préstamo (retraso, daño o pérdida). Su sanción está en la tabla sancion. */
@Entity
@Table(name = "incidencia")
// lombok: @Getter y @Setter crean al compilar el get y el set de cada campo; @NoArgsConstructor, el constructor vacio q pide JPA
@Getter
@Setter
@NoArgsConstructor
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_incidencia")
    private Integer idIncidencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_prestamo", nullable = false)
    private Prestamo prestamo;

    @Column(name = "tipo_incidencia", nullable = false, length = 50)
    private String tipoIncidencia;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "fecha_incidencia", nullable = false)
    private LocalDateTime fechaIncidencia;

    @Column(name = "estado_incidencia", nullable = false, length = 20)
    private String estadoIncidencia = "pendiente";
}
