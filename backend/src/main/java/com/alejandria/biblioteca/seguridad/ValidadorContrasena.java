package com.alejandria.biblioteca.seguridad;

import java.util.regex.Pattern;

/**
 * Regla de contraseña: de 8 a 64 caracteres, con al menos una minúscula,
 * una mayúscula, un número y un símbolo especial (cualquier caracter que no
 * sea letra, número ni espacio).
 */
public final class ValidadorContrasena {

    public static final String MENSAJE_REGLA =
            "La contraseña debe tener entre 8 y 64 caracteres, una minúscula, una mayúscula, un número y un símbolo especial";

    // regla contra: aqui estan los 5 criterios q pidio el profe, todos en una sola expresion regular
    private static final Pattern PATRON = Pattern.compile(
            "^(?=.*\\p{Ll})"         // criterio 2: minimo una letra minuscula
          + "(?=.*\\p{Lu})"          // criterio 3: minimo una letra mayuscula
          + "(?=.*\\d)"              // criterio 4: minimo un digito
          + "(?=.*[^\\p{L}\\d\\s])"  // criterio 5: minimo un simbolo especial (@ # $ % ! etc)
          + ".{8,64}$"               // criterio 1: minimo 8 caracteres (max 64 xq bcrypt no aguanta mas de 72 bytes)
    );

    private ValidadorContrasena() {}

    // regla contra: devuelve true solo si cumple los 5 criterios. se llama en AuthService.registro
    public static boolean esValida(String contrasena) {
        return contrasena != null && PATRON.matcher(contrasena).matches();
    }
}
