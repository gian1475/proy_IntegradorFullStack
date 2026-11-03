import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReservasAdminTabComponent } from './reservas-admin-tab.component';

// RF15: el admin cambia la fecha y la franja de recojo de una reserva pendiente
describe('pestaña Reservas del admin (RF15 y RF17)', () => {
  const API = 'http://localhost:8080/api/reservas';
  const base = { idLibro: 3, tituloLibro: 'Rayuela', isbnLibro: '1', autor: 'Julio Cortázar', categoria: 'Literatura',
                 idPrestatario: 5, nombrePrestatario: 'Valeria Flores', fechaReserva: '2026-10-28T09:00:00', posicionCola: 1 };
  const pendiente = { ...base, idReserva: 7, fechaRecojoLimite: '2026-10-29T11:30:00', estadoReserva: 'pendiente' };
  const cancelada = { ...base, idReserva: 8, fechaRecojoLimite: '2026-10-29T11:30:00', estadoReserva: 'cancelada' };
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ReservasAdminTabComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  function abrir() {
    const fixture = TestBed.createComponent(ReservasAdminTabComponent);
    fixture.detectChanges();
    http.expectOne(API).flush([pendiente, cancelada]);
    fixture.detectChanges();
    return fixture;
  }

  function botones(fixture: ReturnType<typeof abrir>, fila: number): string[] {
    const tr = fixture.nativeElement.querySelectorAll('tbody tr')[fila] as HTMLElement;
    return Array.from(tr.querySelectorAll('button')).map(b => b.textContent!.trim());
  }

  it('solo la pendiente tiene "Cambiar fecha"; solo la cancelada, "Eliminar"', () => {
    const fixture = abrir();
    expect(botones(fixture, 0)).toEqual(['Cambiar fecha']);
    expect(botones(fixture, 1)).toEqual(['Eliminar']);
  });

  it('cambiar fecha carga la fecha y la franja actuales y guarda con PUT', () => {
    const fixture = abrir();
    const c = fixture.componentInstance;

    c.editarFecha(pendiente);
    expect(c.edicion()).toEqual({ idReserva: 7, fecha: '2026-10-29', franja: '08:30 - 11:30' });

    c.edicion.set({ idReserva: 7, fecha: '2026-10-30', franja: '14:30 - 17:30' });
    c.guardarFecha();

    const req = http.expectOne(r => r.method === 'PUT' && r.url === `${API}/7`);
    expect(req.request.body).toEqual({ fechaRecojoLimite: '2026-10-30T17:30:00' });
    req.flush({ ...pendiente, fechaRecojoLimite: '2026-10-30T17:30:00' });
    http.expectOne(API).flush([{ ...pendiente, fechaRecojoLimite: '2026-10-30T17:30:00' }, cancelada]);
    fixture.detectChanges();

    expect(c.edicion()).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Reserva #7: nueva fecha de recojo');
  });

  it('muestra el mensaje del backend si no se puede cambiar', () => {
    const fixture = abrir();
    const c = fixture.componentInstance;
    c.editarFecha(pendiente);

    c.guardarFecha();
    http.expectOne(`${API}/7`).flush({ mensaje: 'Solo se pueden modificar reservas en estado pendiente' },
                                     { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Solo se pueden modificar reservas en estado pendiente');
  });
});
