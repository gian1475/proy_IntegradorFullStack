package com.alejandria.biblioteca.seguridad;

import com.alejandria.biblioteca.service.SesionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de Spring Security: quién puede llamar a la API (CORS), el filtro del token
 * y la activación de @RolesAllowed en los controllers.
 */
// spring security: aqui se arma la seguridad de todo el backend. reemplaza al interceptor q habiamos hecho a mano
@Configuration
@EnableMethodSecurity(jsr250Enabled = true) // jsr250 = activa @RolesAllowed en los controllers
public class SeguridadConfig {

    /** Única dirección desde la que el navegador puede llamar a la API: el frontend Angular. */
    public static final String ORIGEN_FRONTEND = "http://localhost:4200";

    @Bean
    public SecurityFilterChain cadenaDeFiltros(HttpSecurity http, SesionService sesionService) throws Exception {
        return http
                // la api usa un token en el header, no cookies: no hace falta la proteccion csrf
                .csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(configuracionCors()))
                // spring security no guarda sesiones: las sesiones (token -> usuario) las guarda SesionService
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // sin la pantalla de login ni el /logout de spring: el login es nuestro (/api/auth/login)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // autorizacion: todas las rutas dejan pasar la peticion; cada endpoint protegido dice sus perfiles
                // con @RolesAllowed (los que no lo tienen son publicos: catalogo, login y registro)
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .addFilterBefore(new FiltroToken(sesionService), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // cors: solo el frontend propio (localhost:4200) puede llamar a la api
    @Bean
    public CorsConfigurationSource configuracionCors() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(ORIGEN_FRONTEND));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource rutas = new UrlBasedCorsConfigurationSource();
        rutas.registerCorsConfiguration("/api/**", cors);
        return rutas;
    }
}
