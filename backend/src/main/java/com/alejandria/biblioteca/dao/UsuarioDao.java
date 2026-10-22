package com.alejandria.biblioteca.dao;

import com.alejandria.biblioteca.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// contras en bd: en el codigo no hay ninguna contraseña escrita. estan en la tabla usuario
// (columna contrasena) y guardadas como hash bcrypt
public interface UsuarioDao extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByDni(String dni);

    // login: busca al usuario por su correo
    Optional<Usuario> findByCorreo(String correo);

    // no se repite dni ni correo al registrarse
    boolean existsByDni(String dni);

    boolean existsByCorreo(String correo);

    // perfil 1: el usuario esta en la tabla administrador?
    @Query(value = "SELECT EXISTS (SELECT 1 FROM administrador WHERE id_usuario = :idUsuario)", nativeQuery = true)
    boolean esAdministrador(@Param("idUsuario") Integer idUsuario);

    // perfil 2: el usuario esta en la tabla encargado?
    @Query(value = "SELECT EXISTS (SELECT 1 FROM encargado WHERE id_usuario = :idUsuario)", nativeQuery = true)
    boolean esEncargado(@Param("idUsuario") Integer idUsuario);

    // perfil 3: el usuario esta en la tabla prestatario?
    @Query(value = "SELECT EXISTS (SELECT 1 FROM prestatario WHERE id_usuario = :idUsuario)", nativeQuery = true)
    boolean esPrestatario(@Param("idUsuario") Integer idUsuario);

    /** Dashboard: cuántos prestatarios están registrados. */
    @Query(value = "SELECT COUNT(*) FROM prestatario", nativeQuery = true)
    long contarPrestatarios();

    // crear perfil 3: mete al usuario nuevo en la tabla prestatario
    @Modifying
    @Query(value = "INSERT INTO prestatario (id_usuario, tiempos_suspendido) VALUES (:idUsuario, 0) ON CONFLICT DO NOTHING", nativeQuery = true)
    void registrarComoPrestatario(@Param("idUsuario") Integer idUsuario);
}
