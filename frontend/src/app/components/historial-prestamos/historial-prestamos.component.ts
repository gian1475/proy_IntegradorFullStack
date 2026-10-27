import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { FiltrosHistorial, HistorialPrestamo, PrestamoService } from '../../services/prestamo.service';
import { mensajeDeError } from '../../utils/mensaje-error';

/** Historial de préstamos (RF07) con filtros. Lo usan los paneles del encargado y del administrador. */
@Component({
  selector: 'app-historial-prestamos',
  standalone: true,
  imports: [DatePipe, FormsModule],
  templateUrl: './historial-prestamos.component.html',
})
export class HistorialPrestamosComponent implements OnInit {
  private prestamoService = inject(PrestamoService);

  filtros: FiltrosHistorial = { dni: '', libro: '', desde: '', hasta: '' };

  readonly prestamos = signal<HistorialPrestamo[]>([]);
  readonly cargando = signal(false);
  readonly mensajeError = signal('');

  ngOnInit(): void {
    this.buscar();
  }

  // historial (front): filtra por prestatario (dni), libro (titulo o isbn) y rango de fechas. los vacios no filtran
  buscar(): void {
    this.mensajeError.set('');
    const { dni, desde, hasta } = this.filtros;
    if (dni.trim() && !/^\d{8}$/.test(dni.trim())) {
      this.mensajeError.set('El DNI debe tener 8 dígitos');
      return;
    }
    if (desde && hasta && desde > hasta) {
      this.mensajeError.set("La fecha 'desde' no puede ser posterior a la fecha 'hasta'");
      return;
    }
    this.cargando.set(true);
    this.prestamoService.historial(this.filtros).subscribe({
      next: data => { this.prestamos.set(data); this.cargando.set(false); },
      error: (err: HttpErrorResponse) => { this.cargando.set(false); this.mensajeError.set(mensajeDeError(err)); }
    });
  }

  limpiar(): void {
    this.filtros = { dni: '', libro: '', desde: '', hasta: '' };
    this.buscar();
  }
}
