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

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {
  private http = inject(HttpClient);
  // ruta usuarios (front): solo el admin la usa (RF09)
  private apiUrl = 'http://localhost:8080/api/usuarios';

  listarEncargados(): Observable<Encargado[]> {
    return this.http.get<Encargado[]>(`${this.apiUrl}/encargados`);
  }

  // crear perfil 2 (front): el admin registra un encargado desde su panel
  registrarEncargado(datos: RegistroEncargado): Observable<Encargado> {
    return this.http.post<Encargado>(`${this.apiUrl}/encargados`, datos);
  }
}
