import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { UserCatalogComponent } from './user-catalog.component';

// RF15: el prestatario cambia la fecha y la franja de recojo de su reserva pendiente desde "Mis Reservas"
describe('Mis Reservas del prestatario (RF15)', () => {
  const API = 'http://localhost:8080/api';
  const reserva = { idReserva: 7, idLibro: 3, tituloLibro: 'Rayuela', isbnLibro: '1', autor: 'Julio Cortázar',
                    categoria: 'Literatura', idPrestatario: 5, nombrePrestatario: 'Valeria Flores',
                    fechaReserva: '2026-10-28T09:00:00', fechaRecojoLimite: '2026-10-29T11:30:00', posicionCola: 1,
                    estadoReserva: 'pendiente' };
  const enCola = { ...reserva, idReserva: 8, fechaRecojoLimite: null, estadoReserva: 'en_espera', posicionCola: 2 };
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.setItem('sesion_alejandria', JSON.stringify({
      token: 't', expiraEn: new Date(Date.now() + 15 * 60000).toISOString(),
      usuario: { idUsuario: 5, nombre: 'Valeria', apellido: 'Flores', dni: '73456783', correo: 'v@x.pe', estado: 'activo', rol: 'PRESTATARIO' }
    }));
    TestBed.configureTestingModule({
      imports: [UserCatalogComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => localStorage.clear());

  function abrir(prestamos: object[] = []) {
    const fixture = TestBed.createComponent(UserCatalogComponent);
    fixture.detectChanges();
    http.match(r => r.url.includes('/libros')).forEach(r => r.flush([]));
    http.expectOne(`${API}/reservas/usuario/5`).flush([reserva, enCola]);
    http.match(r => r.url.includes('/prestamos')).forEach(r => r.flush(prestamos));
    fixture.componentInstance.selectMenu('reservas');
    fixture.detectChanges();
    return fixture;
  }

  it('solo la reserva con fecha de recojo tiene "Cambiar fecha"', () => {
    const fixture = abrir();
    const tarjetas: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('.book-card'));
    const textos = tarjetas.map(t => Array.from(t.querySelectorAll('button')).map(b => b.textContent!.trim()));
    expect(textos).toEqual([['Cambiar fecha', 'Cancelar'], ['Salir de la cola']]);
  });

  it('cambiar fecha abre con la fecha y la franja actuales y guarda con PUT', () => {
    const fixture = abrir();
    const c = fixture.componentInstance;

    c.abrirModalCambiarFecha(c.misReservas()[0]);
    expect(c.fechaNueva).toBe('2026-10-29');
    expect(c.franjaNueva).toBe('08:30 - 11:30');

    c.fechaNueva = '2026-10-30';
    c.franjaNueva = '17:30 - 20:00';
    c.guardarCambioFecha();

    const req = http.expectOne(r => r.method === 'PUT' && r.url === `${API}/reservas/7`);
    expect(req.request.body).toEqual({ fechaRecojoLimite: '2026-10-30T20:00:00' });
    req.flush({ ...reserva, fechaRecojoLimite: '2026-10-30T20:00:00' });
    http.expectOne(`${API}/reservas/usuario/5`).flush([{ ...reserva, fechaRecojoLimite: '2026-10-30T20:00:00' }]);

    expect(c.modalCambiarFecha()).toBeNull();
    expect(c.fechaCambiada()).toBe(true);
  });

  // B9 (RF07: "el historial muestra todos los préstamos"): el anulado también sale, con su estado
  it('Mis Préstamos muestra también los préstamos anulados con su estado', () => {
    const base = { idPrestatario: 5, idEncargado: 2, idLibro: 3, fechaPrestamo: '2026-10-20T10:00:00',
                   fechaDevolucionPrevista: '2026-10-20T13:00:00', fechaDevolucionReal: null, ampliado: false,
                   estadoFisicoDevolucion: null };
    const fixture = abrir([{ ...base, idPrestamo: 1, estadoPrestamo: 'devuelto' },
                           { ...base, idPrestamo: 2, estadoPrestamo: 'anulado' }]);
    const c = fixture.componentInstance;

    expect(c.misPrestamos().map(p => p.estadoPrestamo)).toEqual(['devuelto', 'anulado']);
    expect(c.prestamosEnCurso()).toBe(0); // el anulado no cuenta como "en curso"
  });

  it('si el backend no deja cambiarla, muestra su mensaje en el modal', () => {
    const fixture = abrir();
    const c = fixture.componentInstance;
    c.abrirModalCambiarFecha(c.misReservas()[0]);

    c.guardarCambioFecha();
    http.expectOne(`${API}/reservas/7`).flush(
      { mensaje: 'El recojo debe ser en el futuro y dentro de los próximos 7 días' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(c.modalCambiarFecha()).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('dentro de los próximos 7 días');
  });
});
