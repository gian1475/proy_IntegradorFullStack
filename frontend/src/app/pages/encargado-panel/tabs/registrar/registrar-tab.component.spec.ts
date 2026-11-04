import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RegistrarTabComponent } from './registrar-tab.component';

// TAP (1.2.2): se mide desde que el encargado empieza a llenar el préstamo hasta que lo confirma
describe('Registrar préstamo: tiempo de atención (TAP)', () => {
  const API = 'http://localhost:8080/api';
  const prestamo = { idPrestamo: 50, fechaPrestamo: '2026-11-03T10:01:15', fechaDevolucionPrevista: '2026-11-03T13:01:15' };
  let http: HttpTestingController;

  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-11-03T15:00:00Z'));
    TestBed.configureTestingModule({
      imports: [RegistrarTabComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => vi.useRealTimers());

  function abrir() {
    const fixture = TestBed.createComponent(RegistrarTabComponent);
    fixture.detectChanges();
    http.expectOne(`${API}/libros`).flush([]);
    return fixture;
  }

  function escribirDni(fixture: ReturnType<typeof abrir>) {
    const input: HTMLInputElement = fixture.nativeElement.querySelector('#dni');
    input.value = '33333333';
    input.dispatchEvent(new Event('input', { bubbles: true })); // el <form> escucha el (input): empieza la atención
    fixture.componentInstance.dniPrestatario = '33333333';      // ngModel lo copia recién en el siguiente ciclo
    fixture.detectChanges();
  }

  it('manda los segundos desde que empezó a escribir hasta que confirma', () => {
    const fixture = abrir();
    const c = fixture.componentInstance;

    escribirDni(fixture);                 // empieza la atención
    vi.advanceTimersByTime(75 * 1000);    // 1 min 15 s eligiendo el libro
    c.idLibroSeleccionado = 10;
    c.registrarPrestamo();

    const req = http.expectOne(r => r.method === 'POST' && r.url === `${API}/prestamos`);
    expect(req.request.body).toEqual({ dniPrestatario: '33333333', idLibro: 10, segundosAtencion: 75 });
    req.flush(prestamo);
    http.expectOne(`${API}/libros`).flush([]);
  });

  it('después de registrar, el siguiente préstamo vuelve a contar desde cero', () => {
    const fixture = abrir();
    const c = fixture.componentInstance;
    escribirDni(fixture);
    vi.advanceTimersByTime(60 * 1000);
    c.idLibroSeleccionado = 10;
    c.registrarPrestamo();
    http.expectOne(`${API}/prestamos`).flush(prestamo);
    http.expectOne(`${API}/libros`).flush([]);

    vi.advanceTimersByTime(10 * 60 * 1000); // 10 min sin nadie en la ventanilla: no cuenta
    escribirDni(fixture);
    vi.advanceTimersByTime(30 * 1000);
    c.idLibroSeleccionado = 10;
    c.registrarPrestamo();

    expect(http.expectOne(`${API}/prestamos`).request.body.segundosAtencion).toBe(30);
  });

  it('si viene de "Entregar" una reserva, cuenta desde que se abre el formulario', () => {
    TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(RegistrarTabComponent);
    fixture.componentRef.setInput('reserva', { idReserva: 7, idLibro: 10, tituloLibro: 'Rayuela', nombrePrestatario: 'Valeria Flores' });
    fixture.detectChanges();
    http.expectOne(`${API}/libros`).flush([]);
    const c = fixture.componentInstance;

    vi.advanceTimersByTime(40 * 1000);
    c.dniPrestatario = '73456783';
    c.registrarPrestamo();

    expect(http.expectOne(`${API}/prestamos`).request.body.segundosAtencion).toBe(40);
  });
});
