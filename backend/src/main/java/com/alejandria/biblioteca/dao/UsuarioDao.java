package com.alejandria.biblioteca.dao;

import com.alejandria.biblioteca.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// contras en bd: en el codigo no hay ninguna contraseña escrita. estan en la tabla usuario
// (columna contrasena) y guardadas como hash bcrypt
public interface UsuarioDao extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByDni(String dni);

    // login: busca al usuario por su correo
    Optional<Usuario> findByCorreo(String correo);

    /** Reporte de bajas lógicas: usuarios con estado 'inactivo'. */
    List<Usuario> findByEstadoOrderByApellidoAscNombreAsc(String estado);

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

    // perfil unico: en cuantas tablas de perfil esta el usuario. tiene q ser exactamente 1
    @Query(value = "SELECT (SELECT COUNT(*) FROM administrador WHERE id_usuario = :idUsuario) "
                 + "     + (SELECT COUNT(*) FROM encargado WHERE id_usuario = :idUsuario) "
                 + "     + (SELECT COUNT(*) FROM prestatario WHERE id_usuario = :idUsuario)", nativeQuery = true)
    long contarPerfiles(@Param("idUsuario") Integer idUsuario);

    /** Encargados con su turno (JOIN usuario + encargado). Cada fila: [id, nombre, apellido, dni, correo, estado, turno]. */
    @Query(value = "SELECT u.id_usuario, u.nombre, u.apellido, u.dni, u.correo, u.estado, e.turno "
                 + "FROM usuario u JOIN encargado e ON e.id_usuario = u.id_usuario "
                 + "ORDER BY u.apellido, u.nombre", nativeQuery = true)
    List<Object[]> listarEncargados();

    // crear perfil 2: mete al usuario nuevo en la tabla encargado con su turno (RF09)
    @Modifying
    @Query(value = "INSERT INTO encargado (id_usuario, turno) VALUES (:idUsuario, :turno)", nativeQuery = true)
    void registrarComoEncargado(@Param("idUsuario") Integer idUsuario, @Param("turno") String turno);

    /** Dashboard: cuántos prestatarios están registrados. */
    @Query(value = "SELECT COUNT(*) FROM prestatario", nativeQuery = true)
    long contarPrestatarios();

    // crear perfil 3: mete al usuario nuevo en la tabla prestatario
    @Modifying
    @Query(value = "INSERT INTO prestatario (id_usuario, tiempos_suspendido) VALUES (:idUsuario, 0) ON CONFLICT DO NOTHING", nativeQuery = true)
    void registrarComoPrestatario(@Param("idUsuario") Integer idUsuario);
}
