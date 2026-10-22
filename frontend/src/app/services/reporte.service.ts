import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface ResultadoImportacion {
  creados: number;
  errores: string[];
}

export type FormatoReporte = 'excel' | 'pdf' | 'csv';

/** Métricas de /api/monitoreo (Spring Boot Actuator + datos del negocio). */
export interface Monitoreo {
  estado: string;
  tiempoActivoSegundos: number | null;
  memoriaUsadaMB: number | null;
  memoriaMaximaMB: number | null;
  cpuProcesoPorcentaje: number | null;
  hilosActivos: number | null;
  peticionesHttp: number;
  peticionesConError: number;
  tiempoRespuestaPromedioMs: number;
  conexionesBdActivas: number | null;
  conexionesBdTotales: number | null;
  sesionesActivas: number;
  prestamosActivos: number;
  prestamosVencidos: number;
  reservasPendientes: number;
}

/** Un nombre con su cantidad (barras del dashboard). */
export interface Conteo {
  nombre: string;
  cantidad: number;
}

/** Indicador de gestión del apartado 1.2.2 del informe. valor = null si el sistema no puede medirlo. */
export interface Indicador {
  codigo: string;
  nombre: string;
  valor: number | null;
  unidad: string;
  lineaBase: string;
  meta: string;
  cumpleMeta: boolean | null;
  detalle: string;
}

/** Datos de /api/dashboard. */
export interface Dashboard {
  generadoEn: string;
  mes: string;
  titulos: number;
  ejemplaresDisponibles: number;
  librosPorEstado: Conteo[];
  prestamosActivos: number;
  prestamosVencidos: number;
  reservasPendientes: number;
  prestatarios: number;
  prestamosMes: number;
  devolucionesMes: number;
  devolucionesConRetrasoMes: number;
  devolucionesDanadasMes: number;
  indicadores: Indicador[];
  prestamosPorCategoria: Conteo[];
  prestamosUltimos7Dias: Conteo[];
}

const EXTENSIONES: Record<FormatoReporte, string> = { excel: 'xlsx', pdf: 'pdf', csv: 'csv' };

@Injectable({
  providedIn: 'root'
})
export class ReporteService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/reportes';

  /** Descarga el reporte de préstamos. Se pide con HttpClient para que viaje el token. */
  descargarPrestamos(formato: FormatoReporte): Observable<Blob> {
    const hoy = new Date().toISOString().slice(0, 10);
    return this.http.get(`${this.apiUrl}/prestamos/${formato}`, { responseType: 'blob' }).pipe(
      tap(archivo => this.guardarArchivo(archivo, `reporte_prestamos_${hoy}.${EXTENSIONES[formato]}`))
    );
  }

  importarLibros(archivo: File): Observable<ResultadoImportacion> {
    const datos = new FormData();
    datos.append('archivo', archivo);
    return this.http.post<ResultadoImportacion>(`${this.apiUrl}/libros/csv`, datos);
  }

  obtenerMonitoreo(): Observable<Monitoreo> {
    return this.http.get<Monitoreo>('http://localhost:8080/api/monitoreo');
  }

  // dashboard (front): pide el resumen del dashboard del admin
  obtenerDashboard(): Observable<Dashboard> {
    return this.http.get<Dashboard>('http://localhost:8080/api/dashboard');
  }

  /** Crea un enlace temporal y lo "clickea" para que el navegador descargue el archivo. */
  private guardarArchivo(archivo: Blob, nombre: string): void {
    const url = URL.createObjectURL(archivo);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = nombre;
    enlace.click();
    URL.revokeObjectURL(url);
  }
}
