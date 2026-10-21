import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { HeaderComponent } from '../../components/header/header.component';
import { AuthService } from '../../services/auth.service';
import { PrestamoDetalle, PrestamoService } from '../../services/prestamo.service';
import { LibroBackend, LibroService } from '../../services/libro.service';
import { ReservaBackend, ReservaService } from '../../services/reserva.service';

type Pestana = 'activos' | 'registrar' | 'reservas';

@Component({
  selector: 'app-encargado-panel',
  standalone: true,
  imports: [HeaderComponent, FormsModule, DatePipe],
  templateUrl: './encargado-panel.component.html',
  styleUrl: '../panel-base.less',
})
export class EncargadoPanelComponent implements OnInit {
  authService = inject(AuthService);
  private prestamoService = inject(PrestamoService);
  private libroService = inject(LibroService);
  private reservaService = inject(ReservaService);

  readonly pestana = signal<Pestana>('activos');
  readonly mensajeOk = signal('');
  readonly mensajeError = signal('');
  readonly cargando = signal(false);

  // Préstamos activos (consulta con JOIN)
  readonly prestamos = signal<PrestamoDetalle[]>([]);

  // Registrar préstamo
  readonly libros = signal<LibroBackend[]>([]);
  readonly librosPrestables = computed(() =>
    this.libros().filter(l => l.estadoLibro !== 'mantenimiento')
  );
  dniPrestatario = '';
  idLibroSeleccionado: number | null = null;

  // Devolución
  readonly prestamoADevolver = signal<PrestamoDetalle | null>(null);
  estadoFisico: 'bueno' | 'dañado' = 'bueno';

  // Reservas pendientes
  readonly reservasPendientes = signal<ReservaBackend[]>([]);

  ngOnInit(): void {
    this.cargarPrestamos();
  }

  cambiarPestana(p: Pestana): void {
    this.pestana.set(p);
    this.limpiarMensajes();
    if (p === 'activos') this.cargarPrestamos();
    if (p === 'registrar') this.cargarLibros();
    if (p === 'reservas') this.cargarReservas();
  }

  cargarPrestamos(): void {
    this.cargando.set(true);
    this.prestamoService.listarActivos().subscribe({
      next: data => { this.prestamos.set(data); this.cargando.set(false); },
      error: err => this.mostrarError(err)
    });
  }

  cargarLibros(): void {
    this.libroService.getLibros().subscribe({
      next: data => this.libros.set(data),
      error: err => this.mostrarError(err)
    });
  }

  cargarReservas(): void {
    this.cargando.set(true);
    this.reservaService.getTodas().subscribe({
      next: data => {
        this.reservasPendientes.set(data.filter(r => r.estadoReserva === 'pendiente'));
        this.cargando.set(false);
      },
      error: err => this.mostrarError(err)
    });
  }

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

  /** Atajo desde la pestaña de reservas: precarga el formulario de préstamo. */
  prestarDesdeReserva(r: ReservaBackend): void {
    this.pestana.set('registrar');
    this.limpiarMensajes();
    this.cargarLibros();
    this.idLibroSeleccionado = r.idLibro;
    this.dniPrestatario = '';
    this.mensajeOk.set(`Ingrese el DNI de ${r.nombrePrestatario} para entregar "${r.tituloLibro}"`);
  }

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

  confirmarDevolucion(): void {
    const p = this.prestamoADevolver();
    if (!p) return;
    this.limpiarMensajes();
    this.prestamoService.devolver(p.idPrestamo, this.estadoFisico).subscribe({
      next: () => {
        this.prestamoADevolver.set(null);
        this.mensajeOk.set(`Devolución de "${p.tituloLibro}" registrada (estado: ${this.estadoFisico})`);
        this.cargarPrestamos();
      },
      error: err => { this.prestamoADevolver.set(null); this.mostrarError(err); }
    });
  }

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
    this.mensajeError.set(err.status === 0
      ? 'No se pudo conectar con el backend (puerto 8080)'
      : err.error?.mensaje || 'Ocurrió un error inesperado');
  }
}
