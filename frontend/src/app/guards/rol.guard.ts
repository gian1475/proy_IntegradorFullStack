import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Protege una ruta para que solo entren los roles indicados.
 * Sin sesión -> /login. Con otro rol -> la pantalla principal de su perfil.
 */
export const rolGuard = (rolesPermitidos: string[]): CanActivateFn => () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.estaAutenticado()) {
    return router.createUrlTree(['/login']);
  }

  const rol = authService.usuarioActual()?.rol ?? '';
  if (rolesPermitidos.includes(rol)) {
    return true;
  }

  return router.createUrlTree([authService.rutaInicio(rol)]);
};
