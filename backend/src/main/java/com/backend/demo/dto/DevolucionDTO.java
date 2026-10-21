package com.backend.demo.dto;

/** Estado físico del libro al devolverlo: "bueno" o "dañado". */
public class DevolucionDTO {
    private String estadoFisico;

    public DevolucionDTO() {}

    public String getEstadoFisico() { return estadoFisico; }
    public void setEstadoFisico(String estadoFisico) { this.estadoFisico = estadoFisico; }
}
