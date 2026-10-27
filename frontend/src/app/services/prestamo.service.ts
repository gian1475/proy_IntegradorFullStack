import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
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

/** Fila del historial (RF07): préstamo con prestatario, encargado y libro (consulta con JOIN). */
export interface HistorialPrestamo {
  idPrestamo: number;
  prestatario: string;
  dniPrestatario: string;
  encargado: string;
  tituloLibro: string;
  fechaPrestamo: string;
  fechaDevolucionPrevista: string;
  fechaDevolucionReal: string | null;
  ampliado: boolean;
  estadoFisicoDevolucion: string | null;
  estadoPrestamo: string;
}

/** Filtros del historial. Los vacíos no se envían. Fechas en formato AAAA-MM-DD. */
export interface FiltrosHistorial {
  dni: string;
  libro: string;
  desde: string;
  hasta: string;
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

  // historial (front): GET /api/prestamos/historial con los filtros q no esten vacios
  historial(filtros: FiltrosHistorial): Observable<HistorialPrestamo[]> {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(filtros)) {
      if (valor && valor.trim()) params = params.set(clave, valor.trim());
    }
    return this.http.get<HistorialPrestamo[]>(`${this.apiUrl}/historial`, { params });
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
