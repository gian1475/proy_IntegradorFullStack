package com.backend.demo.dto;

import java.util.List;

/** Datos que envía el Administrador para crear o actualizar un libro. */
public class LibroFormDTO {
    private String titulo;
    private String isbn;
    private Integer anioPublicacion;
    private String editorial;
    private Integer stock;
    private Integer idCategoria;
    private List<Integer> idsAutores;

    public LibroFormDTO() {}

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public Integer getAnioPublicacion() { return anioPublicacion; }
    public void setAnioPublicacion(Integer anioPublicacion) { this.anioPublicacion = anioPublicacion; }

    public String getEditorial() { return editorial; }
    public void setEditorial(String editorial) { this.editorial = editorial; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Integer getIdCategoria() { return idCategoria; }
    public void setIdCategoria(Integer idCategoria) { this.idCategoria = idCategoria; }

    public List<Integer> getIdsAutores() { return idsAutores; }
    public void setIdsAutores(List<Integer> idsAutores) { this.idsAutores = idsAutores; }
}
