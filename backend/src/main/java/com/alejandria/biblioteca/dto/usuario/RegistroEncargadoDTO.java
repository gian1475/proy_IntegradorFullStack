package com.alejandria.biblioteca.dto.usuario;

/** Datos que llena el admin para registrar un encargado (RF09). turno: "mañana" o "tarde". */
public class RegistroEncargadoDTO {
    private String nombre;
    private String apellido;
    private String dni;
    private String correo;
    private String contrasena;
    private String turno;

    public RegistroEncargadoDTO() {}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public String getTurno() { return turno; }
    public void setTurno(String turno) { this.turno = turno; }
}
