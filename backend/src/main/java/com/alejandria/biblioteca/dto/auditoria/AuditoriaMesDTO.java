package com.alejandria.biblioteca.dto.auditoria;

import java.util.List;

/** IPI del mes (null si no hay auditorías), cuántos títulos se auditaron y coinciden, y las auditorías. */
public record AuditoriaMesDTO(
        Double ipi,
        long auditados,
        long coinciden,
        List<AuditoriaDTO> auditorias
) {}
