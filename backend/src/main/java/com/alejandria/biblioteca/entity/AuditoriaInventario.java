package com.alejandria.biblioteca.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * IPI (migración 05): conteo físico de un título que registra el administrador,
 * junto con el stock que tenía el sistema en ese momento.
 */
@Entity
@Table(name = "auditoria_inventario")
// lombok: @Getter y @Setter crean al compilar el get y el set de cada campo; @NoArgsConstructor, el constructor vacio q pide JPA
@Getter
@Setter
@NoArgsConstructor
public class AuditoriaInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Integer idAuditoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_libro", nullable = false)
    private Libro libro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_administrador", nullable = false)
    private Usuario administrador;

    @Column(name = "fecha_auditoria", nullable = false)
    private LocalDateTime fechaAuditoria;

    @Column(name = "stock_sistema", nullable = false)
    private Integer stockSistema;

    @Column(name = "conteo_fisico", nullable = false)
    private Integer conteoFisico;

    /** El título "coincide" si el conteo físico es igual al stock del sistema. */
    public boolean coincide() {
        return conteoFisico.equals(stockSistema);
    }
}
