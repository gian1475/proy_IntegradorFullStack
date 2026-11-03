import { formatDate, registerLocaleData } from '@angular/common';
import localeEsPe from '@angular/common/locales/es-PE';

/**
 * L3: todas las fechas de la pantalla se arman con formatDate de @angular/common (ya viene con Angular).
 * Antes se armaban a mano con padStart y toLocaleTimeString en 5 lugares.
 */
// formato de fechas: los datos del idioma es-PE (a. m. / p. m.) tambien vienen en @angular/common
registerLocaleData(localeEsPe);
const PERU = 'es-PE';

/** AAAA-MM-DD, el formato que pide un input type="date". */
export function fechaInput(fecha: Date): string {
  return formatDate(fecha, 'yyyy-MM-dd', PERU);
}

/** "06:21 p. m." (préstamos y reservas). */
export function horaCorta(fecha: string | Date): string {
  return formatDate(fecha, 'hh:mm a', PERU);
}

/** "2/11/2026". */
export function fechaCorta(fecha: string | Date): string {
  return formatDate(fecha, 'd/M/yyyy', PERU);
}

/** "02/11" (sanciones). */
export function diaMes(fecha: string | Date): string {
  return formatDate(fecha, 'dd/MM', PERU);
}

/** "20:00" (sanciones). */
export function horaDe24(fecha: string | Date): string {
  return formatDate(fecha, 'HH:mm', PERU);
}
