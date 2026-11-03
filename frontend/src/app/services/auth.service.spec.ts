import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

// sesion 15 min (front): el vencimiento lo calcula el navegador con su propio reloj.
// expiraEn del backend llega en hora de Lima SIN zona: un navegador en otra zona lo leia mal
// y daba la sesion por vencida apenas se iniciaba (D15)
describe('sesión del navegador (RNF04)', () => {
  const usuario = { idUsuario: 1, nombre: 'Admin', apellido: 'Principal', dni: '11111111',
                    correo: 'admin@alejandria.edu.pe', estado: 'activo', rol: 'ADMINISTRADOR' };
  let http: HttpTestingController;
  let servicio: AuthService;

  beforeEach(() => {
    localStorage.clear();
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-11-03T02:20:00Z'));
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    servicio = TestBed.inject(AuthService);
  });

  afterEach(() => {
    vi.useRealTimers();
    localStorage.clear();
  });

  function iniciarSesion(expiraEnDelBackend: string) {
    servicio.login('admin@alejandria.edu.pe', 'x').subscribe();
    http.expectOne('http://localhost:8080/api/auth/login')
      .flush({ token: 'abc', expiraEn: expiraEnDelBackend, usuario });
  }

  it('queda conectado aunque la hora del backend se lea como pasada', () => {
    iniciarSesion('2026-11-02T21:35:00'); // 15 min en hora de Lima; en UTC parece de hace 5 horas

    expect(servicio.estaAutenticado()).toBe(true);
    expect(servicio.obtenerToken()).toBe('abc');
  });

  it('se cierra a los 15 minutos sin actividad', () => {
    iniciarSesion('2026-11-02T21:35:00');

    vi.advanceTimersByTime(14 * 60 * 1000);
    expect(servicio.estaAutenticado()).toBe(true);

    vi.advanceTimersByTime(2 * 60 * 1000); // 16 min sin hacer nada
    expect(servicio.estaAutenticado()).toBe(false);
  });
});
