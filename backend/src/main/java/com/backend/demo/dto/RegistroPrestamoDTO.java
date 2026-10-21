package com.backend.demo.dto;

/** Datos que envía el Encargado para registrar un préstamo en ventanilla. */
public class RegistroPrestamoDTO {
    private String dniPrestatario;
    private Integer idLibro;

    public RegistroPrestamoDTO() {}

    public String getDniPrestatario() { return dniPrestatario; }
    public void setDniPrestatario(String dniPrestatario) { this.dniPrestatario = dniPrestatario; }

    public Integer getIdLibro() { return idLibro; }
    public void setIdLibro(Integer idLibro) { this.idLibro = idLibro; }
}
