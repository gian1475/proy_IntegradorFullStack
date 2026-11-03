package com.alejandria.biblioteca.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "libro")
// lombok: @Getter y @Setter crean al compilar el get y el set de cada campo; @NoArgsConstructor, el constructor vacio q pide JPA
@Getter
@Setter
@NoArgsConstructor
public class Libro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_libro")
    private Integer idLibro;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "isbn", nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(name = "anio_publicacion", nullable = false)
    private Integer anioPublicacion;

    @Column(name = "editorial", nullable = false, length = 100)
    private String editorial;

    @Column(name = "stock", nullable = false)
    private Integer stock = 0;

    @Column(name = "estado_libro", nullable = false, length = 20)
    private String estadoLibro = "disponible";

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "libro_autor",
        joinColumns = @JoinColumn(name = "id_libro"),
        inverseJoinColumns = @JoinColumn(name = "id_autor")
    )
    private Set<Autor> autores = new HashSet<>();

    public Libro(Integer idLibro, Categoria categoria, String titulo, String isbn, Integer anioPublicacion, String editorial, Integer stock, String estadoLibro) {
        this.idLibro = idLibro;
        this.categoria = categoria;
        this.titulo = titulo;
        this.isbn = isbn;
        this.anioPublicacion = anioPublicacion;
        this.editorial = editorial;
        this.stock = stock;
        this.estadoLibro = estadoLibro;
    }
}
