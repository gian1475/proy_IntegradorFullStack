package com.backend.demo.dao;

import com.backend.demo.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservaDao extends JpaRepository<Reserva, Integer> {
    List<Reserva> findByPrestatarioIdUsuario(Integer idUsuario);
    List<Reserva> findByEstadoReserva(String estadoReserva);
}
