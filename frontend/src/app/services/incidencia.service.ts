import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Incidencia con su préstamo y su sanción (RF06). */
export interface Incidencia {
  idIncidencia: number;
  idPrestamo: number;
  prestatario: string;
  dniPrestatario: string;
  tituloLibro: string;
  tipoIncidencia: string;
  descripcion: string | null;
  fechaIncidencia: string;
  estadoIncidencia: string;
  idSancion: number | null;
  tipoSancion: string | null;
  monto: number | null;
  descripcionSancion: string | null;
  inicioSancion: string | null;
  finSancion: string | null;
  estadoSancion: string | null;
}

/** Lo que manda el formulario. Los días solo se usan en la suspensión y el monto solo en la multa. */
export interface RegistroIncidencia {
  idPrestamo: number;
  tipoIncidencia: string;
  descripcion: string;
  tipoSancion: string;
  diasSuspension: number | null;
  monto: number | null;
}

@Injectable({
  providedIn: 'root'
})
export class IncidenciaService {
  private http = inject(HttpClient);
  // ruta incidencias (front): las llamadas al backend de incidencias y sanciones (RF06), solo del encargado
  private apiUrl = 'http://localhost:8080/api/incidencias';

  listar(): Observable<Incidencia[]> {
    return this.http.get<Incidencia[]>(this.apiUrl);
  }

  registrar(datos: RegistroIncidencia): Observable<Incidencia> {
    return this.http.post<Incidencia>(this.apiUrl, datos);
  }

  registrarPago(idSancion: number): Observable<Incidencia> {
    return this.http.put<Incidencia>(`${this.apiUrl}/sanciones/${idSancion}/pago`, {});
  }

  cancelarSancion(idSancion: number): Observable<Incidencia> {
    return this.http.put<Incidencia>(`${this.apiUrl}/sanciones/${idSancion}/cancelar`, {});
  }
}
