package com.backend.demo.seguridad;

import java.util.regex.Pattern;

/**
 * Regla de contraseña: de 8 a 64 caracteres, con al menos una minúscula,
 * una mayúscula, un número y un símbolo especial (cualquier caracter que no
 * sea letra, número ni espacio).
 */
public final class ValidadorContrasena {

    public static final String MENSAJE_REGLA =
            "La contraseña debe tener entre 8 y 64 caracteres, una minúscula, una mayúscula, un número y un símbolo especial";

    private static final Pattern PATRON = Pattern.compile(
            "^(?=.*\\p{Ll})"         // al menos una minúscula
          + "(?=.*\\p{Lu})"          // al menos una mayúscula
          + "(?=.*\\d)"              // al menos un número
          + "(?=.*[^\\p{L}\\d\\s])"  // al menos un símbolo especial
          + ".{8,64}$"               // longitud entre 8 y 64
    );

    private ValidadorContrasena() {}

    public static boolean esValida(String contrasena) {
        return contrasena != null && PATRON.matcher(contrasena).matches();
    }
}
