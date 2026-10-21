package com.backend.demo.seguridad;

import com.backend.demo.service.SesionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Se ejecuta antes de cada endpoint de /api. Si el endpoint tiene @RolesPermitidos,
 * revisa que el token sea válido (401 si no) y que el rol esté permitido (403 si no).
 */
@Component
public class AutorizacionInterceptor implements HandlerInterceptor {

    public static final String ATRIBUTO_SESION = "sesion";

    private final SesionService sesionService;

    public AutorizacionInterceptor(SesionService sesionService) {
        this.sesionService = sesionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod metodo)) {
            return true;
        }

        RolesPermitidos anotacion = metodo.getMethodAnnotation(RolesPermitidos.class);
        if (anotacion == null) {
            anotacion = metodo.getBeanType().getAnnotation(RolesPermitidos.class);
        }
        if (anotacion == null) {
            return true; // endpoint público
        }

        String token = SesionService.tokenDesdeHeader(request.getHeader("Authorization"));
        Optional<Sesion> sesion = sesionService.validar(token);
        if (sesion.isEmpty()) {
            responderError(response, HttpStatus.UNAUTHORIZED, "Sesión no válida o expirada. Inicie sesión nuevamente");
            return false;
        }

        if (!Arrays.asList(anotacion.value()).contains(sesion.get().rol())) {
            responderError(response, HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta acción");
            return false;
        }

        request.setAttribute(ATRIBUTO_SESION, sesion.get());
        return true;
    }

    private void responderError(HttpServletResponse response, HttpStatus estado, String mensaje) throws IOException {
        response.setStatus(estado.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"mensaje\":\"" + mensaje + "\"}");
    }
}
