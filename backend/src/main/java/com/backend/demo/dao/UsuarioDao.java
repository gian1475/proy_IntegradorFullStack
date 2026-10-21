package com.backend.demo.dao;

import com.backend.demo.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioDao extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByDni(String dni);

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByDni(String dni);

    boolean existsByCorreo(String correo);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM administrador WHERE id_usuario = :idUsuario)", nativeQuery = true)
    boolean esAdministrador(@Param("idUsuario") Integer idUsuario);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM encargado WHERE id_usuario = :idUsuario)", nativeQuery = true)
    boolean esEncargado(@Param("idUsuario") Integer idUsuario);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM prestatario WHERE id_usuario = :idUsuario)", nativeQuery = true)
    boolean esPrestatario(@Param("idUsuario") Integer idUsuario);

    @Modifying
    @Query(value = "INSERT INTO prestatario (id_usuario, tiempos_suspendido) VALUES (:idUsuario, 0) ON CONFLICT DO NOTHING", nativeQuery = true)
    void registrarComoPrestatario(@Param("idUsuario") Integer idUsuario);
}
