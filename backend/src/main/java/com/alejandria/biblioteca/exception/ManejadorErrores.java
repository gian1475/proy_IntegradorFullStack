package com.alejandria.biblioteca.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/**
 * Convierte los errores en respuestas JSON {"mensaje": "..."} para el frontend.
 * Así ningún error llega a la pantalla sin un mensaje entendible.
 */
// manejo de errores: aqui caen todos los errores del backend y se responden con un mensaje
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    /** Reglas del negocio (préstamo sin stock, DNI repetido, etc.): se respeta su código y su mensaje. */
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Map<String, String>> manejarNegocio(NegocioException e) {
        return responder(e.getEstado(), e.getMessage());
    }

    /** El JSON que mandó el frontend no se puede leer (por ejemplo, una fecha mal escrita). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> manejarDatosIlegibles(HttpMessageNotReadableException e) {
        return responder(HttpStatus.BAD_REQUEST, "Los datos enviados tienen un formato incorrecto");
    }

    /** Un dato de la dirección no tiene el tipo esperado (por ejemplo, /api/libros/abc). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> manejarParametro(MethodArgumentTypeMismatchException e) {
        return responder(HttpStatus.BAD_REQUEST, "El dato '" + e.getName() + "' no es válido");
    }

    /** La BD rechazó el dato (un valor repetido, muy largo o que no cumple un CHECK). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> manejarBd(DataIntegrityViolationException e) {
        log.warn("La BD rechazó un dato: {}", e.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, "La base de datos rechazó el dato: puede estar repetido o no tener el formato permitido");
    }

    /** Cualquier otro error. Al usuario no se le muestra el detalle técnico; ese queda en el log. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> manejarCualquierOtro(Exception e) {
        // los errores propios de spring (ruta que no existe, metodo no permitido...) mantienen su codigo
        if (e instanceof ErrorResponse errorSpring) {
            HttpStatus estado = HttpStatus.valueOf(errorSpring.getStatusCode().value());
            String mensaje = estado == HttpStatus.NOT_FOUND ? "No se encontró la dirección solicitada" : "Solicitud no válida";
            return responder(estado, mensaje);
        }
        // logback: el error completo se guarda en logs/biblioteca.log para revisarlo despues
        log.error("Error inesperado", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error en el servidor. Intente nuevamente");
    }

    private ResponseEntity<Map<String, String>> responder(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("mensaje", mensaje));
    }
}
