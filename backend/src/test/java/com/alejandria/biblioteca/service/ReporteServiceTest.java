package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.CategoriaDao;
import com.alejandria.biblioteca.dao.LibroDao;
import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dao.ReservaDao;
import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.reporte.ImportacionResultadoDTO;
import com.alejandria.biblioteca.dto.libro.LibroFormDTO;
import com.alejandria.biblioteca.dto.reporte.ReportePrestamoDTO;
import com.alejandria.biblioteca.dto.reporte.TablaReporte;
import com.alejandria.biblioteca.entity.Categoria;
import com.alejandria.biblioteca.entity.Libro;
import com.alejandria.biblioteca.entity.Reserva;
import com.alejandria.biblioteca.entity.Usuario;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Reportes (RF08): Excel (POI), PDF (OpenPDF) y CSV (Commons CSV)")
@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock private PrestamoDao prestamoDao;
    @Mock private ReservaDao reservaDao;
    @Mock private LibroDao libroDao;
    @Mock private UsuarioDao usuarioDao;
    @Mock private CategoriaDao categoriaDao;
    @Mock private LibroService libroService;

    private ReporteService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ReporteService(prestamoDao, reservaDao, libroDao, usuarioDao, categoriaDao, libroService);
    }

    @Test
    void elExcelTieneEncabezadoYUnaFilaPorPrestamo() throws Exception {
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 28, 9, 0);
        when(prestamoDao.listarParaReporte(any(), any())).thenReturn(List.of(
            new ReportePrestamoDTO(1, "Juan Estudiante", "33333333", "Carlos Librero", "Clean Code",
                    inicio, inicio.plusHours(3), null, false, null, "activo")
        ));

        byte[] excel = servicio.generar("prestamos", "excel", null, null);

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
        when(prestamoDao.listarParaReporte(any(), any())).thenReturn(List.of(
            new ReportePrestamoDTO(1, "Juan Estudiante", "33333333", "Carlos Librero", "Cien Años de Soledad",
                    inicio, inicio.plusHours(3), null, false, null, "activo")
        ));

        byte[] pdf = servicio.generar("prestamos", "pdf", null, null);

        // Todo archivo PDF empieza con "%PDF"
        assertTrue(pdf.length > 0);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void elCsvDePrestamosTieneEncabezadoYDatos() {
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 28, 9, 0);
        when(prestamoDao.listarParaReporte(any(), any())).thenReturn(List.of(
            new ReportePrestamoDTO(1, "Juan Estudiante", "33333333", "Carlos Librero", "Clean Code",
                    inicio, inicio.plusHours(3), inicio.plusHours(2), true, "bueno", "devuelto")
        ));

        String csv = new String(servicio.generar("prestamos", "csv", null, null), StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("\uFEFFN°,Prestatario")); // el BOM va primero, antes del encabezado
        assertTrue(csv.contains("Prestatario,DNI,Encargado"));
        assertTrue(csv.contains("Juan Estudiante,33333333,Carlos Librero,Clean Code"));
    }

    // ---------- Reportes restantes (RF08) ----------

    @Test
    void elRangoDeFechasIncluyeLosDiasCompletos() {
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 27);

        TablaReporte tabla = servicio.armar("prestamos", desde, hasta);

        // "hasta el 27/10" incluye todo el 27: se consulta hasta el 28/10 a las 00:00
        verify(prestamoDao).listarParaReporte(desde.atStartOfDay(), LocalDate.of(2026, 10, 28).atStartOfDay());
        assertEquals("Del 01/10/2026 al 27/10/2026", tabla.periodo());
    }

    @Test
    void rechazaFechasAlRevesTiposYFormatosQueNoExisten() {
        assertEquals(400, assertThrows(NegocioException.class,
                () -> servicio.armar("prestamos", LocalDate.of(2026, 10, 27), LocalDate.of(2026, 10, 1))).getEstado().value());
        assertEquals(400, assertThrows(NegocioException.class,
                () -> servicio.armar("ventas", null, null)).getEstado().value());
        assertEquals(400, assertThrows(NegocioException.class,
                () -> servicio.generar("prestamos", "word", null, null)).getEstado().value());
    }

    @Test
    void lasDevolucionesDicenSiFueronATiempoOConRetraso() {
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 28, 9, 0);
        when(prestamoDao.listarDevoluciones(any(), any())).thenReturn(List.of(
            new ReportePrestamoDTO(1, "Juan Estudiante", "33333333", "Carlos Librero", "Clean Code",
                    inicio, inicio.plusHours(3), inicio.plusHours(2), false, "bueno", "devuelto"),
            new ReportePrestamoDTO(2, "Ana Lectora", "44444444", "Carlos Librero", "Rayuela",
                    inicio, inicio.plusHours(3), inicio.plusHours(3).plusMinutes(12), false, "dañado", "devuelto")
        ));

        TablaReporte tabla = servicio.armar("devoluciones", null, null);

        assertEquals("A tiempo", tabla.filas().get(0)[7]);
        assertEquals("Con retraso (12 min)", tabla.filas().get(1)[7]);
    }

    @Test
    void lasReservasEnColaMuestranSuPuesto() {
        Reserva enCola = new Reserva();
        enCola.setIdReserva(5);
        enCola.setPrestatario(usuario("Juan", "Estudiante", "33333333", "activo"));
        Libro libro = new Libro();
        libro.setTitulo("Clean Code");
        enCola.setLibro(libro);
        enCola.setFechaReserva(LocalDateTime.of(2026, 10, 28, 9, 0));
        enCola.setEstadoReserva("en_espera");
        enCola.setPosicionCola(2);
        when(reservaDao.listarParaReporte(any(), any())).thenReturn(List.of(enCola));

        String[] fila = servicio.armar("reservas", null, null).filas().get(0);

        assertEquals("2", fila[5]);
        assertEquals("en_espera", fila[6]);
    }

    @Test
    void lasBajasLogicasJuntanLibrosPrestamosAnuladosYUsuariosInactivos() {
        Libro deBaja = new Libro();
        deBaja.setIsbn("9780000000001");
        deBaja.setTitulo("Libro viejo");
        deBaja.setEstadoLibro("baja");
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 28, 9, 0);
        when(libroDao.findByEstadoLibroOrderByTitulo("baja")).thenReturn(List.of(deBaja));
        when(prestamoDao.listarParaReporte(any(), any())).thenReturn(List.of(
            new ReportePrestamoDTO(1, "Juan Estudiante", "33333333", "Carlos Librero", "Clean Code",
                    inicio, inicio.plusHours(3), null, false, null, "anulado"),
            new ReportePrestamoDTO(2, "Juan Estudiante", "33333333", "Carlos Librero", "Clean Code",
                    inicio, inicio.plusHours(3), null, false, null, "activo") // este no es una baja
        ));
        when(usuarioDao.findByEstadoOrderByApellidoAscNombreAsc("inactivo"))
                .thenReturn(List.of(usuario("Pedro", "Antiguo", "55555555", "inactivo")));

        List<String[]> filas = servicio.armar("bajas", null, null).filas();

        assertEquals(3, filas.size());
        assertEquals("Libro", filas.get(0)[0]);
        assertEquals("Préstamo", filas.get(1)[0]);
        assertEquals("Usuario", filas.get(2)[0]);
    }

    @Test
    void elStockPorCategoriaTieneUnaFilaDeTotal() {
        when(libroDao.stockPorCategoria()).thenReturn(List.of(
            new Object[] {"Ciencia", 3L, 5L, 1L, 0L},
            new Object[] {"Tecnología", 4L, 7L, 0L, 1L}
        ));

        List<String[]> filas = servicio.armar("stock", null, null).filas();

        assertArrayEquals(new String[] {"Total", "7", "12", "1", "1"}, filas.get(2));
    }

    @Test
    void sinPrestamosLosIndicadoresSalenSinDato() {
        List<String[]> filas = servicio.armar("indicadores", null, null).filas();

        assertEquals(4, filas.size());
        assertTrue(filas.stream().allMatch(f -> "Sin dato".equals(f[2]))); // no se divide entre cero
    }

    @Test
    void losTiemposDanMinimoPromedioYMaximoConUnaFilaGeneral() {
        when(prestamoDao.tiemposPorCategoria(any(), any())).thenReturn(List.of(
            new Object[] {"Ciencia", 1L, 60.0, 60.0, 60.0},
            new Object[] {"Tecnología", 3L, 30.0, 100.0, 150.0}
        ));

        List<String[]> filas = servicio.armar("tiempos", null, null).filas();

        assertArrayEquals(new String[] {"Ciencia", "1", "1 h 00 min", "1 h 00 min", "1 h 00 min"}, filas.get(0));
        // promedio general = (60 x 1 + 100 x 3) / 4 = 90 min
        assertArrayEquals(new String[] {"Todas", "4", "30 min", "1 h 30 min", "2 h 30 min"}, filas.get(2));
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

    private Usuario usuario(String nombre, String apellido, String dni, String estado) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setDni(dni);
        u.setEstado(estado);
        return u;
    }

    @Test
    void importarRechazaArchivoSinLasColumnas() {
        String csv = "nombre,precio\nAlgo,10\n";
        assertThrows(NegocioException.class,
                () -> servicio.importarLibrosCsv(csv.getBytes(StandardCharsets.UTF_8)));
    }
}
