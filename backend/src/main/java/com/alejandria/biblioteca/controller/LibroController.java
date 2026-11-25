package com.alejandria.biblioteca.controller;

import com.alejandria.biblioteca.dto.libro.LibroDTO;
import com.alejandria.biblioteca.dto.libro.LibroFormDTO;
import com.alejandria.biblioteca.seguridad.Rol;
import com.alejandria.biblioteca.service.LibroService;
import com.alejandria.biblioteca.service.ReservaService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// ruta crud libro: /api/libros. los GET son publicos (cualquiera ve el catalogo),
// adicionar, actualizar y eliminar solo el admin (permisos admin).
// D26 (consulta disponibilidad RF03): antes de mostrar libros, las reservas cuya hora de recojo ya paso se vencen y
// devuelven su ejemplar (ReservaService.liberarVencidas). esta aqui y no en LibroService porque ReservaService ya usa
// LibroService (si LibroService usara ReservaService, cada uno necesitaria al otro: dependencia circular)
@RestController
@RequestMapping("/api/libros")
public class LibroController {

    private final LibroService libroService;
    private final ReservaService reservaService;

    public LibroController(LibroService libroService, ReservaService reservaService) {
        this.libroService = libroService;
        this.reservaService = reservaService;
    }

    // crud libro consultar: GET /api/libros
    @GetMapping
    public List<LibroDTO> listarTodos() {
        reservaService.liberarVencidas(); // D26: el stock y el estado que se muestran son los de ahora
        return libroService.listar();
    }

    @GetMapping("/autor/{nombre}")
    public List<LibroDTO> buscarPorAutor(@PathVariable String nombre) {
        reservaService.liberarVencidas();
        return libroService.buscarPorAutor(nombre);
    }

    // consulta disponibilidad (RF03): GET /api/libros/buscar?q=texto (titulo, autor, categoria o isbn)
    @GetMapping("/buscar")
    public List<LibroDTO> buscar(@RequestParam(name = "q", defaultValue = "") String query) {
        reservaService.liberarVencidas();
        return libroService.buscar(query);
    }

    @GetMapping("/{id}")
    public LibroDTO obtenerPorId(@PathVariable Integer id) {
        reservaService.liberarVencidas();
        return libroService.obtener(id);
    }

    // crud libro adicionar: POST /api/libros (solo admin)
    @PostMapping
    @RolesAllowed(Rol.ADMINISTRADOR)
    public ResponseEntity<LibroDTO> guardar(@Valid @RequestBody LibroFormDTO form) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libroService.crear(form));
    }

    // crud libro actualizar: PUT /api/libros/{id} (solo admin)
    @PutMapping("/{id}")
    @RolesAllowed(Rol.ADMINISTRADOR)
    public LibroDTO actualizar(@PathVariable Integer id, @Valid @RequestBody LibroFormDTO form) {
        return libroService.actualizar(id, form);
    }

    /** Eliminación lógica: el libro queda con estado 'baja'. */
    // crud libro eliminar: DELETE /api/libros/{id} (solo admin). es eliminacion logica, ver LibroService.darDeBaja
    @DeleteMapping("/{id}")
    @RolesAllowed(Rol.ADMINISTRADOR)
    public ResponseEntity<Map<String, String>> darDeBaja(@PathVariable Integer id) {
        libroService.darDeBaja(id);
        return ResponseEntity.ok(Map.of("mensaje", "Libro dado de baja correctamente"));
    }
}
