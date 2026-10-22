package com.alejandria.biblioteca.seguridad;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Configuración web: quién puede llamar a la API (CORS) y el interceptor de autorización. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Única dirección desde la que el navegador puede llamar a la API: el frontend Angular. */
    public static final String ORIGEN_FRONTEND = "http://localhost:4200";

    private final AutorizacionInterceptor autorizacionInterceptor;

    public WebConfig(AutorizacionInterceptor autorizacionInterceptor) {
        this.autorizacionInterceptor = autorizacionInterceptor;
    }

    // cors: solo el frontend propio (localhost:4200) puede llamar a la api. antes estaba repetido en cada controller
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(ORIGEN_FRONTEND)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }

    // autorizacion: aqui se activa el interceptor para todas las rutas /api/**
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(autorizacionInterceptor).addPathPatterns("/api/**");
    }
}
