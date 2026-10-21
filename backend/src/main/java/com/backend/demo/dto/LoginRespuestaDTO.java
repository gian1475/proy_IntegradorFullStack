package com.backend.demo.dto;

import java.time.LocalDateTime;

public class LoginRespuestaDTO {
    private String token;
    private LocalDateTime expiraEn;
    private UsuarioDTO usuario;

    public LoginRespuestaDTO() {}

    public LoginRespuestaDTO(String token, LocalDateTime expiraEn, UsuarioDTO usuario) {
        this.token = token;
        this.expiraEn = expiraEn;
        this.usuario = usuario;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public LocalDateTime getExpiraEn() { return expiraEn; }
    public void setExpiraEn(LocalDateTime expiraEn) { this.expiraEn = expiraEn; }

    public UsuarioDTO getUsuario() { return usuario; }
    public void setUsuario(UsuarioDTO usuario) { this.usuario = usuario; }
}
