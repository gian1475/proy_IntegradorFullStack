import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { UserCatalogComponent } from './pages/user-catalog/user-catalog.component';

export const routes: Routes = [
  {
    path: '',
    component: HomeComponent,
    title: 'Luz del Saber - Inicio',
  },
  {
    path: 'login',
    component: LoginComponent,
    title: 'Luz del Saber - Iniciar Sesión',
  },
  {
    path: 'registro',
    component: RegisterComponent,
    title: 'Luz del Saber - Registro de Usuario',
  },
  {
    path: 'catalogo',
    component: UserCatalogComponent,
    title: 'Luz del Saber - Catálogo de Libros',
  },
  {
    path: '**',
    redirectTo: '',
  },
];
