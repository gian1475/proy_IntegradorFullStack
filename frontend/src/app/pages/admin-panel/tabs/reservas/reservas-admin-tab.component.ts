import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ReservaBackend, ReservaService } from '../../../../services/reserva.service';
import { mensajeDeError } from '../../../../utils/mensaje-error';

type Filtro = 'todas' | 'eliminables';

/** Pestaña "Reservas" del admin: ver todas y eliminar físicamente las canceladas o vencidas (RF17). */
@Component({
  selector: 'app-reservas-admin-tab',
  standalone: true,
  imports: [DatePipe],
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
