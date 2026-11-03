import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReservaBackend, ReservaService } from '../../../../services/reserva.service';
import { mensajeDeError } from '../../../../utils/mensaje-error';
import { FRANJAS_RECOJO, fechaDelLimite, franjaDelLimite, limiteDeRecojo } from '../../../../utils/franjas-recojo';

type Filtro = 'todas' | 'eliminables';

/**
 * Pestaña "Reservas" del admin: ver todas, cambiar la fecha y la franja de recojo de una pendiente (RF15)
 * y eliminar físicamente las canceladas o vencidas (RF17).
 */
@Component({
  selector: 'app-reservas-admin-tab',
  standalone: true,
  imports: [DatePipe, FormsModule],
  templateUrl: './reservas-admin-tab.component.html',
})
export class ReservasAdminTabComponent implements OnInit {
  private reservaService = inject(ReservaService);

  readonly mensajeOk = signal('');
  readonly mensajeError = signal('');
  readonly cargando = signal(false);

  readonly reservas = signal<ReservaBackend[]>([]);
  readonly filtro = signal<Filtro>('todas');
  readonly reservasFiltradas = computed(() =>
    this.filtro() === 'todas' ? this.reservas() : this.reservas().filter(r => this.sePuedeEliminar(r))
  );

  readonly franjas = FRANJAS_RECOJO;
  /** RF15: reserva cuya fecha se está cambiando (null = ninguna). */
  readonly edicion = signal<{ idReserva: number; fecha: string; franja: string } | null>(null);

  ngOnInit(): void {
    this.cargarReservas();
  }

  cargarReservas(): void {
    this.cargando.set(true);
    this.reservaService.getTodas().subscribe({
      next: data => { this.reservas.set(data); this.cargando.set(false); },
      error: err => { this.cargando.set(false); this.mensajeError.set(mensajeDeError(err)); }
    });
  }

  /** RF17: solo se borran las canceladas o vencidas. */
  sePuedeEliminar(r: ReservaBackend): boolean {
    return r.estadoReserva === 'cancelada' || r.estadoReserva === 'vencida';
  }

  // reserva actualizar (front, RF15): solo las pendientes. se abre con la fecha y la franja q tiene ahora
  editarFecha(r: ReservaBackend): void {
    if (!r.fechaRecojoLimite) return;
    this.mensajeOk.set('');
    this.mensajeError.set('');
    this.edicion.set({ idReserva: r.idReserva, fecha: fechaDelLimite(r.fechaRecojoLimite), franja: franjaDelLimite(r.fechaRecojoLimite) });
  }

  guardarFecha(): void {
    const e = this.edicion();
    if (!e) return;
    this.reservaService.actualizar(e.idReserva, limiteDeRecojo(e.fecha, e.franja)).subscribe({
      next: () => {
        this.edicion.set(null);
        this.mensajeOk.set(`Reserva #${e.idReserva}: nueva fecha de recojo ${e.fecha.split('-').reverse().join('/')} (${e.franja}).`);
        this.cargarReservas();
      },
      error: err => this.mensajeError.set(mensajeDeError(err))
    });
  }

  // eliminacion fisica (front): boton eliminar, borra la reserva de la bd (no se puede deshacer)
  eliminar(r: ReservaBackend): void {
    if (!confirm(`¿Eliminar la reserva #${r.idReserva} de "${r.tituloLibro}"? Se borrará definitivamente de la base de datos.`)) return;
    this.mensajeOk.set('');
    this.mensajeError.set('');
    this.reservaService.eliminar(r.idReserva).subscribe({
      next: res => { this.mensajeOk.set(`Reserva #${r.idReserva}: ${res.mensaje}`); this.cargarReservas(); },
      error: err => this.mensajeError.set(mensajeDeError(err))
    });
  }
}
