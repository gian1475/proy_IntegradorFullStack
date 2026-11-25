import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { rolGuard } from './guards/rol.guard';

// autorizacion (front): cada pantalla tiene un guard con el perfil q puede entrar
// perfil 1 admin -> /admin, perfil 2 encargado -> /encargado, perfil 3 prestatario -> /catalogo
// D19 (lazy loading de Angular): las pantallas de cada perfil se descargan recien al entrar (loadComponent).
// el guard se revisa antes, asi q nadie descarga la pantalla de un perfil q no es el suyo
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
    loadComponent: () => import('./pages/user-catalog/user-catalog.component').then(m => m.UserCatalogComponent),
    canActivate: [rolGuard(['PRESTATARIO'])],
    title: 'Biblioteca de Alejandría - Catálogo de Libros',
  },
  {
    // permisos admin: solo el admin entra a su panel (libros, prestamos, reportes, monitoreo)
    path: 'admin',
    loadComponent: () => import('./pages/admin-panel/admin-panel.component').then(m => m.AdminPanelComponent),
    canActivate: [rolGuard(['ADMINISTRADOR'])],
    title: 'Biblioteca de Alejandría - Panel del Administrador',
  },
  {
    // permisos encargado: solo el encargado entra a su panel (prestamos, devoluciones, reservas)
    path: 'encargado',
    loadComponent: () => import('./pages/encargado-panel/encargado-panel.component').then(m => m.EncargadoPanelComponent),
    canActivate: [rolGuard(['ENCARGADO'])],
    title: 'Biblioteca de Alejandría - Panel del Encargado',
  },
  {
    path: '**',
    redirectTo: '',
  },
];
