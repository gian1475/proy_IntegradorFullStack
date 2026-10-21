package com.backend.demo.controller;

import com.backend.demo.dao.AutorDao;
import com.backend.demo.dao.CategoriaDao;
import com.backend.demo.entity.Autor;
import com.backend.demo.entity.Categoria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Listas de categorías y autores para los formularios (por ejemplo, el de libros). */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class CatalogoController {

    private final CategoriaDao categoriaDao;
    private final AutorDao autorDao;

    @Autowired
    public CatalogoController(CategoriaDao categoriaDao, AutorDao autorDao) {
        this.categoriaDao = categoriaDao;
        this.autorDao = autorDao;
    }

    @GetMapping("/categorias")
    public List<Categoria> listarCategorias() {
        return categoriaDao.findAll();
    }

    @GetMapping("/autores")
    public List<Autor> listarAutores() {
        return autorDao.findAll();
    }
}
