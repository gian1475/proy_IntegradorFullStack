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

export interface RespuestaLogin {
  token: string;
  expiraEn: string;
  usuario: UsuarioActual;
}

const CLAVE_SESION = 'sesion_alejandria';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/auth';

  private sesion: RespuestaLogin | null = this.obtenerSesionGuardada();
  readonly usuarioActual = signal<UsuarioActual | null>(this.sesion?.usuario ?? null);

  private obtenerSesionGuardada(): RespuestaLogin | null {
    try {
      const data = localStorage.getItem(CLAVE_SESION);
      if (!data) {
        return null;
      }
      const sesion: RespuestaLogin = JSON.parse(data);
      if (new Date(sesion.expiraEn) <= new Date()) {
        localStorage.removeItem(CLAVE_SESION);
        return null;
      }
      return sesion;
    } catch {
      return null;
    }
  }

  private guardarSesion(respuesta: RespuestaLogin): void {
    localStorage.setItem(CLAVE_SESION, JSON.stringify(respuesta));
    this.sesion = respuesta;
    this.usuarioActual.set(respuesta.usuario);
  }

  login(correo: string, contrasena: string): Observable<RespuestaLogin> {
    return this.http.post<RespuestaLogin>(`${this.apiUrl}/login`, { correo, contrasena }).pipe(
      tap(respuesta => this.guardarSesion(respuesta))
    );
  }

  registro(datos: { nombre: string; apellido: string; dni: string; correo: string; contrasena: string }): Observable<RespuestaLogin> {
    return this.http.post<RespuestaLogin>(`${this.apiUrl}/registro`, datos).pipe(
      tap(respuesta => this.guardarSesion(respuesta))
    );
  }

  /** Invalida el token en el backend y borra la sesión del navegador. */
  logout(): void {
    const token = this.sesion?.token;
    this.cerrarSesionLocal();
    if (token) {
      this.http.post(`${this.apiUrl}/logout`, {}, { headers: { Authorization: `Bearer ${token}` } })
        .subscribe({ error: () => {} });
    }
  }

  /** Borra la sesión solo en el navegador (por ejemplo, cuando el backend dice que el token venció). */
  cerrarSesionLocal(): void {
    localStorage.removeItem(CLAVE_SESION);
    this.sesion = null;
    this.usuarioActual.set(null);
  }

  obtenerToken(): string | null {
    if (this.sesion && new Date(this.sesion.expiraEn) <= new Date()) {
      this.cerrarSesionLocal();
    }
    return this.sesion?.token ?? null;
  }

  estaAutenticado(): boolean {
    return this.obtenerToken() !== null;
  }

  /** Pantalla principal de cada perfil. */
  rutaInicio(rol?: string): string {
    switch (rol ?? this.usuarioActual()?.rol) {
      case 'ADMINISTRADOR': return '/admin';
      case 'ENCARGADO': return '/encargado';
      case 'PRESTATARIO': return '/catalogo';
      default: return '/login';
    }
  }
}
