package com.alejandria.biblioteca.dto;

import com.alejandria.biblioteca.dto.auth.RegistroDTO;
import com.alejandria.biblioteca.dto.libro.LibroFormDTO;
import com.alejandria.biblioteca.dto.prestamo.DevolucionDTO;
import com.alejandria.biblioteca.dto.usuario.ActualizarUsuarioDTO;
import com.alejandria.biblioteca.dto.usuario.RegistroEncargadoDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bean Validation: las reglas de los datos están como anotaciones en los DTOs (@NotBlank, @Pattern, @Size...).
 * Aquí se revisan con el mismo Validator que usa Spring cuando el controller tiene @Valid.
 * Reemplaza a la prueba de la clase ValidadorUsuario, que se borró.
 */
@DisplayName("Validación de datos con anotaciones (Bean Validation)")
class ValidacionDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private RegistroDTO registro(String nombre, String apellido, String dni, String correo) {
        return new RegistroDTO(nombre, apellido, dni, correo, "Biblioteca#2026");
    }

    /** Los mensajes de los datos que no pasan (vacío si todo está bien). */
    private Set<String> errores(Object dto) {
        return validator.validate(dto).stream().map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void aceptaDatosCorrectos() {
        assertTrue(errores(registro("Juan", "Pérez", "12345678", "juan@alejandria.edu.pe")).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567", "123456789", "1234567A", "ABCDEFGH", ""})
    void rechazaDniQueNoTieneOchoDigitos(String dni) {
        assertEquals(Set.of("El DNI debe tener 8 dígitos"),
                errores(registro("Juan", "Pérez", dni, "juan@alejandria.edu.pe")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"juan", "juan@", "@alejandria.edu.pe", "juan alejandria@edu.pe", "juan@alejandria"})
    void rechazaCorreoMalEscrito(String correo) {
        assertEquals(Set.of("El correo no es válido"), errores(registro("Juan", "Pérez", "12345678", correo)));
    }

    @Test
    void rechazaNombreDeMasDeCincuentaCaracteres() {
        assertFalse(errores(registro("a".repeat(51), "Pérez", "12345678", "juan@alejandria.edu.pe")).isEmpty());
    }

    @Test
    void rechazaApellidoVacio() {
        assertEquals(Set.of("El apellido debe tener entre 1 y 50 caracteres"),
                errores(registro("Juan", "   ", "12345678", "juan@alejandria.edu.pe")));
    }

    @Test
    void rechazaCorreoDeMasDeCienCaracteres() {
        assertEquals(Set.of("El correo no puede tener más de 100 caracteres"),
                errores(registro("Juan", "Pérez", "12345678", "a".repeat(95) + "@x.com")));
    }

    @Test
    void rechazaDatosQueFaltan() {
        assertFalse(errores(new RegistroDTO(null, null, null, null, null)).isEmpty());
    }

    @Test
    void elEncargadoTieneLasMismasReglasQueElPrestatario() {
        RegistroEncargadoDTO dto = new RegistroEncargadoDTO("María", "López", "1234",
                "maria@alejandria.edu.pe", "Biblioteca#2026", "tarde");
        assertEquals(Set.of("El DNI debe tener 8 dígitos"), errores(dto));
    }

    @Test
    void alEditarUnUsuarioSeUsanLasMismasReglasDelRegistro() {
        // RF12: el admin edita nombres, DNI y correo (sin la contraseña)
        assertTrue(errores(new ActualizarUsuarioDTO("Ana", "Torres", "71234567", "ana@alejandria.edu.pe", null)).isEmpty());
        assertEquals(Set.of("El DNI debe tener 8 dígitos"),
                errores(new ActualizarUsuarioDTO("Ana", "Torres", "7123", "ana@alejandria.edu.pe", null)));
        assertEquals(Set.of("El correo no es válido"),
                errores(new ActualizarUsuarioDTO("Ana", "Torres", "71234567", "ana@", null)));
        assertEquals(Set.of("El nombre debe tener entre 1 y 50 caracteres"),
                errores(new ActualizarUsuarioDTO(" ", "Torres", "71234567", "ana@alejandria.edu.pe", null)));
    }

    @Test
    void elLibroNoAceptaStockNegativoNiAnioFueraDeRango() {
        assertEquals(Set.of("El stock no puede ser negativo"),
                errores(new LibroFormDTO("Clean Code", "9780132350884", 2008, "Prentice Hall", -1, 1, null)));
        assertEquals(Set.of("El año de publicación no es válido"),
                errores(new LibroFormDTO("Clean Code", "9780132350884", 999, "Prentice Hall", 1, 1, null)));
    }

    @Test
    void elLibroSinTituloNoPasa() {
        assertEquals(Set.of("Complete todos los campos obligatorios del libro"),
                errores(new LibroFormDTO(" ", "9780132350884", 2008, "Prentice Hall", 1, 1, null)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"roto", "Bueno", ""})
    void laDevolucionSoloAceptaBuenoODaniado(String estado) {
        assertEquals(Set.of("El estado físico debe ser 'bueno' o 'dañado'"), errores(new DevolucionDTO(estado)));
    }

    @Test
    void laDevolucionAceptaBuenoYDaniado() {
        assertTrue(errores(new DevolucionDTO("bueno")).isEmpty());
        assertTrue(errores(new DevolucionDTO("dañado")).isEmpty());
    }
}
