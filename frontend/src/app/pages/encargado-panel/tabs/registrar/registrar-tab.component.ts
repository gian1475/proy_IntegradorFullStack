import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { PrestamoService } from '../../../../services/prestamo.service';
import { LibroBackend, LibroService } from '../../../../services/libro.service';
import { ReservaBackend } from '../../../../services/reserva.service';
import { mensajeDeError } from '../../../../utils/mensaje-error';
import { coincideBusqueda, etiquetaEstado } from '../../../../utils/estado-libro';
import { horaCorta } from '../../../../utils/fechas';

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
  // consulta disponibilidad (RF03): el encargado busca el libro por titulo, autor, categoria o isbn
  readonly busqueda = signal('');
  readonly librosPrestables = computed(() =>
    this.libros().filter(l => l.estadoLibro !== 'mantenimiento' && coincideBusqueda(this.busqueda(), {
      titulo: l.titulo,
      autores: (l.autores ?? []).map(a => `${a.nombre} ${a.apellido}`).join(', '),
      categoria: l.categoria?.nombre ?? '',
      isbn: l.isbn,
    }))
  );
  readonly etiquetaEstado = etiquetaEstado;
  dniPrestatario = '';
  idLibroSeleccionado: number | null = null;

  /** TAP: cuándo empezó a llenar este préstamo (ms del reloj del navegador). null = todavía no empieza. */
  private inicioAtencion: number | null = null;

  ngOnInit(): void {
    this.cargarLibros();
    const r = this.reserva();
    if (r) {
      this.marcarInicio(); // viene de "Entregar" una reserva: la atencion empieza al abrir el formulario
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

  // indicadores TAP (front): la atencion empieza cuando el encargado escribe o elige algo en el formulario
  // (el (input) y el (change) de cualquier campo llegan al <form>). solo se marca la primera vez
  marcarInicio(): void {
    this.inicioAtencion ??= Date.now();
  }

  // crud prestamo adicionar (front): el encargado pone el dni del prestatario y elige el libro
  registrarPrestamo(): void {
    this.limpiarMensajes();
    if (!/^\d{8}$/.test(this.dniPrestatario.trim()) || !this.idLibroSeleccionado) {
      this.mensajeError.set('Ingrese un DNI de 8 dígitos y seleccione un libro');
      return;
    }
    // TAP: se manda cuantos segundos tardo (no la hora: asi no importa si el reloj de la PC esta desfasado)
    const segundos = this.inicioAtencion === null ? null : Math.round((Date.now() - this.inicioAtencion) / 1000);
    this.prestamoService.registrar(this.dniPrestatario.trim(), this.idLibroSeleccionado, segundos).subscribe({
      next: p => {
        // horario de atencion: si el limite quedo antes de las 3 horas es porque se ajusto al cierre (20:00)
        const tresHoras = new Date(p.fechaPrestamo).getTime() + 3 * 60 * 60 * 1000;
        const ajustado = new Date(p.fechaDevolucionPrevista).getTime() < tresHoras;
        this.mensajeOk.set(`Préstamo #${p.idPrestamo} registrado. Hora límite: ${this.formatearHora(p.fechaDevolucionPrevista)}`
          + (ajustado ? ' (ajustada a la hora de cierre de la biblioteca)' : ''));
        this.dniPrestatario = '';
        this.idLibroSeleccionado = null;
        this.inicioAtencion = null; // el siguiente prestamo cuenta desde cero
        this.cargarLibros();
      },
      error: err => this.mostrarError(err)
    });
  }

  private formatearHora(fecha: string): string {
    return horaCorta(fecha); // formatDate de angular (utils/fechas.ts)
  }

  private limpiarMensajes(): void {
    this.mensajeOk.set('');
    this.mensajeError.set('');
  }

  private mostrarError(err: HttpErrorResponse): void {
    this.mensajeError.set(mensajeDeError(err));
  }
}
