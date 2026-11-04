package com.alejandria.biblioteca.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * B5 (despliegue): el .jar también trae las pantallas de Angular (carpeta static/, ver el pom.xml).
 * Angular cambia de pantalla sin pedir otra página, pero si alguien recarga o escribe /admin en la barra,
 * el navegador se la pide al servidor: aquí se le entrega index.html y Angular dibuja la pantalla.
 */
// despliegue: rutas de angular (app.routes.ts). la api sigue en /api/**
@Controller
public class FrontendController {

    @GetMapping({"/login", "/registro", "/catalogo", "/admin", "/encargado"})
    public String pantallaDeAngular() {
        return "forward:/index.html";
    }
}
