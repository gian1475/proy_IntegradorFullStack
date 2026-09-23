package com.backend.demo.dao;

import com.backend.demo.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioDao extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByDni(String dni);

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByDni(String dni);

    boolean existsByCorreo(String correo);
}
