import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Dashboard, ReporteService } from '../../../../services/reporte.service';
import { mensajeDeError } from '../../../../utils/mensaje-error';

/** Una tarjeta de número grande arriba del dashboard. */
interface Tarjeta {
  titulo: string;
  valor: number;
  detalle: string;
  color: string; // clase de color de Bootstrap
}

/**
 * Pestaña "Dashboard" del admin (patrón Dashboard del informe), hecha con Bootstrap.
 * Muestra el catálogo, lo de hoy, lo del mes y los 4 indicadores de gestión del apartado 1.2.2.
 */
// dashboard (front): todo lo visual usa clases de bootstrap (card, row, col, badge, progress)
@Component({
  selector: 'app-dashboard-tab',
  standalone: true,
  imports: [DatePipe, DecimalPipe],
  templateUrl: './dashboard-tab.component.html',
})
export class DashboardTabComponent implements OnInit {
  private reporteService = inject(ReporteService);

  readonly datos = signal<Dashboard | null>(null);
  readonly mensajeError = signal('');
  readonly cargando = signal(false);

  readonly tarjetas = computed<Tarjeta[]>(() => {
    const d = this.datos();
    if (!d) return [];
    return [
      { titulo: 'Títulos en el catálogo', valor: d.titulos, detalle: `${d.ejemplaresDisponibles} ejemplares disponibles`, color: 'text-primary' },
      { titulo: 'Préstamos en sala ahora', valor: d.prestamosActivos, detalle: 'activos dentro de su hora', color: 'text-success' },
      { titulo: 'Préstamos vencidos', valor: d.prestamosVencidos, detalle: 'pasaron la hora límite', color: d.prestamosVencidos > 0 ? 'text-danger' : 'text-body-secondary' },
      { titulo: 'Reservas pendientes', valor: d.reservasPendientes, detalle: 'por recoger', color: 'text-info' },
      { titulo: `Préstamos de ${d.mes}`, valor: d.prestamosMes, detalle: `${d.devolucionesMes} ya devueltos`, color: 'text-primary' },
      { titulo: 'Prestatarios', valor: d.prestatarios, detalle: 'registrados', color: 'text-body-secondary' },
    ];
  });

  /** Para que las barras de los últimos 7 días se dibujen proporcionales al día con más préstamos. */
  readonly maximoDia = computed(() => Math.max(1, ...(this.datos()?.prestamosUltimos7Dias ?? []).map(d => d.cantidad)));
  readonly maximoCategoria = computed(() => Math.max(1, ...(this.datos()?.prestamosPorCategoria ?? []).map(c => c.cantidad)));

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.mensajeError.set('');
    this.reporteService.obtenerDashboard().subscribe({
      next: d => { this.datos.set(d); this.cargando.set(false); },
      error: err => { this.mensajeError.set(mensajeDeError(err)); this.cargando.set(false); }
    });
  }

  porcentaje(parte: number, total: number): number {
    return total > 0 ? Math.round((parte * 100) / total) : 0;
  }

  /** Color de Bootstrap para cada estado del libro. */
  colorEstado(estado: string): string {
    switch (estado) {
      case 'disponible': return 'bg-success';
      case 'prestado': return 'bg-warning';
      case 'reservado': return 'bg-info';
      case 'mantenimiento': return 'bg-danger';
      default: return 'bg-secondary';
    }
  }
}
