import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { UserCatalogComponent } from './pages/user-catalog/user-catalog.component';

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
    title: 'Biblioteca de Alejandría - Catálogo de Libros',
  },
  {
    path: '**',
    redirectTo: '',
  },
];
