/**
 * Configuración de desarrollo (npm start y las pruebas): el backend corre aparte en el puerto 8080.
 * B5 (despliegue): en el build de producción Angular reemplaza este archivo por environment.prod.ts
 * (fileReplacements en angular.json).
 */
export const environment = {
  apiUrl: 'http://localhost:8080/api',
};
