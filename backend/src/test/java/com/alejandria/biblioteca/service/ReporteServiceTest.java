package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.CategoriaDao;
import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dto.reporte.ImportacionResultadoDTO;
import com.alejandria.biblioteca.dto.libro.LibroFormDTO;
import com.alejandria.biblioteca.dto.reporte.ReportePrestamoDTO;
import com.alejandria.biblioteca.entity.Categoria;
import com.alejandria.biblioteca.exception.NegocioException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Reportes: Excel (POI), PDF (OpenPDF) y CSV (Commons CSV)")
@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock private PrestamoDao prestamoDao;
    @Mock private CategoriaDao categoriaDao;
    @Mock private LibroService libroService;

    private ReporteService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ReporteService(prestamoDao, categoriaDao, libroService);
    }

    @Test
    void elExcelTieneEncabezadoYUnaFilaPorPrestamo() throws Exception {
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 28, 9, 0);
        when(prestamoDao.listarParaReporte()).thenReturn(List.of(
            new ReportePrestamoDTO(1, "Juan Estudiante", "33333333", "Carlos Librero", "Clean Code",
                    inicio, inicio.plusHours(3), null, false, null, "activo")
        ));

        byte[] excel = servicio.generarExcelPrestamos();

        try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet hoja = libro.getSheet("Préstamos");
            assertNotNull(hoja);
            assertEquals("Prestatario", hoja.getRow(3).getCell(1).getStringCellValue());
            assertEquals("Juan Estudiante", hoja.getRow(4).getCell(1).getStringCellValue());
            assertEquals("28/10/2026 12:00", hoja.getRow(4).getCell(6).getStringCellValue());
        }
    }

    @Test
    void elPdfSeGeneraCorrectamente() {
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 28, 9, 0);
        when(prestamoDao.listarParaReporte()).thenReturn(List.of(
            new ReportePrestamoDTO(1, "Juan Estudiante", "33333333", "Carlos Librero", "Cien Años de Soledad",
                    inicio, inicio.plusHours(3), null, false, null, "activo")
        ));

        byte[] pdf = servicio.generarPdfPrestamos();

        // Todo archivo PDF empieza con "%PDF"
        assertTrue(pdf.length > 0);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void elCsvDePrestamosTieneEncabezadoYDatos() {
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 28, 9, 0);
        when(prestamoDao.listarParaReporte()).thenReturn(List.of(
            new ReportePrestamoDTO(1, "Juan Estudiante", "33333333", "Carlos Librero", "Clean Code",
                    inicio, inicio.plusHours(3), inicio.plusHours(2), true, "bueno", "devuelto")
        ));

        String csv = new String(servicio.generarCsvPrestamos(), StandardCharsets.UTF_8);

        assertTrue(csv.contains("Prestatario,DNI,Encargado"));
        assertTrue(csv.contains("Juan Estudiante,33333333,Carlos Librero,Clean Code"));
    }

    @Test
    void importarLibrosCreaLasFilasValidasYReportaLasInvalidas() {
        when(categoriaDao.findByNombreIgnoreCase("Tecnología"))
                .thenReturn(Optional.of(new Categoria(4, "Tecnología", null)));
        when(categoriaDao.findByNombreIgnoreCase("Cocina")).thenReturn(Optional.empty());

        String csv = """
                titulo,isbn,anio_publicacion,editorial,stock,categoria
                Refactoring,9780134757599,2018,Addison-Wesley,2,Tecnología
                Libro de cocina,1111111111111,2020,Editorial X,1,Cocina
                Otro libro,2222222222222,dos mil,Editorial Y,1,Tecnología
                """;

        ImportacionResultadoDTO resultado = servicio.importarLibrosCsv(csv.getBytes(StandardCharsets.UTF_8));

        assertEquals(1, resultado.creados());
        assertEquals(2, resultado.errores().size());
        assertTrue(resultado.errores().get(0).contains("Fila 3"));
        assertTrue(resultado.errores().get(1).contains("Fila 4"));

        ArgumentCaptor<LibroFormDTO> capturado = ArgumentCaptor.forClass(LibroFormDTO.class);
        verify(libroService, times(1)).crear(capturado.capture());
        assertEquals("Refactoring", capturado.getValue().getTitulo());
        assertEquals(4, capturado.getValue().getIdCategoria());
    }

    @Test
    void importarAceptaPuntoYComaComoSeparador() {
        when(categoriaDao.findByNombreIgnoreCase("Ciencia"))
                .thenReturn(Optional.of(new Categoria(2, "Ciencia", null)));

        String csv = "titulo;isbn;anio_publicacion;editorial;stock;categoria\n"
                   + "Cosmos;9780345539434;1980;Random House;1;Ciencia\n";

        assertEquals(1, servicio.importarLibrosCsv(csv.getBytes(StandardCharsets.UTF_8)).creados());
    }

    @Test
    void importarRechazaArchivoSinLasColumnas() {
        String csv = "nombre,precio\nAlgo,10\n";
        assertThrows(NegocioException.class,
                () -> servicio.importarLibrosCsv(csv.getBytes(StandardCharsets.UTF_8)));
    }
}
