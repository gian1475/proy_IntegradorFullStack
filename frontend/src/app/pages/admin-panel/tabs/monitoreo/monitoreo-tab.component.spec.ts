import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MonitoreoTabComponent } from './monitoreo-tab.component';

// B3 (RNF03): "al menos el 95 % de las operaciones en 2 s o menos": el monitoreo muestra ese %
describe('pestaña Monitoreo del admin (RNF03)', () => {
  const base = { estado: 'UP', tiempoActivoSegundos: 3725, memoriaUsadaMB: 120, memoriaMaximaMB: 4000, cpuProcesoPorcentaje: 1,
                 hilosActivos: 30, peticionesHttp: 20, peticionesConError: 1, tiempoRespuestaPromedioMs: 80,
                 conexionesBdActivas: 0, conexionesBdTotales: 10, sesionesActivas: 1, prestamosActivos: 2,
                 prestamosVencidos: 0, reservasPendientes: 3 };
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [MonitoreoTabComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  function abrir(porcentaje: number | null) {
    const fixture = TestBed.createComponent(MonitoreoTabComponent);
    fixture.detectChanges();
    http.expectOne(r => r.url.endsWith('/monitoreo')).flush({ ...base, peticionesHasta2sPorcentaje: porcentaje });
    fixture.detectChanges();
    return fixture.nativeElement.textContent as string;
  }

  it('muestra el % de peticiones en 2 s o menos y si cumple la meta', () => {
    const texto = abrir(95);
    expect(texto).toContain('Peticiones en 2 s o menos');
    expect(texto).toContain('95 %');
    expect(texto).toContain('Cumple RNF03');
  });

  it('por debajo del 95 % no cumple', () => {
    expect(abrir(90)).toContain('No cumple RNF03');
  });

  it('sin peticiones todavía dice que no hay dato', () => {
    expect(abrir(null)).toContain('Sin dato');
  });
});
