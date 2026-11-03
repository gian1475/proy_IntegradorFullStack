import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** IPI: un conteo físico de un título, con el stock que tenía el sistema en ese momento. */
export interface Auditoria {
  idAuditoria: number;
  idLibro: number;
  tituloLibro: string;
  isbn: string;
  fechaAuditoria: string;
  stockSistema: number;
  conteoFisico: number;
  coincide: boolean;
  administrador: string;
}

/** IPI del mes (null si no hay auditorías) y las auditorías del mes. */
export interface AuditoriaMes {
  ipi: number | null;
  auditados: number;
  coinciden: number;
  auditorias: Auditoria[];
}

@Injectable({
  providedIn: 'root'
})
export class AuditoriaService {
  private http = inject(HttpClient);
  // auditoria inventario (front): solo el admin
  private apiUrl = 'http://localhost:8080/api/auditorias';

  resumenDelMes(): Observable<AuditoriaMes> {
    return this.http.get<AuditoriaMes>(this.apiUrl);
  }

  registrar(idLibro: number, conteoFisico: number): Observable<Auditoria> {
    return this.http.post<Auditoria>(this.apiUrl, { idLibro, conteoFisico });
  }
}
