package com.alejandria.biblioteca.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "autor")
// lombok: @Getter y @Setter crean al compilar el get y el set de cada campo; @NoArgsConstructor, el constructor vacio q pide JPA
@Getter
@Setter
@NoArgsConstructor
public class Autor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_autor")
    private Integer idAutor;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 50)
    private String apellido;

    @Column(name = "nacionalidad", length = 50)
    private String nacionalidad;

    public Autor(Integer idAutor, String nombre, String apellido, String nacionalidad) {
        this.idAutor = idAutor;
        this.nombre = nombre;
        this.apellido = apellido;
        this.nacionalidad = nacionalidad;
    }

    public String getNombreCompleto() {
        return (nombre != null ? nombre : "") + " " + (apellido != null ? apellido : "");
    }
}
