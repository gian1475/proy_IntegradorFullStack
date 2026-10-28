import { Component, inject, signal } from '@angular/core';
import { HeaderComponent } from '../../components/header/header.component';
import { AuthService } from '../../services/auth.service';
import { DashboardTabComponent } from './tabs/dashboard/dashboard-tab.component';
import { LibrosTabComponent } from './tabs/libros/libros-tab.component';
import { PrestamosTabComponent } from './tabs/prestamos/prestamos-tab.component';
import { ReservasAdminTabComponent } from './tabs/reservas/reservas-admin-tab.component';
import { ReportesTabComponent } from './tabs/reportes/reportes-tab.component';
import { MonitoreoTabComponent } from './tabs/monitoreo/monitoreo-tab.component';
import { EncargadosTabComponent } from './tabs/encargados/encargados-tab.component';
import { HistorialPrestamosComponent } from '../../components/historial-prestamos/historial-prestamos.component';

// permisos admin: lo q ve el admin -> dashboard, libros (crud), prestamos activos, historial, reservas (eliminacion fisica),
// encargados (registrarlos, RF09), reportes y monitoreo
type Pestana = 'dashboard' | 'libros' | 'prestamos' | 'historial' | 'reservas' | 'encargados' | 'reportes' | 'monitoreo';

/** Panel del Administrador: solo muestra las pestañas. Cada pestaña es un componente en tabs/. */
@Component({
  selector: 'app-admin-panel',
  standalone: true,
  imports: [HeaderComponent, DashboardTabComponent, LibrosTabComponent, PrestamosTabComponent, HistorialPrestamosComponent,
            ReservasAdminTabComponent, EncargadosTabComponent, ReportesTabComponent, MonitoreoTabComponent],
  templateUrl: './admin-panel.component.html',
})
export class AdminPanelComponent {
  authService = inject(AuthService);

  readonly pestana = signal<Pestana>('dashboard');
}
