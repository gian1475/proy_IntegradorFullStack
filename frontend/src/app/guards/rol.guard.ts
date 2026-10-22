import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Protege una ruta para que solo entren los roles indicados.
 * Sin sesión -> /login. Con otro rol -> la pantalla principal de su perfil.
 */
// autorizacion (front): este guard se ejecuta antes de abrir una pantalla
export const rolGuard = (rolesPermitidos: string[]): CanActivateFn => () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // si no esta logeado lo manda al login
  if (!authService.estaAutenticado()) {
    return router.createUrlTree(['/login']);
  }

  // si su rol esta permitido entra
  const rol = authService.usuarioActual()?.rol ?? '';
  if (rolesPermitidos.includes(rol)) {
    return true;
  }

  // si es otro rol lo regresa a su pantalla (ej: un prestatario q escribe /admin en la url vuelve a /catalogo)
  return router.createUrlTree([authService.rutaInicio(rol)]);
};
