package com.alejandria.biblioteca.dto.usuario;

/** Un encargado con su turno, para la tabla del panel del admin (sin la contraseña). */
public record EncargadoDTO(
        Integer idUsuario,
        String nombre,
        String apellido,
        String dni,
        String correo,
        String estado,
        String turno
) {}
