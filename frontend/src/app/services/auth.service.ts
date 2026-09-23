import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface UsuarioActual {
  idUsuario: number;
  nombre: string;
  apellido: string;
  dni: string;
  correo: string;
  estado: string;
  rol: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/auth';

  readonly usuarioActual = signal<UsuarioActual | null>(this.obtenerSesionGuardada());

  private obtenerSesionGuardada(): UsuarioActual | null {
    try {
      const data = localStorage.getItem('usuario_luz_del_saber');
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  }

  login(correo: string, contrasena: string): Observable<UsuarioActual> {
    return this.http.post<UsuarioActual>(`${this.apiUrl}/login`, { correo, contrasena }).pipe(
      tap(usuario => {
        localStorage.setItem('usuario_luz_del_saber', JSON.stringify(usuario));
        this.usuarioActual.set(usuario);
      })
    );
  }

  registro(datos: { nombre: string; apellido: string; dni: string; correo: string; contrasena: string }): Observable<UsuarioActual> {
    return this.http.post<UsuarioActual>(`${this.apiUrl}/registro`, datos).pipe(
      tap(usuario => {
        localStorage.setItem('usuario_luz_del_saber', JSON.stringify(usuario));
        this.usuarioActual.set(usuario);
      })
    );
  }

  logout(): void {
    localStorage.removeItem('usuario_luz_del_saber');
    this.usuarioActual.set(null);
  }

  estaAutenticado(): boolean {
    return this.usuarioActual() !== null;
  }
}
