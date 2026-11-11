package com.alejandria.biblioteca.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Usuario: nombre completo para las pantallas y los reportes")
class UsuarioTest {

    @Test
    void elNombreCompletoEsNombreYApellidoSeparadosPorUnEspacio() {
        Usuario lucia = new Usuario(7, "Lucía", "Ramírez Soto", "71234567", "lucia@x.pe", "x", "activo");

        assertEquals("Lucía Ramírez Soto", lucia.nombreCompleto());
    }
}
