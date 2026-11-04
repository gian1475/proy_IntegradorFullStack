package com.alejandria.biblioteca.seguridad;

import com.alejandria.biblioteca.service.SesionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Spring Security de punta a punta: la petición pasa por el filtro del token, por @RolesAllowed
 * y por el ManejadorErrores, igual que cuando la manda el frontend.
 */
@DisplayName("Spring Security: 401 sin sesión, 403 sin permiso y validación de datos")
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadEndpointsTest {

    @Autowired private MockMvc mvc;
    @Autowired private SesionService sesionService;

    private String token(String rol) {
        return "Bearer " + sesionService.crear(1, rol).token();
    }

    @Test
    void sinTokenUnEndpointProtegidoDa401() throws Exception {
        mvc.perform(get("/api/prestamos/activos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Sesión no válida o expirada. Inicie sesión nuevamente"));
    }

    @Test
    void conUnTokenQueNoExisteDa401() throws Exception {
        mvc.perform(get("/api/prestamos/activos").header("Authorization", "Bearer token-inventado"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unPrestatarioNoPuedeVerLosPrestamosActivosDa403() throws Exception {
        mvc.perform(get("/api/prestamos/activos").header("Authorization", token(Rol.PRESTATARIO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tiene permisos para realizar esta acción"));
    }

    @Test
    void elEncargadoSiPuedeVerLosPrestamosActivos() throws Exception {
        mvc.perform(get("/api/prestamos/activos").header("Authorization", token(Rol.ENCARGADO)))
                .andExpect(status().isOk());
    }

    @Test
    void elRolDelControllerCompletoTambienSeRespeta() throws Exception {
        // UsuarioController tiene @RolesAllowed(ADMINISTRADOR) en la clase, no en cada método
        mvc.perform(get("/api/usuarios/encargados").header("Authorization", token(Rol.ENCARGADO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void soloElAdminEditaYDaDeBajaUsuarios() throws Exception {
        // RF12 y RF13: actor Administrador
        mvc.perform(get("/api/usuarios").header("Authorization", token(Rol.ENCARGADO)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/usuarios/2").header("Authorization", token(Rol.ENCARGADO)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/usuarios/2").header("Authorization", token(Rol.PRESTATARIO)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/usuarios/2"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void editarUnUsuarioConDniMalEscritoDa400() throws Exception {
        String json = """
                {"nombre":"Ana","apellido":"Torres","dni":"71","correo":"ana@alejandria.edu.pe"}
                """;
        mvc.perform(put("/api/usuarios/2").header("Authorization", token(Rol.ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El DNI debe tener 8 dígitos"));
    }

    @Test
    void elEncargadoNoEditaReservas() throws Exception {
        // RF15: actor Prestatario y Administrador
        mvc.perform(put("/api/reservas/1").header("Authorization", token(Rol.ENCARGADO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fechaRecojoLimite\":\"2026-10-30T17:30:00\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void editarUnaReservaSinFechaDa400() throws Exception {
        mvc.perform(put("/api/reservas/1").header("Authorization", token(Rol.ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Elija la fecha y la franja de recojo"));
    }

    @Test
    void laAuditoriaDelInventarioEsSoloDelAdmin() throws Exception {
        // IPI: la auditoria fisica la registra el administrador
        mvc.perform(get("/api/auditorias").header("Authorization", token(Rol.ENCARGADO)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/auditorias").header("Authorization", token(Rol.PRESTATARIO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unConteoFisicoNegativoDa400() throws Exception {
        mvc.perform(post("/api/auditorias").header("Authorization", token(Rol.ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"idLibro\":1,\"conteoFisico\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El conteo físico no puede ser negativo"));
    }

    @Test
    void elAdminTambienCancelaReservas() throws Exception {
        // B1 (RF16 y CU05): los tres perfiles cancelan. una reserva que no existe da 404 (paso el permiso), no 403
        mvc.perform(put("/api/reservas/999999/cancelar").header("Authorization", token(Rol.ADMINISTRADOR)))
                .andExpect(status().isNotFound());
    }

    @Test
    void soloElEncargadoReservaEnVentanilla() throws Exception {
        // B2 (RF04): el encargado reserva a nombre de un prestatario con su DNI
        String json = """
                {"dniPrestatario":"12","idLibro":1,"fechaRecojoLimite":"2026-10-30T17:30:00"}
                """;
        // con datos validos: el prestatario no tiene permiso (los datos se revisan antes que el permiso del metodo)
        mvc.perform(post("/api/reservas/ventanilla").header("Authorization", token(Rol.PRESTATARIO))
                        .contentType(MediaType.APPLICATION_JSON).content(json.replace("\"12\"", "\"33333333\"")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/reservas/ventanilla").header("Authorization", token(Rol.ENCARGADO))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El DNI debe tener 8 dígitos"));
    }

    @Test
    void soloElEncargadoAnulaPrestamos() throws Exception {
        // B8 (Tabla 35 del informe): anular es del encargado. el admin solo consulta
        mvc.perform(delete("/api/prestamos/999999").header("Authorization", token(Rol.ADMINISTRADOR)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/prestamos/999999").header("Authorization", token(Rol.ENCARGADO)))
                .andExpect(status().isNotFound()); // paso el permiso: el prestamo no existe
    }

    @Test
    void soloElAdminRestableceContrasenas() throws Exception {
        // B7: el admin pone una contraseña nueva a quien la olvido (ventanilla)
        String json = """
                {"contrasena":"Biblioteca#2026"}
                """;
        mvc.perform(put("/api/usuarios/2/contrasena").header("Authorization", token(Rol.ENCARGADO))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    void unPrestatarioNoPuedeVerLosPrestamosDeOtro() throws Exception {
        // el token es del usuario 1; pide los prestamos del usuario 2
        mvc.perform(get("/api/prestamos/prestatario/2").header("Authorization", token(Rol.PRESTATARIO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo puede consultar sus propios préstamos"));
    }

    @Test
    void elCatalogoEsPublico() throws Exception {
        mvc.perform(get("/api/categorias")).andExpect(status().isOk());
    }

    @Test
    void elFrontendPuedeLlamarALaApiCors() throws Exception {
        mvc.perform(options("/api/prestamos/activos")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test
    void otraPaginaNoPuedeLlamarALaApiCors() throws Exception {
        mvc.perform(options("/api/prestamos/activos")
                        .header("Origin", "http://pagina-ajena.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unRegistroConDniMalEscritoDa400ConElMensajeDeLaAnotacion() throws Exception {
        String json = """
                {"nombre":"Juan","apellido":"Pérez","dni":"123","correo":"juan@alejandria.edu.pe","contrasena":"Biblioteca#2026"}
                """;
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El DNI debe tener 8 dígitos"));
    }

    @Test
    void lasIncidenciasSonSoloDelEncargado() throws Exception {
        // RF06 y CU09: actor Encargado (el admin las ve en el reporte de incidencias y sanciones)
        mvc.perform(get("/api/incidencias").header("Authorization", token(Rol.ADMINISTRADOR)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/incidencias").header("Authorization", token(Rol.PRESTATARIO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unaIncidenciaConUnTipoInventadoDa400() throws Exception {
        String json = """
                {"idPrestamo":1,"tipoIncidencia":"robo","tipoSancion":"advertencia"}
                """;
        mvc.perform(post("/api/incidencias").header("Authorization", token(Rol.ENCARGADO))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El tipo de incidencia debe ser retraso, daño o pérdida"));
    }

    @Test
    void unPrestamoSinDatosDa400() throws Exception {
        mvc.perform(post("/api/prestamos").header("Authorization", token(Rol.ENCARGADO))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Debe indicar el DNI del prestatario y el libro"));
    }
}
