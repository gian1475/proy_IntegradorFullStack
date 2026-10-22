package com.alejandria.biblioteca.seguridad;

import com.alejandria.biblioteca.exception.NegocioException;
import org.springframework.http.HttpStatus;

import java.util.regex.Pattern;

/**
 * Valida los datos de un usuario antes de guardarlo (registro y CRUD de usuarios).
 * Los límites son los mismos de la tabla usuario, para que la BD nunca rechace el dato.
 */
public final class ValidadorUsuario {

    // validar datos: dni de exactamente 8 numeros (la columna dni es VARCHAR(8))
    private static final Pattern DNI = Pattern.compile("^\\d{8}$");
    // validar datos: algo@algo.algo, sin espacios
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static final int MAX_NOMBRE = 50;   // nombre y apellido: VARCHAR(50)
    private static final int MAX_CORREO = 100;  // correo: VARCHAR(100)

    private ValidadorUsuario() {}

    public static void validar(String nombre, String apellido, String dni, String correo) {
        validarTexto(nombre, "El nombre");
        validarTexto(apellido, "El apellido");
        if (dni == null || !DNI.matcher(dni.trim()).matches()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "El DNI debe tener 8 dígitos");
        }
        if (correo == null || !CORREO.matcher(correo.trim()).matches()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "El correo no es válido");
        }
        if (correo.trim().length() > MAX_CORREO) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "El correo no puede tener más de " + MAX_CORREO + " caracteres");
        }
    }

    private static void validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank() || valor.trim().length() > MAX_NOMBRE) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, campo + " debe tener entre 1 y " + MAX_NOMBRE + " caracteres");
        }
    }
}
