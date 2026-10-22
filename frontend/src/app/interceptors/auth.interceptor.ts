import { HttpErrorResponse, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, tap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Agrega el token "Authorization: Bearer ..." a cada petición al backend.
 * Si la respuesta es correcta, renueva los 15 minutos de la sesión.
 * Si el backend responde 401 (token vencido o inválido), cierra la sesión y manda al login.
 */
// token: a cada peticion se le pega el token para q el backend sepa quien es y q rol tiene
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.obtenerToken();

  const peticion = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(peticion).pipe(
    // sesion 15 min (front): hubo actividad, se renueva el plazo
    tap(evento => {
      if (token && evento instanceof HttpResponse) {
        authService.renovarSesion();
      }
    }),
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && token) {
        authService.cerrarSesionLocal();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
