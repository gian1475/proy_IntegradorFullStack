package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dao.ReservaDao;
import com.alejandria.biblioteca.entity.Prestamo;
import com.alejandria.biblioteca.entity.Reserva;
import com.alejandria.biblioteca.exception.NegocioException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reporte de UNA operación en PDF (semana 15): el comprobante de un préstamo y la constancia de una reserva.
 * Los reportes por fechas (listados) están en ReporteService; aquí cada documento es de un solo registro.
 * El PDF lo arma OpenPDF, la misma librería de los reportes.
 */
// comprobante (s15): reporte de una operacion individual. lo pidio el profe: "el paciente necesita el documento de
// su propia cita; el listado general no cumple". aqui: el prestatario descarga el de su prestamo o su reserva
@Service
public class ComprobanteService {

    private static final Logger log = LoggerFactory.getLogger(ComprobanteService.class);

    private static final String SIN_DATO = "-";
    private static final Color FONDO_ETIQUETA = new Color(245, 240, 232); // beige claro, como las tarjetas del sistema

    private final PrestamoDao prestamoDao;
    private final ReservaDao reservaDao;
    private final HorarioAtencion horario;

    public ComprobanteService(PrestamoDao prestamoDao, ReservaDao reservaDao, HorarioAtencion horario) {
        this.prestamoDao = prestamoDao;
        this.reservaDao = reservaDao;
        this.horario = horario;
    }

    /** Comprobante de un préstamo en sala: quién lo pidió, qué libro, desde cuándo, hasta qué hora y cómo terminó. */
    // comprobante prestamo: lee el prestamo con su prestatario, su encargado y su libro (las relaciones se cargan
    // dentro de la transaccion) y arma el pdf. sirve para cualquier estado: activo, vencido, devuelto o anulado
    @Transactional(readOnly = true)
    public byte[] comprobantePrestamo(Integer idPrestamo) {
        Prestamo p = prestamoDao.findById(idPrestamo)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Préstamo no encontrado"));
        String estado = p.getEstadoPrestamo();
        boolean enCurso = "activo".equals(estado) || "vencido".equals(estado);

        List<String[]> datos = List.of(
                dato("Prestatario", p.getPrestatario().nombreCompleto()),
                dato("DNI", p.getPrestatario().getDni()),
                dato("Libro", p.getLibro().getTitulo()),
                dato("ISBN", p.getLibro().getIsbn()),
                dato("Registrado por (encargado)", p.getEncargado().nombreCompleto()),
                dato("Inicio", fecha(p.getFechaPrestamo())),
                dato("Hora límite de devolución", fecha(p.getFechaDevolucionPrevista())),
                dato("Ampliado", Boolean.TRUE.equals(p.getAmpliado()) ? "Sí" : "No"),
                dato("Devolución", enCurso ? "Pendiente" : fecha(p.getFechaDevolucionReal())),
                dato("Estado físico al devolver", texto(p.getEstadoFisicoDevolucion())),
                dato("Estado del préstamo", estado));

        // la nota depende del estado: en curso recuerda las reglas; devuelto o anulado dice q ya termino
        List<String> notas;
        if (enCurso) {
            notas = List.of("Préstamo solo en sala: el libro se devuelve en la sala antes de la hora límite "
                            + "(3 horas, sin pasar de las " + horario.getCierre() + ").",
                    "Se puede ampliar una sola vez, hasta 1 hora, si nadie reservó el libro.");
        } else if ("anulado".equals(estado)) {
            notas = List.of("Este préstamo fue anulado (eliminación lógica): el registro se conserva en el sistema.");
        } else {
            notas = List.of("Préstamo terminado: el libro ya se devolvió en la sala.");
        }

        byte[] pdf = generarPdf("Comprobante de préstamo en sala N° " + idPrestamo, datos, notas);
        log.info("Comprobante del préstamo #{} emitido", idPrestamo); // logback
        return pdf;
    }

