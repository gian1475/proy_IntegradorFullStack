package com.alejandria.biblioteca.controller;

import com.alejandria.biblioteca.dto.reporte.ImportacionResultadoDTO;
import com.alejandria.biblioteca.exception.NegocioException;
import com.alejandria.biblioteca.seguridad.Rol;
import com.alejandria.biblioteca.seguridad.RolesPermitidos;
import com.alejandria.biblioteca.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
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

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    /** Descarga un reporte. tipo: prestamos, devoluciones, reservas, incidencias, bajas, stock, indicadores o tiempos. */
    // reportes rf08: GET /api/reportes/{tipo}/{formato}?desde=AAAA-MM-DD&hasta=AAAA-MM-DD
    // permisos admin y permisos encargado: los dos pueden descargar reportes (RF08, CU11)
    @GetMapping("/{tipo}/{formato}")
    @RolesPermitidos({Rol.ADMINISTRADOR, Rol.ENCARGADO})
    public ResponseEntity<byte[]> descargar(@PathVariable String tipo, @PathVariable String formato,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        byte[] contenido = reporteService.generar(tipo, formato, desde, hasta);
        String nombre = "reporte_" + tipo + "_" + LocalDate.now();
        return switch (formato) {
            case "excel" -> archivo(contenido, nombre + ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "pdf" -> archivo(contenido, nombre + ".pdf", MediaType.APPLICATION_PDF_VALUE);
            default -> archivo(contenido, nombre + ".csv", "text/csv; charset=UTF-8");
        };
    }

    // permisos admin: importar libros desde un csv solo lo hace el admin
    @PostMapping("/libros/csv")
    @RolesPermitidos(Rol.ADMINISTRADOR)
    public ImportacionResultadoDTO importarLibros(@RequestParam("archivo") MultipartFile archivo) {
        if (archivo.isEmpty()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Seleccione un archivo CSV");
        }
        try {
            return reporteService.importarLibrosCsv(archivo.getBytes());
        } catch (IOException e) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo");
        }
    }

    private ResponseEntity<byte[]> archivo(byte[] contenido, String nombre, String tipo) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.parseMediaType(tipo))
                .body(contenido);
    }
}
