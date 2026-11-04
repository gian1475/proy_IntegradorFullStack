/**
 * Configuración de producción (npx ng build): el mismo .jar sirve las pantallas y la API, así que la API
 * está en la misma dirección que la página ("/api"). Sirve en la PC (localhost:8080) y en la nube.
 */
export const environment = {
  apiUrl: '/api',
};
