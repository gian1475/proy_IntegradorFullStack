package com.alejandria.biblioteca.dao;

import com.alejandria.biblioteca.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoriaDao extends JpaRepository<Categoria, Integer> {

    Optional<Categoria> findByNombreIgnoreCase(String nombre);
}
