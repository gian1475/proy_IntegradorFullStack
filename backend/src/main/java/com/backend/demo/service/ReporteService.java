package com.backend.demo.service;

import com.backend.demo.dao.CategoriaDao;
import com.backend.demo.dao.PrestamoDao;
import com.backend.demo.dto.ImportacionResultadoDTO;
import com.backend.demo.dto.LibroFormDTO;
import com.backend.demo.dto.ReportePrestamoDTO;
import com.backend.demo.exception.NegocioException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Reportes y archivos:
 * - Apache POI: reporte de préstamos en Excel (.xlsx).
 * - OpenPDF: reporte de préstamos en PDF.
 * - Apache Commons CSV: exportar préstamos a CSV e importar libros desde CSV.
 */
@Service
public class ReporteService {

    private static final Logger log = LoggerFactory.getLogger(ReporteService.class);

    private static final String[] COLUMNAS_PRESTAMOS = {
        "N°", "Prestatario", "DNI", "Encargado", "Libro", "Inicio", "Hora límite",
        "Devolución", "Ampliado", "Estado físico", "Estado"
    };
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color COLOR_MARRON = new Color(133, 77, 14); // mismo color del sistema (#854d0e)

    /** Columnas que debe tener el CSV de libros (la primera fila del archivo). */
    public static final String[] COLUMNAS_CSV_LIBROS = {
        "titulo", "isbn", "anio_publicacion", "editorial", "stock", "categoria"
    };

    private final PrestamoDao prestamoDao;
    private final CategoriaDao categoriaDao;
    private final LibroService libroService;

    public ReporteService(PrestamoDao prestamoDao, CategoriaDao categoriaDao, LibroService libroService) {
        this.prestamoDao = prestamoDao;
        this.categoriaDao = categoriaDao;
        this.libroService = libroService;
    }

    // ---------- EXCEL con Apache POI ----------

    public byte[] generarExcelPrestamos() {
        List<ReportePrestamoDTO> filas = prestamoDao.listarParaReporte();

        try (Workbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Préstamos");

            // Estilo del encabezado: negrita, fondo de color
            Font negrita = libro.createFont();
            negrita.setBold(true);
            negrita.setColor(IndexedColors.WHITE.getIndex());
            CellStyle estiloEncabezado = libro.createCellStyle();
            estiloEncabezado.setFont(negrita);
            estiloEncabezado.setFillForegroundColor(IndexedColors.BROWN.getIndex());
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Título y fecha de generación
            Row titulo = hoja.createRow(0);
            titulo.createCell(0).setCellValue("Biblioteca de Alejandría - Reporte de préstamos");
            Row generado = hoja.createRow(1);
            generado.createCell(0).setCellValue("Generado: " + LocalDateTime.now().format(FORMATO_FECHA)
                    + "  |  Total: " + filas.size());

            // Encabezado de la tabla (fila 3)
            Row encabezado = hoja.createRow(3);
            for (int i = 0; i < COLUMNAS_PRESTAMOS.length; i++) {
                Cell celda = encabezado.createCell(i);
                celda.setCellValue(COLUMNAS_PRESTAMOS[i]);
                celda.setCellStyle(estiloEncabezado);
            }

            // Datos
            int numFila = 4;
            for (ReportePrestamoDTO p : filas) {
                Row fila = hoja.createRow(numFila++);
                String[] valores = valoresDeFila(p);
                fila.createCell(0).setCellValue(p.idPrestamo());
                for (int i = 1; i < valores.length; i++) {
                    fila.createCell(i).setCellValue(valores[i]);
                }
            }

            for (int i = 0; i < COLUMNAS_PRESTAMOS.length; i++) {
                hoja.autoSizeColumn(i);
            }
            hoja.createFreezePane(0, 4); // el encabezado queda fijo al bajar

            libro.write(salida);
            log.info("Reporte Excel de préstamos generado ({} filas)", filas.size());
            return salida.toByteArray();
        } catch (IOException e) {
            throw new NegocioException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el Excel");
        }
    }

    // ---------- PDF con OpenPDF ----------

    public byte[] generarPdfPrestamos() {
        List<ReportePrestamoDTO> filas = prestamoDao.listarParaReporte();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        // Hoja A4 horizontal, porque la tabla tiene 11 columnas
        Document documento = new Document(PageSize.A4.rotate(), 25, 25, 30, 30);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            com.lowagie.text.Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, COLOR_MARRON);
            com.lowagie.text.Font fuenteNormal = FontFactory.getFont(FontFactory.HELVETICA, 9);
            com.lowagie.text.Font fuenteEncabezado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            com.lowagie.text.Font fuenteCelda = FontFactory.getFont(FontFactory.HELVETICA, 8);