    /** Constancia de una reserva: qué libro, para quién, y hasta cuándo recogerlo (o su puesto en la cola). */
    // constancia reserva: si esta en la cola de espera todavia no tiene fecha de recojo, se muestra su puesto
    @Transactional(readOnly = true)
    public byte[] constanciaReserva(Integer idReserva) {
        Reserva r = reservaDao.findById(idReserva)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
        String estado = r.getEstadoReserva();
        boolean enCola = ColaEsperaService.EN_ESPERA.equals(estado);

        List<String[]> datos = new ArrayList<>(List.of(
                dato("Prestatario", r.getPrestatario().nombreCompleto()),
                dato("DNI", r.getPrestatario().getDni()),
                dato("Libro", r.getLibro().getTitulo()),
                dato("ISBN", r.getLibro().getIsbn()),
                dato("Reservada el", fecha(r.getFechaReserva())),
                dato("Estado de la reserva", enCola ? "En cola de espera" : estado)));
        datos.add(enCola
                ? dato("Puesto en la cola", String.valueOf(r.getPosicionCola()))
                : dato("Recoger hasta", fecha(r.getFechaRecojoLimite())));

        List<String> notas;
        if (enCola) {
            notas = List.of("Todavía no hay un ejemplar para usted. Cuando se devuelva uno y le toque, la reserva "
                    + "pasa a pendiente y tendrá hasta las " + horario.getCierre() + " del día siguiente para recogerlo.");
        } else if ("pendiente".equals(estado)) {
            notas = List.of("Presente su DNI en la ventanilla antes de la fecha y la hora límite.",
                    "Si no recoge el libro a tiempo, la reserva vence y el ejemplar vuelve al stock.");
        } else {
            notas = List.of("Muestra el estado de la reserva en el momento en que se emitió.");
        }

        byte[] pdf = generarPdf("Constancia de reserva N° " + idReserva, datos, notas);
        log.info("Constancia de la reserva #{} emitida", idReserva);
        return pdf;
    }

    // ---------- PDF con OpenPDF ----------

    // openpdf: media hoja (A5 vertical) con el nombre de la biblioteca, el titulo, la fecha de emision,
    // una tabla de dos columnas (dato | valor) y las notas
    private byte[] generarPdf(String titulo, List<String[]> datos, List<String> notas) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A5, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Font fuenteBiblioteca = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, ReporteService.COLOR_MARRON);
            Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font fuenteEtiqueta = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font fuenteNormal = FontFactory.getFont(FontFactory.HELVETICA, 9);

            documento.add(new Paragraph("Biblioteca de Alejandría", fuenteBiblioteca));
            documento.add(new Paragraph(titulo, fuenteTitulo));
            documento.add(new Paragraph("Emitido: " + fecha(horario.ahora()), fuenteNormal));
            documento.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(new float[] {2, 3}); // la etiqueta ocupa 2/5 del ancho y el valor 3/5
            tabla.setWidthPercentage(100);
            for (String[] fila : datos) {
                PdfPCell etiqueta = new PdfPCell(new Phrase(fila[0], fuenteEtiqueta));
                etiqueta.setBackgroundColor(FONDO_ETIQUETA);
                etiqueta.setPadding(5);
                tabla.addCell(etiqueta);
                PdfPCell valor = new PdfPCell(new Phrase(fila[1], fuenteNormal));
                valor.setPadding(5);
                tabla.addCell(valor);
            }
            documento.add(tabla);

            documento.add(new Paragraph(" "));
            for (String nota : notas) {
                documento.add(new Paragraph("Nota: " + nota, fuenteNormal));
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

    // ---------- Auxiliares ----------

    private String[] dato(String etiqueta, String valor) {
        return new String[] {etiqueta, valor};
    }

    private String fecha(LocalDateTime valor) {
        return valor == null ? SIN_DATO : valor.format(ReporteService.FORMATO_FECHA);
    }

    private String texto(String valor) {
        return valor == null ? SIN_DATO : valor;
    }
}
