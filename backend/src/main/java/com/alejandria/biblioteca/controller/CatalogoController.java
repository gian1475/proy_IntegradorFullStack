package com.alejandria.biblioteca.controller;

import com.alejandria.biblioteca.dto.libro.LibroCategoriaDTO;
import com.alejandria.biblioteca.dto.libro.LibroDTO;
import com.alejandria.biblioteca.seguridad.Rol;
import jakarta.annotation.security.RolesAllowed;
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

    // CONSULTA DE DOS TABLAS: GET /api/libros-por-categoria?categoria=Novela (vacio = todas). libro JOIN categoria
    @GetMapping("/libros-por-categoria")
    @RolesAllowed({Rol.ADMINISTRADOR, Rol.ENCARGADO})
    public List<LibroCategoriaDTO> librosPorCategoria(@RequestParam(defaultValue = "") String categoria) {
        return libroService.consultarConCategoria(categoria);
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
