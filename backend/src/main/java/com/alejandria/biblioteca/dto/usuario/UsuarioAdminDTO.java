package com.alejandria.biblioteca.dto.usuario;

/**
 * Un prestatario o un encargado en la pestaña "Usuarios" del admin (RF12 y RF13), sin la contraseña.
 * perfil: ENCARGADO o PRESTATARIO (constantes de Rol). turno: solo el encargado lo tiene; el prestatario, null.
 */
public record UsuarioAdminDTO(
        Integer idUsuario,
        String nombre,
        String apellido,
        String dni,
        String correo,
        String estado,
        String perfil,
        String turno
) {}
