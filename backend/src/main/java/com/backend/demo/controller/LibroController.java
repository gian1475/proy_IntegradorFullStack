package com.backend.demo.controller;

import com.backend.demo.dto.LibroDTO;
import com.backend.demo.dto.LibroFormDTO;
import com.backend.demo.seguridad.Rol;
import com.backend.demo.seguridad.RolesPermitidos;
import com.backend.demo.service.LibroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/libros")
@CrossOrigin(origins = "http://localhost:4200")
public class LibroController {

    private final LibroService libroService;

    @Autowired
    public LibroController(LibroService libroService) {
        this.libroService = libroService;
    }

    @GetMapping
    public List<LibroDTO> listarTodos() {
        return libroService.listar();
    }

    @GetMapping("/autor/{nombre}")
    public List<LibroDTO> buscarPorAutor(@PathVariable String nombre) {
        return libroService.buscarPorAutor(nombre);
    }

    @GetMapping("/buscar")
    public List<LibroDTO> buscarPorTitulo(@RequestParam(name = "q", defaultValue = "") String query) {
        return libroService.buscarPorTitulo(query);
    }

    @GetMapping("/{id}")
    public LibroDTO obtenerPorId(@PathVariable Integer id) {
        return libroService.obtener(id);
    }

    @PostMapping
    @RolesPermitidos(Rol.ADMINISTRADOR)
    public ResponseEntity<LibroDTO> guardar(@RequestBody LibroFormDTO form) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libroService.crear(form));
    }

    @PutMapping("/{id}")
    @RolesPermitidos(Rol.ADMINISTRADOR)
    public LibroDTO actualizar(@PathVariable Integer id, @RequestBody LibroFormDTO form) {
        return libroService.actualizar(id, form);
    }

    /** Eliminación lógica: el libro queda con estado 'baja'. */
    @DeleteMapping("/{id}")
    @RolesPermitidos(Rol.ADMINISTRADOR)
    public ResponseEntity<Map<String, String>> darDeBaja(@PathVariable Integer id) {
        libroService.darDeBaja(id);
        return ResponseEntity.ok(Map.of("mensaje", "Libro dado de baja correctamente"));
    }
}
