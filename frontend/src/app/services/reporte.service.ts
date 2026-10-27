import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface ResultadoImportacion {
  creados: number;
  errores: string[];
}

export type FormatoReporte = 'excel' | 'pdf' | 'csv';

/** Un tipo de reporte (RF08). usaFechas: false si el rango de fechas no lo cambia (es una foto de ahora). */
export interface TipoReporte {
  valor: string;
  nombre: string;
  descripcion: string;
  usaFechas: boolean;
}

// reportes rf08 (front): los 8 reportes, mismos nombres q en ReporteService del backend
export const TIPOS_REPORTE: TipoReporte[] = [
  { valor: 'prestamos', nombre: 'Préstamos', descripcion: 'Todos los préstamos del periodo con prestatario, encargado, libro, fechas y estado.', usaFechas: true },
  { valor: 'devoluciones', nombre: 'Devoluciones', descripcion: 'Libros devueltos en el periodo, a tiempo o con retraso, y su estado físico.', usaFechas: true },
  { valor: 'reservas', nombre: 'Reservas', descripcion: 'Reservas hechas en el periodo, con su estado y su puesto si están en la cola de espera.', usaFechas: true },
  { valor: 'incidencias', nombre: 'Incidencias y sanciones', descripcion: 'Incidencias por retraso, daño o pérdida, con la sanción que se aplicó.', usaFechas: true },
  { valor: 'bajas', nombre: 'Bajas lógicas', descripcion: 'Libros dados de baja, préstamos anulados y usuarios inactivos (siguen en la base de datos).', usaFechas: true },
  { valor: 'stock', nombre: 'Stock por categoría', descripcion: 'Títulos y ejemplares disponibles de cada categoría en este momento.', usaFechas: false },
  { valor: 'indicadores', nombre: 'Indicadores de gestión', descripcion: 'TAP, PDF, IPI y TIM del periodo, con su línea base y su meta.', usaFechas: true },
  { valor: 'tiempos', nombre: 'Tiempos de préstamo', descripcion: 'Tiempo mínimo, promedio y máximo que estuvieron los libros en sala, por categoría.', usaFechas: true },
];

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

  /** Descarga un reporte. desde / hasta en formato AAAA-MM-DD (vacío = sin límite). Se pide con HttpClient para que viaje el token. */
  descargar(tipo: string, formato: FormatoReporte, desde: string, hasta: string): Observable<Blob> {
    let params = new HttpParams();
    if (desde) params = params.set('desde', desde);
    if (hasta) params = params.set('hasta', hasta);
    const hoy = new Date().toISOString().slice(0, 10);
    return this.http.get(`${this.apiUrl}/${tipo}/${formato}`, { params, responseType: 'blob' }).pipe(
      tap(archivo => this.guardarArchivo(archivo, `reporte_${tipo}_${hoy}.${EXTENSIONES[formato]}`))
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
