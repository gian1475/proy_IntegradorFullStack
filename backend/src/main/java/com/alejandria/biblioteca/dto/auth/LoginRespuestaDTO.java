package com.alejandria.biblioteca.dto.auth;

import com.alejandria.biblioteca.dto.usuario.UsuarioDTO;

import java.time.LocalDateTime;

public record LoginRespuestaDTO(String token, LocalDateTime expiraEn, UsuarioDTO usuario) {}
