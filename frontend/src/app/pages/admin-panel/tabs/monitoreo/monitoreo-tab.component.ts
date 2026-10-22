import { Component, OnInit, inject, signal } from '@angular/core';
import { Monitoreo, ReporteService } from '../../../../services/reporte.service';
import { mensajeDeError } from '../../../../utils/mensaje-error';

/** Pestaña "Monitoreo" del admin: métricas de Spring Boot Actuator. */
@Component({
  selector: 'app-monitoreo-tab',
  standalone: true,
  templateUrl: './monitoreo-tab.component.html',
})
export class MonitoreoTabComponent implements OnInit {
  private reporteService = inject(ReporteService);

  readonly monitoreo = signal<Monitoreo | null>(null);
  readonly mensajeError = signal('');

  ngOnInit(): void {
    this.cargarMonitoreo();
  }

  // actuator / monitoreo (front): pide el resumen de metricas al backend
  cargarMonitoreo(): void {
    this.mensajeError.set('');
    this.reporteService.obtenerMonitoreo().subscribe({
      next: datos => this.monitoreo.set(datos),
      error: err => this.mensajeError.set(mensajeDeError(err))
    });
  }

  /** 3725 segundos -> "1 h 2 min" */
  formatearTiempo(segundos: number | null): string {
    if (segundos === null) return '-';
    const horas = Math.floor(segundos / 3600);
    const minutos = Math.floor((segundos % 3600) / 60);
    return horas > 0 ? `${horas} h ${minutos} min` : `${minutos} min`;
  }
}
