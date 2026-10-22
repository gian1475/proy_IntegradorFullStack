import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { PrestamoService } from '../../../../services/prestamo.service';
import { LibroBackend, LibroService } from '../../../../services/libro.service';
import { ReservaBackend } from '../../../../services/reserva.service';
import { mensajeDeError } from '../../../../utils/mensaje-error';

/** Pestaña "Registrar préstamo" del encargado. */
@Component({
  selector: 'app-registrar-tab',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './registrar-tab.component.html',
})
export class RegistrarTabComponent implements OnInit {
  private prestamoService = inject(PrestamoService);
  private libroService = inject(LibroService);

  /** Si viene de "Entregar" en la pestaña de reservas, el libro llega ya elegido. */
  readonly reserva = input<ReservaBackend | null>(null);

  readonly mensajeOk = signal('');
  readonly mensajeError = signal('');

  readonly libros = signal<LibroBackend[]>([]);
  readonly librosPrestables = computed(() =>
    this.libros().filter(l => l.estadoLibro !== 'mantenimiento')
  );
  dniPrestatario = '';
  idLibroSeleccionado: number | null = null;

  ngOnInit(): void {
    this.cargarLibros();
    const r = this.reserva();
    if (r) {
      this.idLibroSeleccionado = r.idLibro;
      this.mensajeOk.set(`Ingrese el DNI de ${r.nombrePrestatario} para entregar "${r.tituloLibro}"`);
    }
  }

  cargarLibros(): void {
    this.libroService.getLibros().subscribe({
      next: data => this.libros.set(data),
      error: err => this.mostrarError(err)
    });
  }

  // crud prestamo adicionar (front): el encargado pone el dni del prestatario y elige el libro
  registrarPrestamo(): void {
    this.limpiarMensajes();
    if (!/^\d{8}$/.test(this.dniPrestatario.trim()) || !this.idLibroSeleccionado) {
      this.mensajeError.set('Ingrese un DNI de 8 dígitos y seleccione un libro');
      return;
    }
    this.prestamoService.registrar(this.dniPrestatario.trim(), this.idLibroSeleccionado).subscribe({
      next: p => {
        this.mensajeOk.set(`Préstamo #${p.idPrestamo} registrado. Hora límite: ${this.formatearHora(p.fechaDevolucionPrevista)}`);
        this.dniPrestatario = '';
        this.idLibroSeleccionado = null;
        this.cargarLibros();
      },
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
    this.mensajeError.set(mensajeDeError(err));
  }
}
