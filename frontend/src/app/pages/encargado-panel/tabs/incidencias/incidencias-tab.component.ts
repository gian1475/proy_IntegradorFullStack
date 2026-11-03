import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { HistorialPrestamo, PrestamoService } from '../../../../services/prestamo.service';
import { Incidencia, IncidenciaService } from '../../../../services/incidencia.service';
import { mensajeDeError } from '../../../../utils/mensaje-error';
import { TipoIncidencia, incidenciasPosibles, textoSancion } from '../../../../utils/sancion';

/** Pestaña "Incidencias" del encargado (RF06, CU09): registrar una incidencia con su sanción y cumplirla o cancelarla. */
@Component({
  selector: 'app-incidencias-tab',
  standalone: true,
  imports: [FormsModule, DatePipe],
  templateUrl: './incidencias-tab.component.html',
})
export class IncidenciasTabComponent implements OnInit {
  private incidenciaService = inject(IncidenciaService);
  private prestamoService = inject(PrestamoService);

  readonly mensajeOk = signal('');
  readonly mensajeError = signal('');
  readonly cargando = signal(true);
  readonly incidencias = signal<Incidencia[]>([]);
  readonly textoSancion = textoSancion;

  // ---------- Formulario ----------
  dni = '';
  /** Préstamos del DNI buscado a los que les corresponde alguna incidencia. */
  readonly prestamos = signal<HistorialPrestamo[]>([]);
  readonly idPrestamo = signal<number | null>(null);
  readonly prestamoElegido = computed(() => this.prestamos().find(p => p.idPrestamo === this.idPrestamo()) ?? null);
  readonly tiposPosibles = computed<TipoIncidencia[]>(() => {
    const p = this.prestamoElegido();
    return p ? incidenciasPosibles(p, new Date(), this.yaRegistradas(p.idPrestamo)) : [];
  });
  readonly tipoIncidencia = signal<TipoIncidencia | ''>('');
  readonly tipoSancion = signal<'advertencia' | 'suspensión' | 'multa'>('advertencia');
  descripcion = '';
  diasSuspension: number | null = 7;
  monto: number | null = null;

  ngOnInit(): void {
    this.cargarIncidencias();
  }

  cargarIncidencias(): void {
    this.cargando.set(true);
    this.incidenciaService.listar().subscribe({
      next: data => { this.incidencias.set(data); this.cargando.set(false); },
      error: err => { this.cargando.set(false); this.mostrarError(err); }
    });
  }

  // registrar incidencia (front): 1) busca los prestamos del dni (historial) 2) elige el prestamo
  // 3) solo se ofrecen las incidencias q le corresponden (utils/sancion.ts) 4) elige la sancion
  buscarPrestamos(): void {
    this.limpiarMensajes();
    this.idPrestamo.set(null);
    this.tipoIncidencia.set('');
    if (!/^\d{8}$/.test(this.dni.trim())) {
      this.mensajeError.set('Ingrese un DNI de 8 dígitos');
      return;
    }
    this.prestamoService.historial({ dni: this.dni, libro: '', desde: '', hasta: '' }).subscribe({
      next: data => {
        const conIncidencia = data.filter(p => incidenciasPosibles(p, new Date(), this.yaRegistradas(p.idPrestamo)).length > 0);
        this.prestamos.set(conIncidencia);
        if (conIncidencia.length === 0) {
          this.mensajeError.set('Ese DNI no tiene préstamos con retraso, devolución o libro sin devolver');
        }
      },
      error: err => this.mostrarError(err)
    });
  }

  /** Tipos de incidencia que ese préstamo ya tiene (salen de la tabla de abajo). */
  private yaRegistradas(idPrestamo: number): string[] {
    return this.incidencias().filter(i => i.idPrestamo === idPrestamo).map(i => i.tipoIncidencia);
  }

  elegirPrestamo(id: number | null): void {
    this.idPrestamo.set(id);
    this.tipoIncidencia.set(this.tiposPosibles()[0] ?? '');
  }

  registrar(): void {
    this.limpiarMensajes();
    const idPrestamo = this.idPrestamo();
    const tipo = this.tipoIncidencia();
    if (!idPrestamo || !tipo) {
      this.mensajeError.set('Elija el préstamo y el tipo de incidencia');
      return;
    }
    const sancion = this.tipoSancion();
    if (tipo === 'pérdida' && !confirm('El préstamo se cerrará como devolución dañada: el ejemplar no vuelve al stock. ¿Continuar?')) {
      return;
    }
    this.incidenciaService.registrar({
      idPrestamo,
      tipoIncidencia: tipo,
      descripcion: this.descripcion.trim(),
      tipoSancion: sancion,
      diasSuspension: sancion === 'suspensión' ? this.diasSuspension : null,
      monto: sancion === 'multa' ? this.monto : null,
    }).subscribe({
      next: i => {
        this.mensajeOk.set(`Incidencia #${i.idIncidencia} registrada: ${textoSancion(i)} para ${i.prestatario}`);
        this.limpiarFormulario();
        this.cargarIncidencias();
      },
      error: err => this.mostrarError(err)
    });
  }

  // pago de multa (front): la multa pasa a cumplida y el prestatario ya no queda bloqueado por ella
  registrarPago(i: Incidencia): void {
    if (!i.idSancion || !confirm(`¿Registrar el pago de la multa de ${i.prestatario}?`)) return;
    this.limpiarMensajes();
    this.incidenciaService.registrarPago(i.idSancion).subscribe({
      next: () => { this.mensajeOk.set(`Multa de ${i.prestatario} pagada`); this.cargarIncidencias(); },
      error: err => this.mostrarError(err)
    });
  }

  cancelarSancion(i: Incidencia): void {
    if (!i.idSancion || !confirm(`¿Cancelar la ${i.tipoSancion} de ${i.prestatario}? Úselo solo si se registró por error.`)) return;
    this.limpiarMensajes();
    this.incidenciaService.cancelarSancion(i.idSancion).subscribe({
      next: () => { this.mensajeOk.set(`Sanción de ${i.prestatario} cancelada`); this.cargarIncidencias(); },
      error: err => this.mostrarError(err)
    });
  }

  private limpiarFormulario(): void {
    this.dni = '';
    this.prestamos.set([]);
    this.idPrestamo.set(null);
    this.tipoIncidencia.set('');
    this.tipoSancion.set('advertencia');
    this.descripcion = '';
    this.diasSuspension = 7;
    this.monto = null;
  }

  private limpiarMensajes(): void {
    this.mensajeOk.set('');
    this.mensajeError.set('');
  }

  private mostrarError(err: HttpErrorResponse): void {
    this.mensajeError.set(mensajeDeError(err));
  }
}
