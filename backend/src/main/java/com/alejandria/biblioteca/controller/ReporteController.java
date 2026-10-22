package com.alejandria.biblioteca.controller;

import com.alejandria.biblioteca.dto.reporte.ImportacionResultadoDTO;
import com.alejandria.biblioteca.exception.NegocioException;
import com.alejandria.biblioteca.seguridad.Rol;
import com.alejandria.biblioteca.seguridad.RolesPermitidos;
import com.alejandria.biblioteca.service.ReporteService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

// permisos admin: los reportes (excel, pdf, csv) solo los saca el admin.
// la anotacion esta en la clase asi q aplica a todos los endpoints de aca
@RestController
@RequestMapping("/api/reportes")
@RolesPermitidos(Rol.ADMINISTRADOR)
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/prestamos/excel")
    public ResponseEntity<byte[]> prestamosExcel() {
        return archivo(reporteService.generarExcelPrestamos(),
                "reporte_prestamos_" + LocalDate.now() + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping("/prestamos/pdf")
    public ResponseEntity<byte[]> prestamosPdf() {
        return archivo(reporteService.generarPdfPrestamos(),
                "reporte_prestamos_" + LocalDate.now() + ".pdf",
                MediaType.APPLICATION_PDF_VALUE);
    }

    @GetMapping("/prestamos/csv")
    public ResponseEntity<byte[]> prestamosCsv() {
        return archivo(reporteService.generarCsvPrestamos(),
                "reporte_prestamos_" + LocalDate.now() + ".csv",
                "text/csv; charset=UTF-8");
    }

    @PostMapping("/libros/csv")
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
