package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dao.ReservaDao;
import com.alejandria.biblioteca.entity.Categoria;
import com.alejandria.biblioteca.entity.Libro;
import com.alejandria.biblioteca.entity.Prestamo;
import com.alejandria.biblioteca.entity.Reserva;
import com.alejandria.biblioteca.entity.Usuario;
import com.alejandria.biblioteca.exception.NegocioException;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Semana 15: el reporte de UNA operación en PDF (el comprobante). Se lee el texto del PDF con
 * PdfTextExtractor, que viene en OpenPDF (no es una librería nueva).
 */
@DisplayName("Comprobante del préstamo y constancia de la reserva en PDF (semana 15)")
@ExtendWith(MockitoExtension.class)
class ComprobanteServiceTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 24, 9, 0);

    @Mock private PrestamoDao prestamoDao;
    @Mock private ReservaDao reservaDao;

    private ComprobanteService servicio;

    @BeforeEach
    void preparar() {
        // el reloj del sistema marca el 24/11/2026 a las 10:30: es la fecha de emision del documento
        servicio = new ComprobanteService(prestamoDao, reservaDao,
                HorarioAtencionTest.horario(LocalDateTime.of(2026, 11, 24, 10, 30)));
    }

    // ---------- Comprobante del préstamo ----------

    @Test
    void elComprobanteDeUnPrestamoEnCursoTraeSusDatos() throws IOException {
        when(prestamoDao.findById(12)).thenReturn(Optional.of(prestamo("activo")));

        byte[] pdf = servicio.comprobantePrestamo(12);

        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII)); // todo PDF empieza con "%PDF"
        String texto = textoDe(pdf);
        assertTrue(texto.contains("Comprobante de préstamo en sala N° 12"));
        assertTrue(texto.contains("Emitido: 24/11/2026 10:30"));
        assertTrue(texto.contains("Valeria Flores"));
        assertTrue(texto.contains("73456783"));
        assertTrue(texto.contains("Rayuela"));
        assertTrue(texto.contains("9788437604572"));
        assertTrue(texto.contains("Carlos Librero"));
        assertTrue(texto.contains("24/11/2026 09:00")); // inicio
        assertTrue(texto.contains("24/11/2026 12:00")); // hora limite (inicio + 3 h)
        assertTrue(texto.contains("Pendiente"));        // todavia no se devuelve
    }

    @Test
    void elComprobanteDeUnPrestamoDevueltoMuestraLaDevolucionYElEstadoFisico() throws IOException {
        Prestamo p = prestamo("devuelto");
        p.setFechaDevolucionReal(INICIO.plusHours(2).plusMinutes(15));
        p.setEstadoFisicoDevolucion("dañado");
        p.setAmpliado(true);
        when(prestamoDao.findById(12)).thenReturn(Optional.of(p));

        String texto = textoDe(servicio.comprobantePrestamo(12));

        assertTrue(texto.contains("24/11/2026 11:15"));
        assertTrue(texto.contains("dañado"));
        assertTrue(texto.contains("devuelto"));
        assertFalse(texto.contains("Pendiente"));
        // las reglas de la ampliacion solo sirven mientras el libro esta en sala
        assertTrue(texto.contains("Préstamo terminado"));
        assertFalse(texto.contains("ampliar"));
    }

    @Test
    void elComprobanteDeUnPrestamoEnCursoRecuerdaLasReglas() throws IOException {
        when(prestamoDao.findById(12)).thenReturn(Optional.of(prestamo("vencido")));

        String texto = textoDe(servicio.comprobantePrestamo(12));

        assertTrue(texto.contains("Pendiente"));
        assertTrue(texto.contains("sin pasar de las 20:00"));
        assertTrue(texto.contains("ampliar una sola vez"));
    }

    @Test
    void elComprobanteDeUnPrestamoAnuladoLoDice() throws IOException {
        when(prestamoDao.findById(12)).thenReturn(Optional.of(prestamo("anulado")));

        String texto = textoDe(servicio.comprobantePrestamo(12));

        assertTrue(texto.contains("anulado"));
        assertTrue(texto.contains("eliminación lógica"));
    }

    @Test
    void unPrestamoQueNoExisteDa404() {
        when(prestamoDao.findById(99)).thenReturn(Optional.empty());

        NegocioException error = assertThrows(NegocioException.class, () -> servicio.comprobantePrestamo(99));
        assertEquals(404, error.getEstado().value());
        assertEquals("Préstamo no encontrado", error.getMessage());
    }

    // ---------- Constancia de la reserva ----------

    @Test
    void laConstanciaDeUnaReservaPendienteDiceHastaCuandoRecogerla() throws IOException {
        when(reservaDao.findById(5)).thenReturn(Optional.of(reserva("pendiente", INICIO.plusDays(1).withHour(11).withMinute(30))));

        byte[] pdf = servicio.constanciaReserva(5);

        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
        String texto = textoDe(pdf);
        assertTrue(texto.contains("Constancia de reserva N° 5"));
        assertTrue(texto.contains("Emitido: 24/11/2026 10:30"));
        assertTrue(texto.contains("Valeria Flores"));
        assertTrue(texto.contains("73456783"));
        assertTrue(texto.contains("Rayuela"));
        assertTrue(texto.contains("Recoger hasta"));
        assertTrue(texto.contains("25/11/2026 11:30"));
    }

    @Test
    void laConstanciaDeUnaReservaEnColaDiceSuPuesto() throws IOException {
        Reserva r = reserva("en_espera", null);
        r.setPosicionCola(2);
        when(reservaDao.findById(5)).thenReturn(Optional.of(r));

        String texto = textoDe(servicio.constanciaReserva(5));

        assertTrue(texto.contains("En cola de espera"));
        assertTrue(texto.contains("Puesto en la cola"));
        assertFalse(texto.contains("Recoger hasta"));
    }

    @Test
    void unaReservaQueNoExisteDa404() {
        when(reservaDao.findById(99)).thenReturn(Optional.empty());

        NegocioException error = assertThrows(NegocioException.class, () -> servicio.constanciaReserva(99));
        assertEquals(404, error.getEstado().value());
        assertEquals("Reserva no encontrada", error.getMessage());
    }

    // ---------- Datos de prueba ----------

    private Prestamo prestamo(String estado) {
        Prestamo p = new Prestamo();
        p.setIdPrestamo(12);
        p.setPrestatario(usuario("Valeria", "Flores", "73456783"));
        p.setEncargado(usuario("Carlos", "Librero", "44444444"));
        p.setLibro(libro());
        p.setFechaPrestamo(INICIO);
        p.setFechaDevolucionPrevista(INICIO.plusHours(3));
        p.setEstadoPrestamo(estado);
        return p;
    }

    private Reserva reserva(String estado, LocalDateTime recojo) {
        Reserva r = new Reserva(usuario("Valeria", "Flores", "73456783"), libro(), INICIO, recojo, estado);
        r.setIdReserva(5);
        return r;
    }

    private Usuario usuario(String nombre, String apellido, String dni) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setDni(dni);
        return u;
    }

    private Libro libro() {
        Categoria categoria = new Categoria();
        categoria.setNombre("Arte y Cultura");
        Libro l = new Libro();
        l.setTitulo("Rayuela");
        l.setIsbn("9788437604572");
        l.setCategoria(categoria);
        return l;
    }

    /** Texto de todas las páginas del PDF (OpenPDF lo lee con PdfTextExtractor). */
    private String textoDe(byte[] pdf) throws IOException {
        try (PdfReader lector = new PdfReader(pdf)) {
            PdfTextExtractor extractor = new PdfTextExtractor(lector);
            StringBuilder texto = new StringBuilder();
            for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
                texto.append(extractor.getTextFromPage(pagina)).append('\n');
            }
            return texto.toString();
        }
    }
}
