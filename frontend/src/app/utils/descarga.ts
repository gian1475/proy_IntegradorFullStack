/**
 * Guarda en la PC un archivo que llegó del backend (reportes, comprobantes y constancias).
 * Se pide con HttpClient (para que viaje el token) y aquí se crea un enlace temporal que se "clickea".
 */
// descarga (front): antes estaba dentro de ReporteService; ahora la usan reportes, comprobantes y constancias
export function guardarArchivo(archivo: Blob, nombre: string): void {
  const url = URL.createObjectURL(archivo);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombre;
  enlace.click();
  URL.revokeObjectURL(url);
}
