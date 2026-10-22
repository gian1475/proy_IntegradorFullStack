import { HttpErrorResponse } from '@angular/common/http';

/** Convierte un error del backend en el texto que se muestra en pantalla. */
export function mensajeDeError(err: HttpErrorResponse): string {
  return err.status === 0
    ? 'No se pudo conectar con el backend (puerto 8080)'
    : err.error?.mensaje || 'Ocurrió un error inesperado';
}
