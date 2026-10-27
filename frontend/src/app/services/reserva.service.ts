import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ReservaBackend {
  idReserva: number;
  idLibro: number;
  tituloLibro: string;
  isbnLibro: string;
  autor: string;
  categoria: string;
  idPrestatario: number;
  nombrePrestatario: string;
  fechaReserva: string;
  /** null mientras está en la cola de espera (todavía no le toca un ejemplar). */
  fechaRecojoLimite: string | null;
  /** Puesto en la cola de espera (solo cuenta si estadoReserva es 'en_espera'). */
  posicionCola: number;
  estadoReserva: string;
}

@Injectable({
  providedIn: 'root'
})
export class ReservaService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/reservas';

  /**
   * fechaRecojoLimite en formato local "AAAA-MM-DDTHH:mm:ss" (hasta cuándo puede recoger el libro).
   * Sin stock va null: el backend pone la reserva en la cola de espera (RF04).
   */
  crearReserva(idLibro: number, idUsuario: number, fechaRecojoLimite: string | null): Observable<ReservaBackend> {
    return this.http.post<ReservaBackend>(this.apiUrl, { idLibro, idUsuario, fechaRecojoLimite });
  }

  getReservasPorUsuario(idUsuario: number): Observable<ReservaBackend[]> {
    return this.http.get<ReservaBackend[]>(`${this.apiUrl}/usuario/${idUsuario}`);
  }

  getTodas(): Observable<ReservaBackend[]> {
    return this.http.get<ReservaBackend[]>(this.apiUrl);
  }

  /** Eliminación lógica: la reserva queda 'cancelada' (PUT). */
  cancelar(idReserva: number): Observable<{ mensaje: string }> {
    return this.http.put<{ mensaje: string }>(`${this.apiUrl}/${idReserva}/cancelar`, {});
  }

  // eliminacion fisica (front): DELETE, la reserva se borra de la bd. solo el admin y solo canceladas o vencidas
  eliminar(idReserva: number): Observable<{ mensaje: string }> {
    return this.http.delete<{ mensaje: string }>(`${this.apiUrl}/${idReserva}`);
  }
}
