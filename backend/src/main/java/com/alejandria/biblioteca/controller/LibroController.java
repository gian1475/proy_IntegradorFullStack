package com.alejandria.biblioteca.controller;

import com.alejandria.biblioteca.dto.libro.LibroDTO;
import com.alejandria.biblioteca.dto.libro.LibroFormDTO;
import com.alejandria.biblioteca.seguridad.Rol;
import com.alejandria.biblioteca.seguridad.RolesPermitidos;
import com.alejandria.biblioteca.service.LibroService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// ruta crud libro: /api/libros. los GET son publicos (cualquiera ve el catalogo),
// adicionar, actualizar y eliminar solo el admin (permisos admin)
@RestController
@RequestMapping("/api/libros")
public class LibroController {

    private final LibroService libroService;

    public LibroController(LibroService libroService) {
        this.libroService = libroService;
    }

    // crud libro consultar: GET /api/libros
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

    // crud libro adicionar: POST /api/libros (solo admin)
    @PostMapping
    @RolesPermitidos(Rol.ADMINISTRADOR)
    public ResponseEntity<LibroDTO> guardar(@RequestBody LibroFormDTO form) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libroService.crear(form));
    }

    // crud libro actualizar: PUT /api/libros/{id} (solo admin)
    @PutMapping("/{id}")
    @RolesPermitidos(Rol.ADMINISTRADOR)
    public LibroDTO actualizar(@PathVariable Integer id, @RequestBody LibroFormDTO form) {
        return libroService.actualizar(id, form);
    }

    /** Eliminación lógica: el libro queda con estado 'baja'. */
    // crud libro eliminar: DELETE /api/libros/{id} (solo admin). es eliminacion logica, ver LibroService.darDeBaja
    @DeleteMapping("/{id}")
    @RolesPermitidos(Rol.ADMINISTRADOR)
    public ResponseEntity<Map<String, String>> darDeBaja(@PathVariable Integer id) {
        libroService.darDeBaja(id);
        return ResponseEntity.ok(Map.of("mensaje", "Libro dado de baja correctamente"));
    }
}
