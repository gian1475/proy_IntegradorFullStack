import { HttpErrorResponse } from '@angular/common/http';

/** Cuando el backend no responde (apagado, sin internet o el servidor de Render "dormido"). */
export const MENSAJE_SIN_CONEXION = 'No se pudo conectar con el servidor. Verifique que el sistema esté iniciado e intente de nuevo.';

/** Convierte un error del backend en el texto que se muestra en pantalla. */
// manejo de errores (front): una sola funcion para todas las pantallas, tambien el login y el registro
export function mensajeDeError(err: HttpErrorResponse, porDefecto = 'Ocurrió un error inesperado'): string {
  return err.status === 0 ? MENSAJE_SIN_CONEXION : err.error?.mensaje || porDefecto;
}
