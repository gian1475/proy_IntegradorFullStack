package com.alejandria.biblioteca.controller;

import com.alejandria.biblioteca.dto.reporte.ImportacionResultadoDTO;
import com.alejandria.biblioteca.exception.NegocioException;
import com.alejandria.biblioteca.seguridad.Rol;
import com.alejandria.biblioteca.service.ReporteService;
import com.alejandria.biblioteca.service.ReservaService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;
    private final ReservaService reservaService;

    public ReporteController(ReporteService reporteService, ReservaService reservaService) {
        this.reporteService = reporteService;
        this.reservaService = reservaService;
    }

    /** Descarga un reporte. tipo: prestamos, devoluciones, reservas, incidencias, bajas, stock, indicadores, tiempos o pordia. */
    // reportes rf08: GET /api/reportes/{tipo}/{formato}?desde=AAAA-MM-DD&hasta=AAAA-MM-DD
    // permisos admin y permisos encargado: los dos pueden descargar reportes (RF08, CU11)
    @GetMapping("/{tipo}/{formato}")
    @RolesAllowed({Rol.ADMINISTRADOR, Rol.ENCARGADO})
    public ResponseEntity<byte[]> descargar(@PathVariable String tipo, @PathVariable String formato,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        // D26: las reservas vencidas liberan su ejemplar antes de armar el reporte (el de stock es una foto de ahora).
        // va aqui porque generar() es de solo lectura (@Transactional(readOnly = true))
        reservaService.liberarVencidas();
        byte[] contenido = reporteService.generar(tipo, formato, desde, hasta);
        String nombre = "reporte_" + tipo + "_" + LocalDate.now();
        // D30: la misma respuesta de descarga que el comprobante y la constancia (Descarga.archivo)
        return switch (formato) {
            case "excel" -> Descarga.archivo(contenido, nombre + ".xlsx",
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            case "pdf" -> Descarga.archivo(contenido, nombre + ".pdf", MediaType.APPLICATION_PDF);
            default -> Descarga.archivo(contenido, nombre + ".csv", MediaType.parseMediaType("text/csv; charset=UTF-8"));
        };
    }

    // permisos admin: importar libros desde un csv solo lo hace el admin.
    // D27: el campo del formulario se llama "archivo", igual que el parametro: no hace falta repetirlo en @RequestParam
    @PostMapping("/libros/csv")
    @RolesAllowed(Rol.ADMINISTRADOR)
    public ImportacionResultadoDTO importarLibros(@RequestParam MultipartFile archivo) {
        if (archivo.isEmpty()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Seleccione un archivo CSV");
        }
        try {
            return reporteService.importarLibrosCsv(archivo.getBytes());
        } catch (IOException e) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo");
        }
    }
}
