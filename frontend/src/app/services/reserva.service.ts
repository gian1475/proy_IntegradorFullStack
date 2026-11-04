import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

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
  private apiUrl = `${environment.apiUrl}/reservas`;

  /**
   * fechaRecojoLimite en formato local "AAAA-MM-DDTHH:mm:ss" (hasta cuándo puede recoger el libro).
   * Sin stock va null: el backend pone la reserva en la cola de espera (RF04).
   */
  crearReserva(idLibro: number, idUsuario: number, fechaRecojoLimite: string | null): Observable<ReservaBackend> {
    return this.http.post<ReservaBackend>(this.apiUrl, { idLibro, idUsuario, fechaRecojoLimite });
  }

  // reserva ventanilla (front, B2): el encargado reserva a nombre del prestatario con su DNI.
  // sin stock va null y el prestatario entra a la cola de espera
  crearEnVentanilla(dniPrestatario: string, idLibro: number, fechaRecojoLimite: string | null): Observable<ReservaBackend> {
    return this.http.post<ReservaBackend>(`${this.apiUrl}/ventanilla`, { dniPrestatario, idLibro, fechaRecojoLimite });
  }

  getReservasPorUsuario(idUsuario: number): Observable<ReservaBackend[]> {
    return this.http.get<ReservaBackend[]>(`${this.apiUrl}/usuario/${idUsuario}`);
  }

  getTodas(): Observable<ReservaBackend[]> {
    return this.http.get<ReservaBackend[]>(this.apiUrl);
  }

  // reserva actualizar (front, RF15): nueva fecha y franja de recojo (el final de la franja, "AAAA-MM-DDTHH:mm:ss")
  actualizar(idReserva: number, fechaRecojoLimite: string): Observable<ReservaBackend> {
    return this.http.put<ReservaBackend>(`${this.apiUrl}/${idReserva}`, { fechaRecojoLimite });
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
