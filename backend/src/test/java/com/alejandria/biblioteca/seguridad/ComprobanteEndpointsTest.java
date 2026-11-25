package com.alejandria.biblioteca.seguridad;

import com.alejandria.biblioteca.service.SesionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Semana 15: el comprobante del préstamo y la constancia de la reserva, de punta a punta
 * (token, @RolesAllowed, "solo lo suyo" para el prestatario y el PDF que llega al navegador).
 * Usa la base de datos real: busca un préstamo y una reserva que ya existan (solo lee, no cambia nada).
 */
@DisplayName("Comprobante y constancia en PDF: quién los descarga (semana 15)")
@SpringBootTest
@AutoConfigureMockMvc
class ComprobanteEndpointsTest {

    @Autowired private MockMvc mvc;
    @Autowired private SesionService sesionService;
    @Autowired private JdbcTemplate jdbc;

    /** Sesión de un usuario con ese id y ese rol (como si hubiera iniciado sesión). */
    private String token(Integer idUsuario, String rol) {
        return "Bearer " + sesionService.crear(idUsuario, rol).token();
    }

    /** Un préstamo cualquiera con su prestatario: [id del préstamo, id del prestatario]. */
    private Map<String, Object> unPrestamo() {
        List<Map<String, Object>> filas = jdbc.queryForList(
                "SELECT id_prestamo AS id, id_prestatario AS dueno FROM prestamo ORDER BY id_prestamo LIMIT 1");
        assumeFalse(filas.isEmpty(), "La base de datos no tiene préstamos");
        return filas.get(0);
    }

    private Map<String, Object> unaReserva() {
        List<Map<String, Object>> filas = jdbc.queryForList(
                "SELECT id_reserva AS id, id_prestatario AS dueno FROM reserva ORDER BY id_reserva LIMIT 1");
        assumeFalse(filas.isEmpty(), "La base de datos no tiene reservas");
        return filas.get(0);
    }

    /** Un id que no es el del dueño (para probar el 403). */
    private Integer otroUsuario(Map<String, Object> fila) {
        Integer dueno = (Integer) fila.get("dueno");
        return dueno + 1000;
    }

    // ---------- Comprobante del préstamo ----------

    @Test
    void sinSesionNoSeDescargaElComprobante() throws Exception {
        mvc.perform(get("/api/prestamos/1/comprobante"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elEncargadoDescargaElComprobanteEnPdf() throws Exception {
        Integer id = (Integer) unPrestamo().get("id");
        mvc.perform(get("/api/prestamos/" + id + "/comprobante").header("Authorization", token(1, Rol.ENCARGADO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("comprobante_prestamo_" + id + ".pdf")));
    }

    @Test
    void elPrestatarioDescargaElComprobanteDeSuPrestamo() throws Exception {
        Map<String, Object> p = unPrestamo();
        mvc.perform(get("/api/prestamos/" + p.get("id") + "/comprobante")
                        .header("Authorization", token((Integer) p.get("dueno"), Rol.PRESTATARIO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void unPrestatarioNoDescargaElComprobanteDeOtro() throws Exception {
        Map<String, Object> p = unPrestamo();
        mvc.perform(get("/api/prestamos/" + p.get("id") + "/comprobante")
                        .header("Authorization", token(otroUsuario(p), Rol.PRESTATARIO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo puede descargar el comprobante de sus propios préstamos"));
    }

    @Test
    void elComprobanteDeUnPrestamoQueNoExisteDa404() throws Exception {
        mvc.perform(get("/api/prestamos/999999/comprobante").header("Authorization", token(1, Rol.ADMINISTRADOR)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Préstamo no encontrado"));
    }

    // ---------- Constancia de la reserva ----------

    @Test
    void elAdminYElEncargadoDescarganLaConstanciaEnPdf() throws Exception {
        Integer id = (Integer) unaReserva().get("id");
        for (String rol : List.of(Rol.ADMINISTRADOR, Rol.ENCARGADO)) {
            mvc.perform(get("/api/reservas/" + id + "/constancia").header("Authorization", token(1, rol)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(header().string("Content-Disposition", containsString("constancia_reserva_" + id + ".pdf")));
        }
    }

    @Test
    void elPrestatarioDescargaLaConstanciaDeSuReserva() throws Exception {
        Map<String, Object> r = unaReserva();
        mvc.perform(get("/api/reservas/" + r.get("id") + "/constancia")
                        .header("Authorization", token((Integer) r.get("dueno"), Rol.PRESTATARIO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void unPrestatarioNoDescargaLaConstanciaDeOtro() throws Exception {
        Map<String, Object> r = unaReserva();
        mvc.perform(get("/api/reservas/" + r.get("id") + "/constancia")
                        .header("Authorization", token(otroUsuario(r), Rol.PRESTATARIO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo puede gestionar sus propias reservas"));
    }

    @Test
    void laConstanciaDeUnaReservaQueNoExisteDa404() throws Exception {
        mvc.perform(get("/api/reservas/999999/constancia").header("Authorization", token(1, Rol.ENCARGADO)))
                .andExpect(status().isNotFound());
    }
}
