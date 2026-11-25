package com.alejandria.biblioteca.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Respuesta para que el navegador descargue un archivo: reportes, comprobantes y constancias. */
// D30: antes cada controller armaba esta respuesta por su cuenta (y ReporteController escribia la cabecera a mano)
final class Descarga {

    private Descarga() {}

    // ContentDisposition de Spring arma la cabecera q le dice al navegador "descarga este archivo con este nombre"
    static ResponseEntity<byte[]> archivo(byte[] contenido, String nombre, MediaType tipo) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .contentType(tipo)
                .body(contenido);
    }
}
