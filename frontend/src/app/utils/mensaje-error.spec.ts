import { HttpErrorResponse } from '@angular/common/http';
import { MENSAJE_SIN_CONEXION, mensajeDeError } from './mensaje-error';

// el texto que ve el usuario cuando falla una peticion (lo usan todas las pantallas, tambien el login y el registro)
describe('mensaje de error', () => {
  it('si el backend no responde, dice que no hay conexión (sin nombrar un puerto: en Render no es el 8080)', () => {
    const sinConexion = new HttpErrorResponse({ status: 0 });

    expect(mensajeDeError(sinConexion)).toBe(MENSAJE_SIN_CONEXION);
    expect(MENSAJE_SIN_CONEXION).not.toContain('8080');
  });

  it('si el backend responde con un mensaje, muestra ese mensaje', () => {
    const credenciales = new HttpErrorResponse({ status: 401, error: { mensaje: 'Credenciales incorrectas' } });

    expect(mensajeDeError(credenciales)).toBe('Credenciales incorrectas');
  });

  it('si el backend no manda mensaje, usa el texto que pide la pantalla', () => {
    const sinMensaje = new HttpErrorResponse({ status: 500 });

    expect(mensajeDeError(sinMensaje)).toBe('Ocurrió un error inesperado');
    expect(mensajeDeError(sinMensaje, 'Error al registrar usuario')).toBe('Error al registrar usuario');
  });
});
