import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReservasTabComponent } from './reservas-tab.component';

// B2 (RF04 y RF16, actor Encargado): en la ventanilla el encargado reserva a nombre de un prestatario y cancela
describe('Reservas pendientes del encargado (RF04 y RF16)', () => {
  const API = 'http://localhost:8080/api';
  const base = { idLibro: 3, tituloLibro: 'Rayuela', isbnLibro: '1', autor: 'Julio Cortázar', categoria: 'Literatura',
                 idPrestatario: 5, nombrePrestatario: 'Valeria Flores', fechaReserva: '2026-10-28T09:00:00' };
  const pendiente = { ...base, idReserva: 7, fechaRecojoLimite: '2026-10-29T11:30:00', posicionCola: 1, estadoReserva: 'pendiente' };
  const enCola = { ...base, idReserva: 8, fechaRecojoLimite: null, posicionCola: 1, estadoReserva: 'en_espera' };
  const libros = [
    { idLibro: 3, titulo: 'Rayuela', isbn: '1', anioPublicacion: 1963, editorial: 'X', stock: 2, estadoLibro: 'disponible', autores: [] },
    { idLibro: 4, titulo: 'Cosmos', isbn: '2', anioPublicacion: 1980, editorial: 'Y', stock: 0, estadoLibro: 'reservado', autores: [] },
  ];
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ReservasTabComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => vi.restoreAllMocks());

  function abrir() {
    const fixture = TestBed.createComponent(ReservasTabComponent);
    fixture.detectChanges();
    http.expectOne(`${API}/reservas`).flush([pendiente, enCola]);
    http.expectOne(`${API}/libros`).flush(libros);
    fixture.detectChanges();
    return fixture;
  }

  function recargar() {
    http.expectOne(`${API}/reservas`).flush([]);
    http.expectOne(`${API}/libros`).flush(libros);
  }

  it('la pendiente y la de la cola tienen "Cancelar"', () => {
    const fixture = abrir();
    const filas: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('tbody tr'));
    const textos = filas.map(f => Array.from(f.querySelectorAll('button')).map(b => b.textContent!.trim()));
    expect(textos).toEqual([['Entregar', 'Cancelar'], ['Cancelar']]);
  });

  it('cancelar pide confirmación y manda PUT a /cancelar', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const fixture = abrir();

    fixture.componentInstance.cancelar(pendiente);

    http.expectOne(r => r.method === 'PUT' && r.url === `${API}/reservas/7/cancelar`).flush({ mensaje: 'Reserva cancelada correctamente' });
    recargar();
  });

  it('reserva en ventanilla con el DNI, el libro y el final de la franja', () => {
    const fixture = abrir();
    const c = fixture.componentInstance;
    c.dni = '73456783';
    c.idLibro = 3;
    c.fecha = '2026-10-30';
    c.franja = '14:30 - 17:30';

    c.reservarEnVentanilla();

    const req = http.expectOne(r => r.method === 'POST' && r.url === `${API}/reservas/ventanilla`);
    expect(req.request.body).toEqual({ dniPrestatario: '73456783', idLibro: 3, fechaRecojoLimite: '2026-10-30T17:30:00' });
    req.flush({ ...pendiente, idReserva: 9 });
    recargar();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Reserva #9 registrada');
  });

  it('sin stock no manda fecha: el prestatario entra a la cola de espera', () => {
    const fixture = abrir();
    const c = fixture.componentInstance;
    c.dni = '73456783';
    c.idLibro = 4; // Cosmos, stock 0

    c.reservarEnVentanilla();

    const req = http.expectOne(`${API}/reservas/ventanilla`);
    expect(req.request.body.fechaRecojoLimite).toBeNull();
    req.flush({ ...enCola, idReserva: 10, posicionCola: 2 });
    recargar();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('entró a la cola de espera (puesto 2)');
  });

  it('no manda la reserva si el DNI no tiene 8 dígitos', () => {
    const fixture = abrir();
    const c = fixture.componentInstance;
    c.dni = '123';
    c.idLibro = 3;

    c.reservarEnVentanilla();
    fixture.detectChanges();

    http.expectNone(r => r.method === 'POST');
    expect(fixture.nativeElement.textContent).toContain('Ingrese un DNI de 8 dígitos y seleccione un libro');
  });
});
