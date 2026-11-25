package com.alejandria.biblioteca.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * B5 (despliegue): un solo .jar sirve la API y las pantallas de Angular. Si alguien abre o recarga /admin,
 * Spring Boot entrega index.html y Angular dibuja la pantalla. El origen de CORS se puede cambiar sin tocar
 * el código (aquí se prueba con otro valor, como en un servidor en la nube).
 */
@DisplayName("Despliegue: rutas de Angular y CORS configurable (B5)")
@SpringBootTest(properties = "app.cors.origenes=http://localhost:4200,https://biblioteca-prueba.onrender.com")
@AutoConfigureMockMvc
class DespliegueTest {

    @Autowired private MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {"/login", "/registro", "/catalogo", "/admin", "/encargado"})
    void lasRutasDeAngularEntreganIndexHtml(String ruta) throws Exception {
        mvc.perform(get(ruta))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    // D25: en un contenedor (Render, 512 MB) java toma por defecto solo 1/4 de la memoria para el heap (~128 MB),
    // y el sistema ya usa unos 240 MB al arrancar. el Dockerfile tiene q darle mas (MaxRAMPercentage)
    @Test
    void elDockerfileLeDaAJavaLaMayorParteDeLaMemoriaDelContenedor() throws Exception {
        String dockerfile = Files.readString(Path.of("..", "Dockerfile")); // las pruebas corren dentro de backend/
        String entrada = dockerfile.lines().filter(l -> l.startsWith("ENTRYPOINT")).findFirst().orElseThrow();
        assertTrue(entrada.contains("\"-XX:MaxRAMPercentage=75\""), "ENTRYPOINT: " + entrada);
        assertTrue(entrada.indexOf("MaxRAMPercentage") < entrada.indexOf("-jar"),
                "la opcion de java va antes de -jar (despues seria un argumento del sistema)");
    }

    @Test
    void elOrigenDeCorsSeConfiguraSinTocarElCodigo() throws Exception {
        mvc.perform(options("/api/libros")
                        .header("Origin", "https://biblioteca-prueba.onrender.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://biblioteca-prueba.onrender.com"));
    }
}
