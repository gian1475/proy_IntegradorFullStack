import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { PrestamoDetalle, PrestamoService } from '../../../../services/prestamo.service';
import { mensajeDeError } from '../../../../utils/mensaje-error';

/** Pestaña "Préstamos activos" del admin: solo consulta. */
@Component({
  selector: 'app-prestamos-tab',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './prestamos-tab.component.html',
})
export class PrestamosTabComponent implements OnInit {
  private prestamoService = inject(PrestamoService);

  readonly prestamos = signal<PrestamoDetalle[]>([]);
  readonly mensajeError = signal('');

  // crud prestamo consultar (front): el admin tambien ve la consulta dos tablas, pero sin botones
  ngOnInit(): void {
    this.prestamoService.listarActivos().subscribe({
      next: data => this.prestamos.set(data),
      error: err => this.mensajeError.set(mensajeDeError(err))
    });
  }
}
