package com.backend.demo.controller;

import com.backend.demo.dao.LibroDao;
import com.backend.demo.entity.Libro;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/libros")
@CrossOrigin(origins = "*")
public class LibroController {

    private final LibroDao libroDao;

    @Autowired
    public LibroController(LibroDao libroDao) {
        this.libroDao = libroDao;
    }

    @GetMapping
    public List<Libro> listarTodos() {
        return libroDao.findAll();
    }

    @GetMapping("/autor/{nombre}")
    public List<Libro> buscarPorAutor(@PathVariable String nombre) {
        return libroDao.findByAutorNombreOrApellido(nombre, nombre);
    }

    @GetMapping("/buscar")
    public List<Libro> buscarPorTitulo(@RequestParam(name = "q", defaultValue = "") String query) {
        if (query.trim().isEmpty()) {
            return libroDao.findAll();
        }
        return libroDao.findByTituloContainingIgnoreCase(query);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Libro> obtenerPorId(@PathVariable Integer id) {
        return libroDao.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Libro> guardar(@RequestBody Libro libro) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libroDao.save(libro));
    }
}
