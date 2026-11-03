package com.alejandria.biblioteca.dto.auditoria;

import com.alejandria.biblioteca.entity.AuditoriaInventario;

import java.time.LocalDateTime;

/** Una auditoría para la pestaña "Auditoría" del admin. */
public record AuditoriaDTO(
        Integer idAuditoria,
        Integer idLibro,
        String tituloLibro,
        String isbn,
        LocalDateTime fechaAuditoria,
        Integer stockSistema,
        Integer conteoFisico,
        boolean coincide,
        String administrador
) {
    public static AuditoriaDTO desde(AuditoriaInventario a) {
        return new AuditoriaDTO(a.getIdAuditoria(), a.getLibro().getIdLibro(), a.getLibro().getTitulo(),
                a.getLibro().getIsbn(), a.getFechaAuditoria(), a.getStockSistema(), a.getConteoFisico(), a.coincide(),
                a.getAdministrador().getNombre() + " " + a.getAdministrador().getApellido());
    }
}
