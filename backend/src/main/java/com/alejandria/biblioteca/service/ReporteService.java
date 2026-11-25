package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.CategoriaDao;
import com.alejandria.biblioteca.dao.AuditoriaInventarioDao;
import com.alejandria.biblioteca.dao.LibroDao;
import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dao.ReservaDao;
import com.alejandria.biblioteca.dao.UsuarioDao;
import com.alejandria.biblioteca.dto.libro.LibroFormDTO;
import com.alejandria.biblioteca.dto.reporte.ImportacionResultadoDTO;
import com.alejandria.biblioteca.dto.reporte.IndicadorDTO;
import com.alejandria.biblioteca.dto.reporte.ReportePrestamoDTO;
import com.alejandria.biblioteca.dto.reporte.TablaReporte;
import com.alejandria.biblioteca.entity.Libro;
import com.alejandria.biblioteca.entity.Reserva;
import com.alejandria.biblioteca.entity.Usuario;
import com.alejandria.biblioteca.exception.NegocioException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
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
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Reportes y archivos (RF08):
 * - Cada reporte se arma como una TablaReporte (título, periodo, columnas, filas y notas).
 * - Apache POI la convierte en Excel (.xlsx), OpenPDF en PDF y Apache Commons CSV en CSV.
 * - Commons CSV también importa libros desde un CSV.
 */
// reportes rf08: prestamos, devoluciones, reservas, incidencias y sanciones, bajas logicas, stock por categoria,
// indicadores, tiempos (minimo, promedio, maximo) y prestamos por dia. todos con rango de fechas y en excel, pdf o csv.
// el comprobante de UNA operacion (un prestamo o una reserva) esta en ComprobanteService
@Service
public class ReporteService {

    private static final Logger log = LoggerFactory.getLogger(ReporteService.class);

    private static final String[] COLUMNAS_PRESTAMOS = {
        "N°", "Prestatario", "DNI", "Encargado", "Libro", "Inicio", "Hora límite",
        "Devolución", "Ampliado", "Estado físico", "Estado"
    };
    // sin "private": ComprobanteService (mismo paquete) usa el mismo formato y el mismo color en sus pdf
    static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    static final Color COLOR_MARRON = new Color(133, 77, 14); // mismo color del sistema (#854d0e)
    private static final String SIN_DATO = "Sin dato";

    /** Columnas que debe tener el CSV de libros (la primera fila del archivo). */
    public static final String[] COLUMNAS_CSV_LIBROS = {
        "titulo", "isbn", "anio_publicacion", "editorial", "stock", "categoria"
    };

    private final PrestamoDao prestamoDao;
    private final ReservaDao reservaDao;
    private final LibroDao libroDao;
    private final UsuarioDao usuarioDao;
    private final CategoriaDao categoriaDao;
    private final LibroService libroService;
    private final AuditoriaInventarioDao auditoriaDao;
    private final Validator validator;

    public ReporteService(PrestamoDao prestamoDao, ReservaDao reservaDao, LibroDao libroDao, UsuarioDao usuarioDao,
                          CategoriaDao categoriaDao, LibroService libroService, AuditoriaInventarioDao auditoriaDao,
                          Validator validator) {
        this.prestamoDao = prestamoDao;
        this.reservaDao = reservaDao;
        this.libroDao = libroDao;
        this.usuarioDao = usuarioDao;
        this.categoriaDao = categoriaDao;
        this.libroService = libroService;
        this.auditoriaDao = auditoriaDao;
        this.validator = validator;
    }

