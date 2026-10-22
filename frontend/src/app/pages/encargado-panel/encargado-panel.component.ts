import { Component, inject, signal } from '@angular/core';
import { HeaderComponent } from '../../components/header/header.component';
import { AuthService } from '../../services/auth.service';
import { ReservaBackend } from '../../services/reserva.service';
import { ActivosTabComponent } from './tabs/activos/activos-tab.component';
import { RegistrarTabComponent } from './tabs/registrar/registrar-tab.component';
import { ReservasTabComponent } from './tabs/reservas/reservas-tab.component';

// permisos encargado: lo q ve el encargado -> prestamos activos, registrar prestamo y reservas pendientes
type Pestana = 'activos' | 'registrar' | 'reservas';

/** Panel del Encargado: solo muestra las pestañas. Cada pestaña es un componente en tabs/. */
@Component({
  selector: 'app-encargado-panel',
  standalone: true,
  imports: [HeaderComponent, ActivosTabComponent, RegistrarTabComponent, ReservasTabComponent],
  templateUrl: './encargado-panel.component.html',
})
export class EncargadoPanelComponent {
  authService = inject(AuthService);

  readonly pestana = signal<Pestana>('activos');

  /** Reserva que se va a entregar: la pestaña "Registrar préstamo" la usa para precargar el libro. */
  readonly reservaAEntregar = signal<ReservaBackend | null>(null);

  cambiarPestana(p: Pestana): void {
    this.reservaAEntregar.set(null);
    this.pestana.set(p);
  }

  /** Botón "Entregar" de la pestaña de reservas: pasa a "Registrar préstamo" con el libro ya elegido. */
  entregar(reserva: ReservaBackend): void {
    this.reservaAEntregar.set(reserva);
    this.pestana.set('registrar');
  }
}
