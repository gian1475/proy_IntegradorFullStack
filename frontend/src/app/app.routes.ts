import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { UserCatalogComponent } from './pages/user-catalog/user-catalog.component';
import { AdminPanelComponent } from './pages/admin-panel/admin-panel.component';
import { EncargadoPanelComponent } from './pages/encargado-panel/encargado-panel.component';
import { rolGuard } from './guards/rol.guard';

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
    path: 'catalogo',
    component: UserCatalogComponent,
    canActivate: [rolGuard(['PRESTATARIO'])],
    title: 'Biblioteca de Alejandría - Catálogo de Libros',
  },
  {
    path: 'admin',
    component: AdminPanelComponent,
    canActivate: [rolGuard(['ADMINISTRADOR'])],
    title: 'Biblioteca de Alejandría - Panel del Administrador',
  },
  {
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
