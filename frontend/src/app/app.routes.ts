import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { UserCatalogComponent } from './pages/user-catalog/user-catalog.component';
import { AdminPanelComponent } from './pages/admin-panel/admin-panel.component';
import { EncargadoPanelComponent } from './pages/encargado-panel/encargado-panel.component';
import { rolGuard } from './guards/rol.guard';

// autorizacion (front): cada pantalla tiene un guard con el perfil q puede entrar
// perfil 1 admin -> /admin, perfil 2 encargado -> /encargado, perfil 3 prestatario -> /catalogo
export const routes: Routes = [
  {
    path: '',
    component: HomeComponent,
    title: 'Biblioteca de Alejandría - Inicio',
  },
  {
    path: 'login',
    component: LoginComponent,
    title: 'Biblioteca de Alejandría - Iniciar Sesión',
  },
  {
    path: 'registro',
    component: RegisterComponent,
    title: 'Biblioteca de Alejandría - Registro de Usuario',
  },
  {
    // permisos prestatario: solo el prestatario entra al catalogo y a sus reservas
    path: 'catalogo',
    component: UserCatalogComponent,
    canActivate: [rolGuard(['PRESTATARIO'])],
    title: 'Biblioteca de Alejandría - Catálogo de Libros',
  },
  {
    // permisos admin: solo el admin entra a su panel (libros, prestamos, reportes, monitoreo)
    path: 'admin',
    component: AdminPanelComponent,
    canActivate: [rolGuard(['ADMINISTRADOR'])],
    title: 'Biblioteca de Alejandría - Panel del Administrador',
  },
  {
    // permisos encargado: solo el encargado entra a su panel (prestamos, devoluciones, reservas)
    path: 'encargado',
    component: EncargadoPanelComponent,
    canActivate: [rolGuard(['ENCARGADO'])],
    title: 'Biblioteca de Alejandría - Panel del Encargado',
  },
  {
    path: '**',
    redirectTo: '',
  },
];
