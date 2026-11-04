package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.AuditoriaInventarioDao;
import com.alejandria.biblioteca.dao.LibroDao;
import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.auditoria.AuditoriaDTO;
import com.alejandria.biblioteca.dto.auditoria.AuditoriaMesDTO;
import com.alejandria.biblioteca.dto.auditoria.RegistroAuditoriaDTO;
import com.alejandria.biblioteca.entity.AuditoriaInventario;
import com.alejandria.biblioteca.entity.Libro;
import com.alejandria.biblioteca.exception.NegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * IPI (apartado 1.2.2): auditoría física del inventario. El administrador cuenta los ejemplares disponibles
 * de un título y el sistema guarda ese conteo junto con el stock que tenía en ese momento.
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final AuditoriaInventarioDao auditoriaDao;
    private final LibroDao libroDao;
    private final UsuarioDao usuarioDao;
    private final HorarioAtencion horario;

    public AuditoriaService(AuditoriaInventarioDao auditoriaDao, LibroDao libroDao, UsuarioDao usuarioDao,
                            HorarioAtencion horario) {
        this.auditoriaDao = auditoriaDao;
        this.libroDao = libroDao;
        this.usuarioDao = usuarioDao;
        this.horario = horario;
    }

    // auditoria inventario: se guarda el conteo fisico y el stock q decia el sistema en ese momento.
    // si no coinciden solo se registra (el IPI baja): el stock se corrige aparte con "Editar libro"
    @Transactional
    public AuditoriaDTO registrar(RegistroAuditoriaDTO dto, Integer idAdministrador) {
        Libro libro = libroDao.findById(dto.idLibro())
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Libro no encontrado"));
        if ("baja".equals(libro.getEstadoLibro())) {
            throw new NegocioException(HttpStatus.CONFLICT, "No se audita un libro dado de baja");
        }

        AuditoriaInventario auditoria = new AuditoriaInventario();
        auditoria.setLibro(libro);
        auditoria.setAdministrador(usuarioDao.getReferenceById(idAdministrador));
        auditoria.setFechaAuditoria(horario.ahora());
        auditoria.setStockSistema(libro.getStock());
        auditoria.setConteoFisico(dto.conteoFisico());
        AuditoriaInventario guardada = auditoriaDao.save(auditoria);

        log.info("Auditoría de '{}': sistema {}, conteo físico {} ({})", libro.getTitulo(), libro.getStock(),
                dto.conteoFisico(), guardada.coincide() ? "coincide" : "NO coincide");
        return AuditoriaDTO.desde(guardada);
    }

    /** El IPI del mes en curso y sus auditorías (la más reciente primero). */
    @Transactional(readOnly = true)
    public AuditoriaMesDTO resumenDelMes() {
        LocalDateTime inicioMes = horario.ahora().toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMes = inicioMes.plusMonths(1);
        long auditados = auditoriaDao.contarAuditadosEntre(inicioMes, finMes);
        long coinciden = auditoriaDao.contarCoincidentesEntre(inicioMes, finMes);
        return new AuditoriaMesDTO(
                IndicadoresGestion.porcentaje(coinciden, auditados),
                auditados,
                coinciden,
                auditoriaDao.listarEntre(inicioMes, finMes).stream().map(AuditoriaDTO::desde).toList());
    }
}
