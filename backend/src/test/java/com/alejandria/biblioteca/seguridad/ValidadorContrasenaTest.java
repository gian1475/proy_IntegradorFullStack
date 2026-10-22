package com.alejandria.biblioteca.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Regla de contraseña")
class ValidadorContrasenaTest {

    @ParameterizedTest(name = "\"{0}\" es válida")
    @ValueSource(strings = {"Abcdefg1!", "Biblioteca#2026", "Ñandú#2026", "xY9$xxxx"})
    void aceptaContrasenasQueCumplenTodo(String contrasena) {
        assertTrue(ValidadorContrasena.esValida(contrasena));
    }

    @ParameterizedTest(name = "\"{0}\" es inválida")
    @ValueSource(strings = {
        "Ab1!",          // menos de 8 caracteres
        "abcdefg1!",     // sin mayúscula
        "ABCDEFG1!",     // sin minúscula
        "Abcdefgh!",     // sin número
        "Abcdefg12",     // sin símbolo
        "Abc 1234",      // el espacio no cuenta como símbolo
        "123456"         // la contraseña antigua de los usuarios de prueba
    })
    void rechazaContrasenasQueNoCumplen(String contrasena) {
        assertFalse(ValidadorContrasena.esValida(contrasena));
    }

    @Test
    void rechazaMasDe64Caracteres() {
        assertFalse(ValidadorContrasena.esValida("Aa1!" + "x".repeat(61))); // 65 caracteres
        assertTrue(ValidadorContrasena.esValida("Aa1!" + "x".repeat(60)));  // 64 caracteres
    }

    @Test
    void rechazaNulo() {
        assertFalse(ValidadorContrasena.esValida(null));
    }
}
