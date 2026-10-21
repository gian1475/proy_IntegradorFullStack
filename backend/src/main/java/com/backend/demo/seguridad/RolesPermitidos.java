package com.backend.demo.seguridad;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un endpoint (o un controlador completo) como protegido.
 * Solo pueden usarlo los usuarios con sesión válida y uno de los roles indicados.
 * Los endpoints sin esta anotación son públicos.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RolesPermitidos {
    String[] value();
}
