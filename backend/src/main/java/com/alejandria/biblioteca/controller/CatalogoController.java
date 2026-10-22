package com.alejandria.biblioteca.controller;

import com.alejandria.biblioteca.dto.libro.LibroDTO;
import com.alejandria.biblioteca.service.LibroService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Listas de categorías y autores para los formularios (por ejemplo, el de libros). */
@RestController
@RequestMapping("/api")
public class CatalogoController {

    private final LibroService libroService;

    public CatalogoController(LibroService libroService) {
        this.libroService = libroService;
    }

    @GetMapping("/categorias")
    public List<LibroDTO.CategoriaResumen> listarCategorias() {
        return libroService.listarCategorias();
    }

    @GetMapping("/autores")
    public List<LibroDTO.AutorResumen> listarAutores() {
        return libroService.listarAutores();
    }
}
