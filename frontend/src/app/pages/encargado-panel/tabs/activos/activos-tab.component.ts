import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { PrestamoDetalle, PrestamoService } from '../../../../services/prestamo.service';
import { mensajeDeError } from '../../../../utils/mensaje-error';
import { AlertaPrestamo, MINUTOS_AVISO, alertaPrestamo } from '../../../../utils/alerta-prestamo';

/** Cada cuánto se recalculan las alertas (el reloj de la pantalla). */
const SEGUNDOS_RELOJ = 30;

/** Pestaña "Préstamos activos" del encargado: ampliar, devolver y anular. */
@Component({
  selector: 'app-activos-tab',
  standalone: true,
  imports: [DatePipe, FormsModule],
  templateUrl: './activos-tab.component.html',
})
export class ActivosTabComponent implements OnInit, OnDestroy {
  private prestamoService = inject(PrestamoService);

  readonly mensajeOk = signal('');
  readonly mensajeError = signal('');
  readonly cargando = signal(false);

  // Préstamos activos (consulta con JOIN)
  readonly prestamos = signal<PrestamoDetalle[]>([]);

  // alertas 15 min (RF05): cada prestamo con su alerta, recalculada cada 30 segundos con el reloj
  readonly minutosAviso = MINUTOS_AVISO;
  private readonly ahora = signal(new Date());
  private reloj?: ReturnType<typeof setInterval>;
  readonly prestamosConAlerta = computed(() =>
    this.prestamos().map(p => ({ ...p, alerta: alertaPrestamo(p.fechaDevolucionPrevista, this.ahora()) }))
  );
  readonly cantidadVencidos = computed(() => this.contar('vencido'));
  readonly cantidadPorVencer = computed(() => this.contar('por-vencer'));

  // Devolución
  readonly prestamoADevolver = signal<PrestamoDetalle | null>(null);
  estadoFisico: 'bueno' | 'dañado' = 'bueno';

  ngOnInit(): void {
    this.cargarPrestamos();
    this.reloj = setInterval(() => this.ahora.set(new Date()), SEGUNDOS_RELOJ * 1000);
  }

  ngOnDestroy(): void {
    clearInterval(this.reloj); // al salir de la pestaña se apaga el reloj
  }

  private contar(tipo: AlertaPrestamo['tipo']): number {
    return this.prestamosConAlerta().filter(p => p.alerta.tipo === tipo).length;
  }

  // crud prestamo consultar (front): la tabla de prestamos activos sale de la consulta dos tablas
  cargarPrestamos(): void {
    this.cargando.set(true);
    this.prestamoService.listarActivos().subscribe({
      next: data => { this.prestamos.set(data); this.ahora.set(new Date()); this.cargando.set(false); },
      error: err => this.mostrarError(err)
    });
  }

  // crud prestamo actualizar (front): boton ampliar
  ampliar(p: PrestamoDetalle): void {
    this.limpiarMensajes();
    this.prestamoService.ampliar(p.idPrestamo).subscribe({
      next: actualizado => {
        this.mensajeOk.set(`Préstamo #${p.idPrestamo} ampliado hasta las ${this.formatearHora(actualizado.fechaDevolucionPrevista)}`);
        this.cargarPrestamos();
      },
      error: err => this.mostrarError(err)
    });
  }

  abrirDevolucion(p: PrestamoDetalle): void {
    this.estadoFisico = 'bueno';
    this.prestamoADevolver.set(p);
  }

  // crud prestamo actualizar (front): devolucion con el estado fisico (bueno / dañado)
  confirmarDevolucion(): void {
    const p = this.prestamoADevolver();
    if (!p) return;
    this.limpiarMensajes();
    this.prestamoService.devolver(p.idPrestamo, this.estadoFisico).subscribe({
      next: devuelto => {
        this.prestamoADevolver.set(null);
        const conRetraso = devuelto.fechaDevolucionReal !== null
          && new Date(devuelto.fechaDevolucionReal) > new Date(devuelto.fechaDevolucionPrevista);
        this.mensajeOk.set(`Devolución de "${p.tituloLibro}" registrada (estado: ${this.estadoFisico})`
          + (conRetraso ? '. Se devolvió con retraso' : '')
          + (this.estadoFisico === 'dañado' ? '. El ejemplar quedó en mantenimiento y no vuelve al stock' : ''));
        this.cargarPrestamos();
      },
      error: err => { this.prestamoADevolver.set(null); this.mostrarError(err); }
    });
  }

  // crud prestamo eliminar (front): boton anular (eliminacion logica)
  anular(p: PrestamoDetalle): void {
    if (!confirm(`¿Anular el préstamo #${p.idPrestamo} de "${p.tituloLibro}"?`)) return;
    this.limpiarMensajes();
    this.prestamoService.anular(p.idPrestamo).subscribe({
      next: res => { this.mensajeOk.set(res.mensaje); this.cargarPrestamos(); },
      error: err => this.mostrarError(err)
    });
  }

  private formatearHora(fecha: string): string {
    return new Date(fecha).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
  }

  private limpiarMensajes(): void {
    this.mensajeOk.set('');
    this.mensajeError.set('');
  }

  private mostrarError(err: HttpErrorResponse): void {
    this.cargando.set(false);
    this.mensajeError.set(mensajeDeError(err));
  }
}
