import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Préstamo con nombre del prestatario y título del libro (consulta con JOIN). */
export interface PrestamoDetalle {
  idPrestamo: number;
  nombrePrestatario: string;
  dniPrestatario: string;
  tituloLibro: string;
  fechaPrestamo: string;
  fechaDevolucionPrevista: string;
  ampliado: boolean;
  estadoPrestamo: string;
}

/** Préstamo tal como está en la tabla prestamo (consulta de una tabla). */
export interface Prestamo {
  idPrestamo: number;
  idPrestatario: number;
  idEncargado: number;
  idLibro: number;
  fechaPrestamo: string;
  fechaDevolucionPrevista: string;
  fechaDevolucionReal: string | null;
  ampliado: boolean;
  estadoFisicoDevolucion: string | null;
  estadoPrestamo: string;
}

@Injectable({
  providedIn: 'root'
})
export class PrestamoService {
  private http = inject(HttpClient);
  // ruta crud prestamo (front): aqui estan las llamadas al backend del crud de prestamos
  private apiUrl = 'http://localhost:8080/api/prestamos';

  registrar(dniPrestatario: string, idLibro: number): Observable<Prestamo> {
    return this.http.post<Prestamo>(this.apiUrl, { dniPrestatario, idLibro });
  }

  listarActivos(): Observable<PrestamoDetalle[]> {
    return this.http.get<PrestamoDetalle[]>(`${this.apiUrl}/activos`);
  }

  listarPorPrestatario(idPrestatario: number): Observable<Prestamo[]> {
    return this.http.get<Prestamo[]>(`${this.apiUrl}/prestatario/${idPrestatario}`);
  }

  ampliar(idPrestamo: number): Observable<Prestamo> {
    return this.http.put<Prestamo>(`${this.apiUrl}/${idPrestamo}/ampliar`, {});
  }

  devolver(idPrestamo: number, estadoFisico: 'bueno' | 'dañado'): Observable<Prestamo> {
    return this.http.put<Prestamo>(`${this.apiUrl}/${idPrestamo}/devolucion`, { estadoFisico });
  }

  anular(idPrestamo: number): Observable<{ mensaje: string }> {
    return this.http.delete<{ mensaje: string }>(`${this.apiUrl}/${idPrestamo}`);
  }
}
