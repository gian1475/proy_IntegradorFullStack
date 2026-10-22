import { Component, OnInit, inject, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ReservaBackend, ReservaService } from '../../../../services/reserva.service';
import { mensajeDeError } from '../../../../utils/mensaje-error';

/** Pestaña "Reservas pendientes" del encargado. */
@Component({
  selector: 'app-reservas-tab',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './reservas-tab.component.html',
})
export class ReservasTabComponent implements OnInit {
  private reservaService = inject(ReservaService);

  /** Avisa al panel que se va a entregar esta reserva (pasa a "Registrar préstamo"). */
  readonly entregar = output<ReservaBackend>();

  readonly reservasPendientes = signal<ReservaBackend[]>([]);
  readonly mensajeError = signal('');
  readonly cargando = signal(false);

  ngOnInit(): void {
    this.cargando.set(true);
    this.reservaService.getTodas().subscribe({
      next: data => {
        this.reservasPendientes.set(data.filter(r => r.estadoReserva === 'pendiente'));
        this.cargando.set(false);
      },
      error: err => {
        this.cargando.set(false);
        this.mensajeError.set(mensajeDeError(err));
      }
    });
  }
}
