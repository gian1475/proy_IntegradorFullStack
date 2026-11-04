import { Component, OnInit, computed, inject, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ReservaBackend, ReservaService } from '../../../../services/reserva.service';
import { LibroBackend, LibroService } from '../../../../services/libro.service';
import { mensajeDeError } from '../../../../utils/mensaje-error';
import { coincideBusqueda, etiquetaEstado } from '../../../../utils/estado-libro';
import { FRANJAS_RECOJO, limiteDeRecojo } from '../../../../utils/franjas-recojo';
import { fechaInput } from '../../../../utils/fechas';

/**
 * Pestaña "Reservas pendientes" del encargado: entregar o cancelar las pendientes, ver y cancelar la cola de espera
 * (RF04 y RF16) y reservar en la ventanilla a nombre de un prestatario (B2, actor Encargado de RF04).
 */
@Component({
  selector: 'app-reservas-tab',
  standalone: true,
  imports: [DatePipe, FormsModule],
  templateUrl: './reservas-tab.component.html',
})
export class ReservasTabComponent implements OnInit {
  private reservaService = inject(ReservaService);
  private libroService = inject(LibroService);

  /** Avisa al panel que se va a entregar esta reserva (pasa a "Registrar préstamo"). */
  readonly entregar = output<ReservaBackend>();

  readonly reservasPendientes = signal<ReservaBackend[]>([]);
  readonly enCola = signal<ReservaBackend[]>([]);
  readonly mensajeOk = signal('');
  readonly mensajeError = signal('');
  readonly cargando = signal(false);

  // reservar en ventanilla
  readonly libros = signal<LibroBackend[]>([]);
  readonly busqueda = signal('');
  readonly librosReservables = computed(() => this.libros().filter(l => l.estadoLibro !== 'mantenimiento'
    && coincideBusqueda(this.busqueda(), {
      titulo: l.titulo,
      autores: (l.autores ?? []).map(a => `${a.nombre} ${a.apellido}`).join(', '),
      categoria: l.categoria?.nombre ?? '',
      isbn: l.isbn,
    })));
  readonly franjas = FRANJAS_RECOJO;
  readonly etiquetaEstado = etiquetaEstado;
  dni = '';
  idLibro: number | null = null;
  fecha = fechaInput(new Date());
  franja = FRANJAS_RECOJO[0].valor;

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.reservaService.getTodas().subscribe({
      next: data => {
        this.reservasPendientes.set(data.filter(r => r.estadoReserva === 'pendiente'));
        // cola de espera (front): por libro y en orden de puesto
        this.enCola.set(data
          .filter(r => r.estadoReserva === 'en_espera')
          .sort((a, b) => a.tituloLibro.localeCompare(b.tituloLibro) || a.posicionCola - b.posicionCola));
        this.cargando.set(false);
      },
      error: err => {
        this.cargando.set(false);
        this.mensajeError.set(mensajeDeError(err));
      }
    });
    this.libroService.getLibros().subscribe({
      next: data => this.libros.set(data),
      error: err => this.mensajeError.set(mensajeDeError(err))
    });
  }

  /** El libro elegido no tiene stock: no se pide fecha, el prestatario entra a la cola de espera. */
  sinStock(): boolean {
    return this.libros().find(l => l.idLibro === this.idLibro)?.stock === 0;
  }

  // reserva ventanilla (front, B2): mismas reglas q el prestatario (las revisa el backend: sancion, 7 dias, horario)
  reservarEnVentanilla(): void {
    this.limpiarMensajes();
    if (!/^\d{8}$/.test(this.dni.trim()) || !this.idLibro) {
      this.mensajeError.set('Ingrese un DNI de 8 dígitos y seleccione un libro');
      return;
    }
    const limite = this.sinStock() ? null : limiteDeRecojo(this.fecha, this.franja);
    this.reservaService.crearEnVentanilla(this.dni.trim(), this.idLibro, limite).subscribe({
      next: r => {
        this.mensajeOk.set(r.estadoReserva === 'en_espera'
          ? `Sin ejemplares: ${r.nombrePrestatario} entró a la cola de espera (puesto ${r.posicionCola}) de "${r.tituloLibro}".`
          : `Reserva #${r.idReserva} registrada para ${r.nombrePrestatario}: "${r.tituloLibro}".`);
        this.dni = '';
        this.idLibro = null;
        this.cargar();
      },
      error: (err: HttpErrorResponse) => this.mensajeError.set(mensajeDeError(err))
    });
  }

  // reserva cancelar (front, B2): el encargado cancela una pendiente o saca a alguien de la cola (RF16)
  cancelar(r: ReservaBackend): void {
    const que = r.estadoReserva === 'en_espera' ? 'Sacar de la cola de espera a' : 'Cancelar la reserva de';
    if (!confirm(`¿${que} ${r.nombrePrestatario} ("${r.tituloLibro}")?`)) return;
    this.limpiarMensajes();
    this.reservaService.cancelar(r.idReserva).subscribe({
      next: res => { this.mensajeOk.set(`Reserva #${r.idReserva}: ${res.mensaje}`); this.cargar(); },
      error: (err: HttpErrorResponse) => this.mensajeError.set(mensajeDeError(err))
    });
  }

  private limpiarMensajes(): void {
    this.mensajeOk.set('');
    this.mensajeError.set('');
  }
}
