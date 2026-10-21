package com.backend.demo.exception;

import org.springframework.http.HttpStatus;

/** Error de una regla del negocio, con el código HTTP que debe devolverse. */
public class NegocioException extends RuntimeException {

    private final HttpStatus estado;

    public NegocioException(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() { return estado; }
}
