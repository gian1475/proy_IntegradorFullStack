package com.alejandria.biblioteca.controller;

import com.alejandria.biblioteca.service.DashboardService;
import com.alejandria.biblioteca.service.LibroService;
import com.alejandria.biblioteca.service.ReporteService;
import com.alejandria.biblioteca.service.ReservaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

/**
 * D26: el catálogo mostraba un libro "reservado" con stock 0 aunque su reserva ya había vencido (visto el 17/11
 * con "Cien Años de Soledad"), hasta que alguien prestaba o reservaba. Lo mismo podía pasar en el reporte de stock,
 * en el dashboard y en "Libros por categoría". Ahora, antes de mostrar stock o estado, las reservas vencidas
 * liberan su ejemplar (ReservaService.liberarVencidas): lo que se ve es lo de ese momento.
 */
@DisplayName("Stock y estado al día: las reservas vencidas liberan su ejemplar antes de mostrar (D26)")
@ExtendWith(MockitoExtension.class)
class ReservasAlDiaTest {

    @Mock private LibroService libroService;
    @Mock private ReservaService reservaService;
    @Mock private ReporteService reporteService;
    @Mock private DashboardService dashboardService;

    @Test
    void elCatalogoVenceLasReservasAntesDeListar() {
        new LibroController(libroService, reservaService).listarTodos();

        InOrder orden = inOrder(reservaService, libroService);
        orden.verify(reservaService).liberarVencidas();
        orden.verify(libroService).listar();
    }

    @Test
    void laBusquedaElAutorYElDetalleDelLibroTambien() {
        LibroController controller = new LibroController(libroService, reservaService);
        controller.buscar("rayuela");
        controller.buscarPorAutor("cortazar");
        controller.obtenerPorId(16);

        InOrder orden = inOrder(reservaService, libroService);
        orden.verify(reservaService).liberarVencidas();
        orden.verify(libroService).buscar("rayuela");
        orden.verify(reservaService).liberarVencidas();
        orden.verify(libroService).buscarPorAutor("cortazar");
        orden.verify(reservaService).liberarVencidas();
        orden.verify(libroService).obtener(16);
    }

    @Test
    void losReportesVencenLasReservasAntesDeArmarse() {
        // el reporte de stock y el de reservas tienen que salir con los datos de ahora
        when(reporteService.generar(any(), any(), any(), any())).thenReturn(new byte[] {1});
        new ReporteController(reporteService, reservaService).descargar("stock", "pdf", null, LocalDate.now());

        InOrder orden = inOrder(reservaService, reporteService);
        orden.verify(reservaService).liberarVencidas();
        orden.verify(reporteService).generar("stock", "pdf", null, LocalDate.now());
    }

    @Test
    void elDashboardYLosLibrosPorCategoriaTambien() {
        new DashboardController(dashboardService, reservaService).obtener();
        new CatalogoController(libroService, reservaService).librosPorCategoria("");

        InOrder orden = inOrder(reservaService, dashboardService, libroService);
        orden.verify(reservaService).liberarVencidas();
        orden.verify(dashboardService).obtener();
        orden.verify(reservaService).liberarVencidas();
        orden.verify(libroService).consultarConCategoria("");
    }
}
