package com.backend.demo.dto;

public class ReservaDTO {
    private Integer idLibro;
    private Integer idUsuario;

    public ReservaDTO() {}

    public ReservaDTO(Integer idLibro, Integer idUsuario) {
        this.idLibro = idLibro;
        this.idUsuario = idUsuario;
    }

    public Integer getIdLibro() { return idLibro; }
    public void setIdLibro(Integer idLibro) { this.idLibro = idLibro; }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
}
