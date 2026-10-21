package com.backend.demo.dto;

import java.time.LocalDateTime;

/** Datos para crear una reserva: libro, prestatario y hasta cuándo puede recogerlo. */
public class ReservaDTO {
    private Integer idLibro;
    private Integer idUsuario;
    private LocalDateTime fechaRecojoLimite;

    public ReservaDTO() {}

    public Integer getIdLibro() { return idLibro; }
    public void setIdLibro(Integer idLibro) { this.idLibro = idLibro; }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public LocalDateTime getFechaRecojoLimite() { return fechaRecojoLimite; }
    public void setFechaRecojoLimite(LocalDateTime fechaRecojoLimite) { this.fechaRecojoLimite = fechaRecojoLimite; }
}
