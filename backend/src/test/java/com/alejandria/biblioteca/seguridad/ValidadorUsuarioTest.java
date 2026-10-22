package com.alejandria.biblioteca.seguridad;

import com.alejandria.biblioteca.exception.NegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Validación de los datos del usuario (registro y CRUD de usuarios)")
class ValidadorUsuarioTest {

    @Test
    void aceptaDatosCorrectos() {
        assertDoesNotThrow(() -> ValidadorUsuario.validar("Juan", "Pérez", "12345678", "juan@alejandria.edu.pe"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567", "123456789", "1234567A", "ABCDEFGH", ""})
    void rechazaDniQueNoTieneOchoDigitos(String dni) {
        NegocioException error = assertThrows(NegocioException.class,
                () -> ValidadorUsuario.validar("Juan", "Pérez", dni, "juan@alejandria.edu.pe"));
        assertEquals("El DNI debe tener 8 dígitos", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"juan", "juan@", "@alejandria.edu.pe", "juan alejandria@edu.pe", "juan@alejandria"})
    void rechazaCorreoMalEscrito(String correo) {
        NegocioException error = assertThrows(NegocioException.class,
                () -> ValidadorUsuario.validar("Juan", "Pérez", "12345678", correo));
        assertEquals("El correo no es válido", error.getMessage());
    }

    @Test
    void rechazaNombreDeMasDeCincuentaCaracteres() {
        String largo = "a".repeat(51);
        assertThrows(NegocioException.class,
                () -> ValidadorUsuario.validar(largo, "Pérez", "12345678", "juan@alejandria.edu.pe"));
    }

    @Test
    void rechazaApellidoVacio() {
        assertThrows(NegocioException.class,
                () -> ValidadorUsuario.validar("Juan", "   ", "12345678", "juan@alejandria.edu.pe"));
    }

    @Test
    void rechazaCorreoDeMasDeCienCaracteres() {
        String correo = "a".repeat(95) + "@x.com";
        assertThrows(NegocioException.class,
                () -> ValidadorUsuario.validar("Juan", "Pérez", "12345678", correo));
    }
}
