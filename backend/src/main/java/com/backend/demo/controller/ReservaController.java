package com.backend.demo.controller;

import com.backend.demo.dao.LibroDao;
import com.backend.demo.dao.ReservaDao;
import com.backend.demo.dao.UsuarioDao;
import com.backend.demo.dto.ReservaDTO;
import com.backend.demo.entity.Libro;
import com.backend.demo.entity.Reserva;
import com.backend.demo.entity.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reservas")
@CrossOrigin(origins = "*")
public class ReservaController {

    private final ReservaDao reservaDao;
    private final LibroDao libroDao;
    private final UsuarioDao usuarioDao;

    @Autowired
    public ReservaController(ReservaDao reservaDao, LibroDao libroDao, UsuarioDao usuarioDao) {
        this.reservaDao = reservaDao;
        this.libroDao = libroDao;
        this.usuarioDao = usuarioDao;
    }

    @GetMapping
    public List<Reserva> listarTodas() {
        return reservaDao.findAll();
    }

    @GetMapping("/usuario/{idUsuario}")
    public List<Reserva> listarPorUsuario(@PathVariable Integer idUsuario) {
        return reservaDao.findByPrestatarioIdUsuario(idUsuario);
    }

    @PostMapping
    public ResponseEntity<?> crearReserva(@RequestBody ReservaDTO dto) {
        if (dto.getIdLibro() == null || dto.getIdUsuario() == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Datos incompletos para procesar la reserva"));
        }

        Optional<Libro> libroOpt = libroDao.findById(dto.getIdLibro());
        Optional<Usuario> usuarioOpt = usuarioDao.findById(dto.getIdUsuario());

        if (libroOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", "Libro no encontrado"));
        }

        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", "Usuario no encontrado"));
        }

        Libro libro = libroOpt.get();
        Usuario usuario = usuarioOpt.get();

        Reserva reserva = new Reserva();
        reserva.setLibro(libro);
        reserva.setPrestatario(usuario);
        reserva.setFechaReserva(LocalDateTime.now());
        reserva.setFechaRecojoLimite(LocalDateTime.now().plusDays(2));
        reserva.setEstadoReserva("pendiente");
        reserva.setPosicionCola(1);

        Reserva guardada = reservaDao.save(reserva);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelarReserva(@PathVariable Integer id) {
        return reservaDao.findById(id).map(r -> {
            r.setEstadoReserva("cancelada");
            reservaDao.save(r);
            return ResponseEntity.ok(Map.of("mensaje", "Reserva cancelada correctamente"));
        }).orElse(ResponseEntity.notFound().build());
    }
}