            documento.add(new Paragraph("Biblioteca de Alejandría - Reporte de préstamos", fuenteTitulo));
            documento.add(new Paragraph("Generado: " + LocalDateTime.now().format(FORMATO_FECHA)
                    + "   |   Total de préstamos: " + filas.size(), fuenteNormal));
            documento.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(COLUMNAS_PRESTAMOS.length);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[] {3, 12, 7, 11, 14, 9, 9, 9, 5, 7, 6});
            tabla.setHeaderRows(1); // el encabezado se repite en cada página

            for (String columna : COLUMNAS_PRESTAMOS) {
                PdfPCell celda = new PdfPCell(new Phrase(columna, fuenteEncabezado));
                celda.setBackgroundColor(COLOR_MARRON);
                celda.setPadding(4);
                tabla.addCell(celda);
            }
            for (ReportePrestamoDTO p : filas) {
                for (String valor : valoresDeFila(p)) {
                    PdfPCell celda = new PdfPCell(new Phrase(valor, fuenteCelda));
                    celda.setPadding(3);
                    tabla.addCell(celda);
                }
            }

            documento.add(tabla);
        } catch (DocumentException e) {
            throw new NegocioException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el PDF");
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }

        log.info("Reporte PDF de préstamos generado ({} filas)", filas.size());
        return salida.toByteArray();
    }

    // ---------- CSV con Apache Commons CSV ----------

    public byte[] generarCsvPrestamos() {
        List<ReportePrestamoDTO> filas = prestamoDao.listarParaReporte();
        CSVFormat formato = CSVFormat.DEFAULT.builder().setHeader(COLUMNAS_PRESTAMOS).get();

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (OutputStreamWriter escritor = new OutputStreamWriter(salida, StandardCharsets.UTF_8);
             CSVPrinter csv = new CSVPrinter(escritor, formato)) {
            escritor.write('﻿'); // BOM: para que Excel muestre bien las tildes
            for (ReportePrestamoDTO p : filas) {
                csv.printRecord((Object[]) valoresDeFila(p));
            }
        } catch (IOException e) {
            throw new NegocioException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el CSV");
        }
        log.info("Reporte CSV de préstamos generado ({} filas)", filas.size());
        return salida.toByteArray();
    }

    /**
     * Importa libros desde un CSV. Cada fila se guarda por separado:
     * si una falla (ISBN repetido, categoría que no existe...), las demás igual se registran.
     * Acepta coma o punto y coma como separador (Excel en español guarda con ";").
     */
    public ImportacionResultadoDTO importarLibrosCsv(byte[] archivo) {
        String contenido = new String(archivo, StandardCharsets.UTF_8).replace("﻿", "");
        if (contenido.isBlank()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "El archivo está vacío");
        }
        String primeraLinea = contenido.lines().findFirst().orElse("");
        char separador = primeraLinea.contains(";") && !primeraLinea.contains(",") ? ';' : ',';

        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setDelimiter(separador)
                .setHeader()                 // los nombres de columna salen de la primera fila
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .get();

        int creados = 0;
        List<String> errores = new ArrayList<>();

        try (CSVParser parser = CSVParser.parse(new StringReader(contenido), formato)) {
            for (String columna : COLUMNAS_CSV_LIBROS) {
                if (!parser.getHeaderMap().containsKey(columna)) {
                    throw new NegocioException(HttpStatus.BAD_REQUEST,
                            "Falta la columna '" + columna + "'. Columnas esperadas: " + String.join(",", COLUMNAS_CSV_LIBROS));
                }
            }

            for (CSVRecord registro : parser) {
                long linea = registro.getRecordNumber() + 1; // +1 por la fila de encabezados
                try {
                    libroService.crear(aFormulario(registro));
                    creados++;
                } catch (NegocioException e) {
                    errores.add("Fila " + linea + ": " + e.getMessage());
                } catch (NumberFormatException e) {
                    errores.add("Fila " + linea + ": año o stock no es un número");
                } catch (IllegalArgumentException e) {
                    errores.add("Fila " + linea + ": faltan columnas");
                }
            }
        } catch (IOException e) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "No se pudo leer el CSV");
        }

        log.info("Importación CSV de libros: {} creados, {} con error", creados, errores.size());
        return new ImportacionResultadoDTO(creados, errores);
    }

    // ---------- Auxiliares ----------

    private LibroFormDTO aFormulario(CSVRecord registro) {
        String nombreCategoria = registro.get("categoria");
        Integer idCategoria = categoriaDao.findByNombreIgnoreCase(nombreCategoria)
                .orElseThrow(() -> new NegocioException(HttpStatus.BAD_REQUEST, "La categoría '" + nombreCategoria + "' no existe"))
                .getIdCategoria();

        LibroFormDTO form = new LibroFormDTO();
        form.setTitulo(registro.get("titulo"));
        form.setIsbn(registro.get("isbn"));
        form.setAnioPublicacion(Integer.parseInt(registro.get("anio_publicacion")));
        form.setEditorial(registro.get("editorial"));
        form.setStock(Integer.parseInt(registro.get("stock")));
        form.setIdCategoria(idCategoria);
        return form;
    }

    private String[] valoresDeFila(ReportePrestamoDTO p) {
        return new String[] {
            String.valueOf(p.idPrestamo()),
            p.prestatario(),
            p.dniPrestatario(),
            p.encargado(),
            p.tituloLibro(),
            formatear(p.fechaPrestamo()),
            formatear(p.fechaDevolucionPrevista()),
            formatear(p.fechaDevolucionReal()),
            Boolean.TRUE.equals(p.ampliado()) ? "Sí" : "No",
            p.estadoFisicoDevolucion() == null ? "" : p.estadoFisicoDevolucion(),
            p.estadoPrestamo()
        };
    }

    private String formatear(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FORMATO_FECHA);
    }
}
