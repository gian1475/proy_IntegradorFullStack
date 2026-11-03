package com.alejandria.biblioteca.dto.usuario;

public record UsuarioDTO(
        Integer idUsuario,
        String nombre,
        String apellido,
        String dni,
        String correo,
        String estado,
        String rol
) {}
