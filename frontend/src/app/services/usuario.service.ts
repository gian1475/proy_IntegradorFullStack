import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Encargado con su turno (sin la contraseña). */
export interface Encargado {
  idUsuario: number;
  nombre: string;
  apellido: string;
  dni: string;
  correo: string;
  estado: string;
  turno: string;
}

export interface RegistroEncargado {
  nombre: string;
  apellido: string;
  dni: string;
  correo: string;
  contrasena: string;
  turno: string;
}

/** RF12 y RF13: prestatario o encargado en la pestaña "Usuarios" (sin la contraseña). */
export interface UsuarioAdmin {
  idUsuario: number;
  nombre: string;
  apellido: string;
  dni: string;
  correo: string;
  estado: string;            // activo, suspendido o inactivo
  perfil: string;            // PRESTATARIO o ENCARGADO
  turno: string | null;      // solo el encargado
}

/** RF12: lo que el admin puede cambiar (sin contraseña ni estado). turno: solo para encargados. */
export interface ActualizarUsuario {
  nombre: string;
  apellido: string;
  dni: string;
  correo: string;
  turno: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {
  private http = inject(HttpClient);
  // ruta usuarios (front): solo el admin la usa (RF09, RF12 y RF13)
  private apiUrl = 'http://localhost:8080/api/usuarios';

  // crear perfil 2 (front): el admin registra un encargado desde su panel
  registrarEncargado(datos: RegistroEncargado): Observable<Encargado> {
    return this.http.post<Encargado>(`${this.apiUrl}/encargados`, datos);
  }

  listarUsuarios(): Observable<UsuarioAdmin[]> {
    return this.http.get<UsuarioAdmin[]>(this.apiUrl);
  }

  // RF12 editar usuario (front)
  actualizar(id: number, datos: ActualizarUsuario): Observable<UsuarioAdmin> {
    return this.http.put<UsuarioAdmin>(`${this.apiUrl}/${id}`, datos);
  }

  // RF13 baja logica (front): DELETE, pero en la bd el usuario solo pasa a 'inactivo'
  darDeBaja(id: number): Observable<UsuarioAdmin> {
    return this.http.delete<UsuarioAdmin>(`${this.apiUrl}/${id}`);
  }

  // restablecer contra (front, B7): el admin pone una contraseña nueva a quien la olvido
  restablecerContrasena(id: number, contrasena: string): Observable<UsuarioAdmin> {
    return this.http.put<UsuarioAdmin>(`${this.apiUrl}/${id}/contrasena`, { contrasena });
  }

  reactivar(id: number): Observable<UsuarioAdmin> {
    return this.http.put<UsuarioAdmin>(`${this.apiUrl}/${id}/reactivar`, {});
  }
}
