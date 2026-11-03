package com.alejandria.biblioteca.seguridad;

import com.alejandria.biblioteca.service.SesionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro de Spring Security: lee el token del header "Authorization: Bearer ..." y, si la sesión
 * es válida, le dice a Spring Security quién es el usuario y qué perfil tiene.
 * No rechaza nada: el 401 o el 403 los decide @RolesAllowed en cada endpoint.
 */
// spring security: este filtro corre una vez en cada peticion, antes de llegar al controller
public class FiltroToken extends OncePerRequestFilter {

    private final SesionService sesionService;

    public FiltroToken(SesionService sesionService) {
        this.sesionService = sesionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        String token = SesionService.tokenDesdeHeader(request.getHeader("Authorization"));

        // token valido -> el usuario queda autenticado con su perfil. "ROLE_" es el prefijo q usa spring security
        // para los perfiles: @RolesAllowed("ENCARGADO") busca "ROLE_ENCARGADO"
        sesionService.validar(token).ifPresent(sesion -> {
            var perfil = new SimpleGrantedAuthority("ROLE_" + sesion.rol());
            var autenticacion = new UsernamePasswordAuthenticationToken(sesion, null, List.of(perfil));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        });

        cadena.doFilter(request, response);
    }
}
