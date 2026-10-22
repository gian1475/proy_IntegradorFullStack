package com.alejandria.biblioteca.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Todos los errores llegan al frontend con un mensaje")
class ManejadorErroresTest {

    private final ManejadorErrores manejador = new ManejadorErrores();

    @Test
    void unErrorDelNegocioMantieneSuCodigoYMensaje() {
        ResponseEntity<Map<String, String>> r = manejador.manejarNegocio(
                new NegocioException(HttpStatus.CONFLICT, "El libro tiene reservas pendientes"));
        assertEquals(409, r.getStatusCode().value());
        assertEquals("El libro tiene reservas pendientes", r.getBody().get("mensaje"));
    }

    @Test
    void unJsonMalEscritoDa400ConMensaje() {
        ResponseEntity<Map<String, String>> r = manejador.manejarDatosIlegibles(
                new HttpMessageNotReadableException("fecha mal escrita", new MockHttpInputMessage(new byte[0])));
        assertEquals(400, r.getStatusCode().value());
        assertNotNull(r.getBody().get("mensaje"));
    }

    @Test
    void unDatoQueLaBdRechazaDa409ConMensaje() {
        ResponseEntity<Map<String, String>> r = manejador.manejarBd(new DataIntegrityViolationException("dni muy largo"));
        assertEquals(409, r.getStatusCode().value());
        assertNotNull(r.getBody().get("mensaje"));
    }

    @Test
    void cualquierOtroErrorDa500SinMostrarDetallesInternos() {
        ResponseEntity<Map<String, String>> r = manejador.manejarCualquierOtro(new IllegalStateException("NullPointer en la línea 42"));
        assertEquals(500, r.getStatusCode().value());
        assertFalse(r.getBody().get("mensaje").contains("42"), "no debe mostrar el detalle técnico al usuario");
    }

    @Test
    void unaRutaQueNoExisteSigueDando404() {
        ResponseEntity<Map<String, String>> r = manejador.manejarCualquierOtro(
                new NoResourceFoundException(org.springframework.http.HttpMethod.GET, "/api/no-existe", "/api/no-existe"));
        assertEquals(404, r.getStatusCode().value());
    }
}
