package com.alejandria.biblioteca.seguridad;

/** Nombres de los perfiles del sistema. */
// perfiles: estos son los 3 perfiles (roles) del sistema
public final class Rol {

    public static final String ADMINISTRADOR = "ADMINISTRADOR"; // perfil 1: coordinador bibliotecario
    public static final String ENCARGADO = "ENCARGADO";         // perfil 2: encargado de biblioteca
    public static final String PRESTATARIO = "PRESTATARIO";     // perfil 3: lector

    private Rol() {}
}
