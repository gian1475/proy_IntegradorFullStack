import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ReservaBackend {
  idReserva?: number;
  idLibro: number;
  idUsuario: number;
  fechaReserva?: string;
  posicionCola?: number;
  estadoReserva?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ReservaService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/reservas';

  crearReserva(idLibro: number, idUsuario: number): Observable<ReservaBackend> {
    return this.http.post<ReservaBackend>(this.apiUrl, { idLibro, idUsuario });
  }

  getReservasPorUsuario(idUsuario: number): Observable<ReservaBackend[]> {
    return this.http.get<ReservaBackend[]>(`${this.apiUrl}/usuario/${idUsuario}`);
  }
}
