package com.alejandria.biblioteca.dao;

import com.alejandria.biblioteca.entity.Autor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutorDao extends JpaRepository<Autor, Integer> {
}
