package com.backend.demo.dao;

import com.backend.demo.entity.Autor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AutorDao extends JpaRepository<Autor, Integer> {
    List<Autor> findByApellidoContainingIgnoreCase(String apellido);
    List<Autor> findByNacionalidadIgnoreCase(String nacionalidad);
}