    /** Arma el reporte pedido y lo convierte al formato (excel, pdf o csv). Las fechas son opcionales. */
    @Transactional(readOnly = true)
    public byte[] generar(String tipo, String formato, LocalDate desde, LocalDate hasta) {
        if (!List.of("excel", "pdf", "csv").contains(formato)) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Formato no válido: use excel, pdf o csv");
        }
        TablaReporte tabla = armar(tipo, desde, hasta);
        byte[] archivo = switch (formato) {
            case "excel" -> generarExcel(tabla);
            case "pdf" -> generarPdf(tabla);
            default -> generarCsv(tabla);
        };
        log.info("Reporte '{}' en {} generado ({} filas, {})", tipo, formato, tabla.filas().size(), tabla.periodo());
        return archivo;
    }

    // ---------- Los reportes (RF08) ----------

    /** Arma la tabla del reporte. tipo: prestamos, devoluciones, reservas, incidencias, bajas, stock, indicadores, tiempos o pordia. */
    @Transactional(readOnly = true)
    public TablaReporte armar(String tipo, LocalDate desde, LocalDate hasta) {
        RangoFechas rango = new RangoFechas(desde, hasta); // valida q "desde" no sea despues de "hasta"
        return switch (Objects.toString(tipo, "")) {
            case "prestamos" -> prestamos(rango);
            case "devoluciones" -> devoluciones(rango);
            case "reservas" -> reservas(rango);
            case "incidencias" -> incidencias(rango);
            case "bajas" -> bajas(rango);
            case "stock" -> stockPorCategoria();
            case "indicadores" -> indicadores(rango);
            case "tiempos" -> tiempos(rango);
            case "pordia" -> prestamosPorDia(rango);
            default -> throw new NegocioException(HttpStatus.BAD_REQUEST, "Tipo de reporte no válido");
        };
    }

    // reporte prestamos: todos los prestamos del rango (por la fecha del prestamo), con todos sus estados
    private TablaReporte prestamos(RangoFechas rango) {
        List<String[]> filas = new ArrayList<>();
        for (ReportePrestamoDTO p : prestamoDao.listarParaReporte(rango.inicio(), rango.fin())) {
            filas.add(valoresDeFila(p));
        }
        return new TablaReporte("Préstamos", rango.texto(), COLUMNAS_PRESTAMOS, filas, List.of());
    }

    // reporte devoluciones: prestamos devueltos en el rango (por la fecha de devolucion), a tiempo o con retraso
    private TablaReporte devoluciones(RangoFechas rango) {
        String[] columnas = {"N°", "Prestatario", "DNI", "Libro", "Inicio", "Hora límite", "Devolución",
                             "Puntualidad", "Estado físico", "Encargado"};
        List<String[]> filas = new ArrayList<>();
        for (ReportePrestamoDTO p : prestamoDao.listarDevoluciones(rango.inicio(), rango.fin())) {
            filas.add(new String[] {
                String.valueOf(p.idPrestamo()), p.prestatario(), p.dniPrestatario(), p.tituloLibro(),
                formatear(p.fechaPrestamo()), formatear(p.fechaDevolucionPrevista()), formatear(p.fechaDevolucionReal()),
                puntualidad(p.fechaDevolucionPrevista(), p.fechaDevolucionReal()),
                texto(p.estadoFisicoDevolucion()), p.encargado()
            });
        }
        return new TablaReporte("Devoluciones", rango.texto(), columnas, filas,
                List.of("Se filtra por la fecha en que se devolvió el libro."));
    }

    // reporte reservas: las hechas en el rango. las borradas fisicamente (RF17) ya no estan en la bd
    private TablaReporte reservas(RangoFechas rango) {
        String[] columnas = {"N°", "Prestatario", "Libro", "Reservada el", "Recoger hasta", "Puesto en la cola", "Estado"};
        List<String[]> filas = new ArrayList<>();
        for (Reserva r : reservaDao.listarParaReporte(rango.inicio(), rango.fin())) {
            boolean enCola = ColaEsperaService.EN_ESPERA.equals(r.getEstadoReserva());
            filas.add(new String[] {
                String.valueOf(r.getIdReserva()),
                r.getPrestatario().nombreCompleto(),
                r.getLibro().getTitulo(),
                formatear(r.getFechaReserva()),
                enCola ? "Cuando le toque un ejemplar" : formatear(r.getFechaRecojoLimite()),
                enCola ? String.valueOf(r.getPosicionCola()) : "-",
                r.getEstadoReserva()
            });
        }
        return new TablaReporte("Reservas", rango.texto(), columnas, filas,
                List.of("Las reservas canceladas o vencidas que el administrador eliminó físicamente (RF17) ya no están en la base de datos y no aparecen."));
    }

    // reporte incidencias: cada incidencia con su sancion (si tiene). se filtra por la fecha de la incidencia
    private TablaReporte incidencias(RangoFechas rango) {
        String[] columnas = {"N°", "Préstamo", "Prestatario", "DNI", "Libro", "Tipo", "Descripción", "Fecha", "Estado",
                             "Sanción", "Monto (S/)", "Inicio sanción", "Fin sanción", "Estado sanción"};
        List<String[]> filas = new ArrayList<>();
        for (Object[] f : prestamoDao.listarIncidenciasConSancion(rango.inicio(), rango.fin())) {
            filas.add(new String[] {
                texto(f[0]), texto(f[1]), texto(f[2]), texto(f[3]), texto(f[4]), texto(f[5]), texto(f[6]),
                fechaDe(f[7]), texto(f[8]),
                f[9] == null ? "Sin sanción" : texto(f[9]),
                f[10] == null ? "" : String.format(Locale.US, "%.2f", (BigDecimal) f[10]),
                fechaDe(f[11]), fechaDe(f[12]), texto(f[13])
            });
        }
        return new TablaReporte("Incidencias y sanciones", rango.texto(), columnas, filas, List.of());
    }

    // una fila del reporte de bajas antes de numerarla (para ordenarlas por la fecha de baja)
    private record Baja(LocalDateTime fecha, String tipo, String codigo, String detalle, String estado) {}

    // reporte bajas logicas: listado numerado (como una lista de asistencia) de los libros 'baja', prestamos
    // 'anulado' y usuarios 'inactivo' dados de baja en el periodo, por su fecha de baja (migracion 06).
    // las filas siguen en la bd. al final dice cuantos registros se eliminaron de manera logica
    private TablaReporte bajas(RangoFechas rango) {
        boolean sinFiltro = rango.desde() == null && rango.hasta() == null;
        List<Baja> bajas = new ArrayList<>();
        int sinFecha = 0; // bajas de antes de la migracion 06: no tienen fecha y no se pueden ubicar en un periodo

        for (Libro l : libroDao.findByEstadoLibroOrderByTitulo("baja")) {
            if (l.getFechaBaja() == null) {
                sinFecha++;
                if (sinFiltro) bajas.add(new Baja(null, "Libro", l.getIsbn(), l.getTitulo(), l.getEstadoLibro()));
            } else if (dentroDe(rango, l.getFechaBaja())) {
                bajas.add(new Baja(l.getFechaBaja(), "Libro", l.getIsbn(), l.getTitulo(), l.getEstadoLibro()));
            }
        }
        for (Object[] p : prestamoDao.listarAnulados(rango.inicio(), rango.fin())) {
            bajas.add(new Baja(aFecha(p[4]), "Préstamo", "N° " + p[0],
                    p[2] + " - " + p[1] + " (prestado el " + fechaDe(p[3]) + ")", "anulado"));
        }
        for (Usuario u : usuarioDao.findByEstadoOrderByApellidoAscNombreAsc("inactivo")) {
            if (u.getFechaBaja() == null) {
                sinFecha++;
                if (sinFiltro) bajas.add(new Baja(null, "Usuario", u.getDni(), u.nombreCompleto(), u.getEstado()));
            } else if (dentroDe(rango, u.getFechaBaja())) {
                bajas.add(new Baja(u.getFechaBaja(), "Usuario", u.getDni(), u.nombreCompleto(), u.getEstado()));
            }
        }

        // de la mas antigua a la mas reciente; las que no tienen fecha van primero
        bajas.sort(java.util.Comparator.comparing(Baja::fecha, java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())));

        String[] columnas = {"N°", "Tipo", "Código", "Detalle", "Fecha de baja", "Estado"};
        List<String[]> filas = new ArrayList<>();
        int numero = 1;
        for (Baja b : bajas) {
            filas.add(new String[] {String.valueOf(numero++), b.tipo(), b.codigo(), b.detalle(),
                                    b.fecha() == null ? "Sin fecha" : formatear(b.fecha()), b.estado()});
        }

        long libros = bajas.stream().filter(b -> "Libro".equals(b.tipo())).count();
        long prestamos = bajas.stream().filter(b -> "Préstamo".equals(b.tipo())).count();
        long usuarios = bajas.stream().filter(b -> "Usuario".equals(b.tipo())).count();
        String total = bajas.size() == 1 ? "1 registro eliminado de manera lógica"
                                         : bajas.size() + " registros eliminados de manera lógica";
        List<String> pie = List.of(total + " (" + cantidad(libros, "libro", "libros") + ", "
                + cantidad(prestamos, "préstamo anulado", "préstamos anulados") + " y "
                + cantidad(usuarios, "usuario inactivo", "usuarios inactivos") + ").");

        List<String> notas = new ArrayList<>();
        notas.add("Eliminación lógica: estas filas siguen en la base de datos con el estado 'baja', 'anulado' o 'inactivo'.");
        notas.add("Se filtra por la fecha de baja: la de la baja del libro o del usuario, o la de la anulación del préstamo.");
        if (sinFecha > 0 && !sinFiltro) {
            notas.add(cantidad(sinFecha, "libro o usuario", "libros o usuarios") + " de baja desde antes del 25/11/2026 "
                    + "no tienen fecha registrada y solo aparecen en el reporte sin fechas.");
        }
        return new TablaReporte("Bajas lógicas", rango.texto(), columnas, filas, notas, pie);
    }

    private static boolean dentroDe(RangoFechas rango, LocalDateTime fecha) {
        return !fecha.isBefore(rango.inicio()) && fecha.isBefore(rango.fin());
    }

    private static String cantidad(long n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }

    private static LocalDateTime aFecha(Object valor) {
        if (valor instanceof Timestamp t) {
            return t.toLocalDateTime();
        }
        return valor instanceof LocalDateTime f ? f : null;
    }

    // reporte stock: titulos y ejemplares disponibles de cada categoria (sin los dados de baja). es una foto de ahora
    private TablaReporte stockPorCategoria() {
        String[] columnas = {"Categoría", "Títulos", "Ejemplares disponibles", "Títulos sin stock", "En mantenimiento"};
        List<String[]> filas = new ArrayList<>();
        long[] totales = new long[4];
        for (Object[] f : libroDao.stockPorCategoria()) {
            String[] fila = new String[5];
            fila[0] = (String) f[0];
            for (int i = 0; i < 4; i++) {
                long valor = ((Number) f[i + 1]).longValue();
                totales[i] += valor;
                fila[i + 1] = String.valueOf(valor);
            }
            filas.add(fila);
        }
        filas.add(new String[] {"Total", String.valueOf(totales[0]), String.valueOf(totales[1]),
                                String.valueOf(totales[2]), String.valueOf(totales[3])});
        // detalle que sustenta los totales: cada libro (sin los de baja) con su categoria, stock y estado
        filas.add(new String[] {"", "", "", "", ""});
        filas.add(new String[] {"DETALLE POR LIBRO", "Título", "Ejemplares disponibles", "ISBN", "Estado"});
        for (Libro l : libroDao.findByEstadoLibroNotOrderByTitulo("baja")) {
            filas.add(new String[] {l.getCategoria() == null ? "-" : l.getCategoria().getNombre(), l.getTitulo(),
                                    String.valueOf(l.getStock()), l.getIsbn(), l.getEstadoLibro()});
        }
        return new TablaReporte("Stock por categoría", "Al " + formatear(LocalDateTime.now()), columnas, filas,
                List.of("Muestra el stock de este momento: no depende del rango de fechas.",
                        "Debajo de los totales está el detalle por libro: la suma de sus ejemplares da el total."));
    }

    // reporte indicadores: los 4 del apartado 1.2.2 calculados con los prestamos del rango. sin datos -> "Sin dato"
    private TablaReporte indicadores(RangoFechas rango) {
        IndicadoresGestion.Datos datos = IndicadoresGestion.Datos.leer(prestamoDao, auditoriaDao, rango.inicio(), rango.fin());

        String[] columnas = {"Código", "Indicador", "Valor", "Línea base", "Meta", "¿Cumple la meta?", "Detalle"};
        List<String[]> filas = new ArrayList<>();
        for (IndicadorDTO i : IndicadoresGestion.calcular(datos, "en el periodo")) {
            filas.add(new String[] {
                i.codigo(), i.nombre(),
                i.valor() == null ? SIN_DATO : i.valor() + " " + i.unidad(),
                i.lineaBase(), i.meta(),
                i.cumpleMeta() == null ? SIN_DATO : (i.cumpleMeta() ? "Sí" : "No"),
                i.detalle()
            });
        }
        return new TablaReporte("Indicadores de gestión", rango.texto(), columnas, filas,
                List.of("Préstamos del periodo (sin anulados): " + datos.prestamos() + ".",
                        "Los indicadores sin información suficiente se presentan como \"Sin dato\"."));
    }

    // reporte tiempos: cuanto estuvo el libro en sala (de la entrega a la devolucion): minimo, promedio y maximo
    private TablaReporte tiempos(RangoFechas rango) {
        String[] columnas = {"Categoría", "Préstamos devueltos", "Mínimo", "Promedio", "Máximo"};
        List<String[]> filas = new ArrayList<>();
        long cantidadTotal = 0;
        double sumaMinutos = 0;
        Double minimoTotal = null;
        Double maximoTotal = null;
        for (Object[] f : prestamoDao.tiemposPorCategoria(rango.inicio(), rango.fin())) {
            long cantidad = ((Number) f[1]).longValue();
            double minimo = ((Number) f[2]).doubleValue();
            double promedio = ((Number) f[3]).doubleValue();
            double maximo = ((Number) f[4]).doubleValue();
            filas.add(new String[] {(String) f[0], String.valueOf(cantidad), duracion(minimo), duracion(promedio), duracion(maximo)});
            // para la fila "Todas": el promedio general se pondera con la cantidad de cada categoria
            cantidadTotal += cantidad;
            sumaMinutos += promedio * cantidad;
            minimoTotal = minimoTotal == null ? minimo : Math.min(minimoTotal, minimo);
            maximoTotal = maximoTotal == null ? maximo : Math.max(maximoTotal, maximo);
        }
        filas.add(cantidadTotal == 0
                ? new String[] {"Todas", "0", SIN_DATO, SIN_DATO, SIN_DATO}
                : new String[] {"Todas", String.valueOf(cantidadTotal), duracion(minimoTotal),
                                duracion(sumaMinutos / cantidadTotal), duracion(maximoTotal)});
        return new TablaReporte("Tiempos de préstamo", rango.texto(), columnas, filas,
                List.of("Tiempo desde que se entrega el libro hasta que se devuelve. Solo cuenta los préstamos devueltos."));
    }

    // reporte por dia (R4 maximo, minimo y promedio): cuantos prestamos hubo cada dia del rango (sin anulados),
    // el dia con mas, el dia con menos y el promedio por dia. debajo, los prestamos de esos dias para verificarlos
    private TablaReporte prestamosPorDia(RangoFechas rango) {
        java.util.TreeMap<LocalDate, List<ReportePrestamoDTO>> porDia = new java.util.TreeMap<>();
        for (ReportePrestamoDTO p : prestamoDao.listarParaReporte(rango.inicio(), rango.fin())) {
            if ("anulado".equals(p.estadoPrestamo())) continue;
            porDia.computeIfAbsent(p.fechaPrestamo().toLocalDate(), d -> new ArrayList<>()).add(p);
        }
        String[] columnas = {"Sección", "Fecha", "Préstamos del día", "N°", "Prestatario", "Libro", "Inicio", "Estado"};
        List<String[]> filas = new ArrayList<>();
        DateTimeFormatter dia = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (porDia.isEmpty()) {
            filas.add(new String[] {"Sin préstamos en el periodo", "", "0", "", "", "", "", ""});
            return new TablaReporte("Préstamos por día (máximo, mínimo y promedio)", rango.texto(), columnas, filas, List.of());
        }
        for (var e : porDia.entrySet()) {
            filas.add(new String[] {"Préstamos por día", e.getKey().format(dia), String.valueOf(e.getValue().size()), "", "", "", "", ""});
        }
        var max = porDia.entrySet().stream().max(java.util.Comparator.comparingInt(e -> e.getValue().size())).orElseThrow();
        var min = porDia.entrySet().stream().min(java.util.Comparator.comparingInt(e -> e.getValue().size())).orElseThrow();
        int total = porDia.values().stream().mapToInt(List::size).sum();
        double promedio = (double) total / porDia.size();
        filas.add(new String[] {"MÁXIMO (día con más préstamos)", max.getKey().format(dia), String.valueOf(max.getValue().size()), "", "", "", "", ""});
        filas.add(new String[] {"MÍNIMO (día con menos préstamos)", min.getKey().format(dia), String.valueOf(min.getValue().size()), "", "", "", "", ""});
        filas.add(new String[] {"PROMEDIO por día con préstamos", porDia.size() + " días", String.format(Locale.US, "%.2f", promedio), "", "", "", "", ""});
        agregarDetalleDia(filas, "Detalle del día MÁXIMO", max.getKey().format(dia), max.getValue());
        agregarDetalleDia(filas, "Detalle del día MÍNIMO", min.getKey().format(dia), min.getValue());
        return new TablaReporte("Préstamos por día (máximo, mínimo y promedio)", rango.texto(), columnas, filas,
                List.of("Total de préstamos del periodo (sin anulados): " + total + ".",
                        "Promedio = total / días que tuvieron al menos un préstamo.",
                        "Las filas de detalle permiten contar a mano los préstamos del día máximo y del mínimo."));
    }

    private void agregarDetalleDia(List<String[]> filas, String seccion, String fecha, List<ReportePrestamoDTO> prestamos) {
        for (ReportePrestamoDTO p : prestamos) {
            filas.add(new String[] {seccion, fecha, "", String.valueOf(p.idPrestamo()), p.prestatario(), p.tituloLibro(),
                                    formatear(p.fechaPrestamo()), p.estadoPrestamo()});
        }
    }

    // ---------- EXCEL con Apache POI ----------

    // apache poi: cualquier reporte (TablaReporte) se convierte en una hoja de excel
    public byte[] generarExcel(TablaReporte tabla) {
        try (Workbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet(WorkbookUtil.createSafeSheetName(tabla.titulo()));

            // Estilo del encabezado: negrita, fondo de color
            Font negrita = libro.createFont();
            negrita.setBold(true);
            negrita.setColor(IndexedColors.WHITE.getIndex());
            CellStyle estiloEncabezado = libro.createCellStyle();
            estiloEncabezado.setFont(negrita);
            estiloEncabezado.setFillForegroundColor(IndexedColors.BROWN.getIndex());
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Título, periodo, fecha de generación y notas
            int numFila = 0;
            hoja.createRow(numFila++).createCell(0).setCellValue("Biblioteca de Alejandría - Reporte de " + tabla.titulo().toLowerCase());
            hoja.createRow(numFila++).createCell(0).setCellValue("Periodo: " + tabla.periodo()
                    + "  |  Generado: " + LocalDateTime.now().format(FORMATO_FECHA) + "  |  Total: " + tabla.filas().size());
            for (String nota : tabla.notas()) {
                hoja.createRow(numFila++).createCell(0).setCellValue("Nota: " + nota);
            }
            numFila++; // una fila en blanco antes de la tabla

            // Encabezado de la tabla
            int filaEncabezado = numFila;
            Row encabezado = hoja.createRow(numFila++);
            for (int i = 0; i < tabla.columnas().length; i++) {
                Cell celda = encabezado.createCell(i);
                celda.setCellValue(tabla.columnas()[i]);
                celda.setCellStyle(estiloEncabezado);
            }

            // Datos
            for (String[] valores : tabla.filas()) {
                Row fila = hoja.createRow(numFila++);
                for (int i = 0; i < valores.length; i++) {
                    fila.createCell(i).setCellValue(valores[i]);
                }
            }

            // pie debajo de la tabla, con una fila en blanco antes
            if (!tabla.pie().isEmpty()) {
                numFila++;
                Font negritaPie = libro.createFont();
                negritaPie.setBold(true);
                CellStyle estiloPie = libro.createCellStyle();
                estiloPie.setFont(negritaPie);
                for (String linea : tabla.pie()) {
                    Cell celda = hoja.createRow(numFila++).createCell(0);
                    celda.setCellValue(linea);
                    celda.setCellStyle(estiloPie);
                }
            }

            for (int i = 0; i < tabla.columnas().length; i++) {
                hoja.autoSizeColumn(i);
            }
            hoja.createFreezePane(0, filaEncabezado + 1); // el encabezado queda fijo al bajar

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new NegocioException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el Excel");
        }
    }

    // ---------- PDF con OpenPDF ----------

    // openpdf: cualquier reporte (TablaReporte) se convierte en un pdf A4 horizontal con su tabla
    public byte[] generarPdf(TablaReporte tabla) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4.rotate(), 25, 25, 30, 30);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            int tamanoCelda = tabla.columnas().length > 10 ? 7 : 8; // con muchas columnas, letra mas chica
            com.lowagie.text.Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, COLOR_MARRON);
            com.lowagie.text.Font fuenteNormal = FontFactory.getFont(FontFactory.HELVETICA, 9);
            com.lowagie.text.Font fuenteEncabezado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, tamanoCelda, Color.WHITE);
            com.lowagie.text.Font fuenteCelda = FontFactory.getFont(FontFactory.HELVETICA, tamanoCelda);

            documento.add(new Paragraph("Biblioteca de Alejandría - Reporte de " + tabla.titulo().toLowerCase(), fuenteTitulo));
            documento.add(new Paragraph("Periodo: " + tabla.periodo() + "   |   Generado: "
                    + LocalDateTime.now().format(FORMATO_FECHA) + "   |   Total: " + tabla.filas().size(), fuenteNormal));
            for (String nota : tabla.notas()) {
                documento.add(new Paragraph("Nota: " + nota, fuenteNormal));
            }
            documento.add(new Paragraph(" "));

            PdfPTable pdfTabla = new PdfPTable(tabla.columnas().length);
            pdfTabla.setWidthPercentage(100);
            pdfTabla.setWidths(anchosDeColumna(tabla));
            pdfTabla.setHeaderRows(1); // el encabezado se repite en cada página

            for (String columna : tabla.columnas()) {
                PdfPCell celda = new PdfPCell(new Phrase(columna, fuenteEncabezado));
                celda.setBackgroundColor(COLOR_MARRON);
                celda.setPadding(4);
                pdfTabla.addCell(celda);
            }
            for (String[] valores : tabla.filas()) {
                for (String valor : valores) {
                    PdfPCell celda = new PdfPCell(new Phrase(valor, fuenteCelda));
                    celda.setPadding(3);
                    pdfTabla.addCell(celda);
                }
            }

            documento.add(pdfTabla);

            // pie debajo de la tabla (ej. "8 registros eliminados de manera logica"), en negrita
            com.lowagie.text.Font fuentePie = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_MARRON);
            for (String linea : tabla.pie()) {
                Paragraph parrafo = new Paragraph(linea, fuentePie);
                parrafo.setSpacingBefore(8);
                documento.add(parrafo);
            }
        } catch (DocumentException e) {
            throw new NegocioException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el PDF");
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return salida.toByteArray();
    }

    // ---------- CSV con Apache Commons CSV ----------

    // commons csv: el csv solo lleva el encabezado y las filas (para abrirlo en excel u otro programa)
    public byte[] generarCsv(TablaReporte tabla) {
        CSVFormat formato = CSVFormat.DEFAULT.builder().setHeader(tabla.columnas()).get();

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (OutputStreamWriter escritor = new OutputStreamWriter(salida, StandardCharsets.UTF_8)) {
            // BOM: para que Excel muestre bien las tildes. va ANTES de crear el CSVPrinter, porque el
            // CSVPrinter escribe el encabezado apenas se crea (antes el BOM quedaba despues del encabezado)
            escritor.write('\uFEFF');
            try (CSVPrinter csv = new CSVPrinter(escritor, formato)) {
                for (String[] valores : tabla.filas()) {
                    csv.printRecord((Object[]) valores);
                }
            }
        } catch (IOException e) {
            throw new NegocioException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el CSV");
        }
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
                    LibroFormDTO form = aFormulario(registro);
                    // bean validation: el csv no pasa por el controller (@Valid), asi q las anotaciones
                    // de LibroFormDTO se revisan aqui con el mismo Validator
                    Set<ConstraintViolation<LibroFormDTO>> problemas = validator.validate(form);
                    if (!problemas.isEmpty()) {
                        errores.add("Fila " + linea + ": " + problemas.iterator().next().getMessage());
                        continue;
                    }
                    libroService.crear(form);
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

        return new LibroFormDTO(registro.get("titulo"), registro.get("isbn"),
                Integer.parseInt(registro.get("anio_publicacion")), registro.get("editorial"),
                Integer.parseInt(registro.get("stock")), idCategoria, null);
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
            texto(p.estadoFisicoDevolucion()),
            p.estadoPrestamo()
        };
    }

    /** "A tiempo" o "Con retraso (12 min)". */
    private String puntualidad(LocalDateTime limite, LocalDateTime devolucion) {
        if (devolucion == null || !devolucion.isAfter(limite)) {
            return "A tiempo";
        }
        return "Con retraso (" + Duration.between(limite, devolucion).toMinutes() + " min)";
    }

    /** Minutos en texto: "45 min" o "2 h 05 min". */
    private String duracion(double minutos) {
        long total = Math.round(minutos);
        if (total < 60) {
            return total + " min";
        }
        return String.format("%d h %02d min", total / 60, total % 60);
    }

    /** Ancho de cada columna del PDF según lo largo de su contenido (entre 4 y 30). */
    private float[] anchosDeColumna(TablaReporte tabla) {
        float[] anchos = new float[tabla.columnas().length];
        for (int i = 0; i < anchos.length; i++) {
            int largo = tabla.columnas()[i].length();
            for (String[] fila : tabla.filas().subList(0, Math.min(50, tabla.filas().size()))) {
                largo = Math.max(largo, fila[i] == null ? 0 : fila[i].length());
            }
            anchos[i] = Math.max(4, Math.min(30, largo));
        }
        return anchos;
    }

    /** Las consultas nativas devuelven las fechas como Timestamp o LocalDateTime según el driver. */
    private String fechaDe(Object valor) {
        if (valor instanceof Timestamp t) {
            return formatear(t.toLocalDateTime());
        }
        if (valor instanceof LocalDateTime f) {
            return formatear(f);
        }
        return texto(valor);
    }

    private String texto(Object valor) {
        return Objects.toString(valor, ""); // java.util.Objects: null -> ""
    }

    private String formatear(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FORMATO_FECHA);
    }
}
