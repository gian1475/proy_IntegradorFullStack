import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { guardarArchivo } from './descarga';
import { PrestamoService } from '../services/prestamo.service';
import { ReservaService } from '../services/reserva.service';
import { ReporteService } from '../services/reporte.service';

// semana 15: el comprobante del préstamo y la constancia de la reserva se descargan como PDF.
// jsdom (el navegador de las pruebas) no crea enlaces de archivos: se reemplazan createObjectURL y el click
describe('descarga de archivos (comprobante y constancia en PDF)', () => {
  const API = 'http://localhost:8080/api';
  let http: HttpTestingController;
  let click: ReturnType<typeof vi.spyOn>;
  let nombres: string[];

  beforeEach(() => {
    nombres = [];
    URL.createObjectURL = vi.fn(() => 'blob:prueba');
    URL.revokeObjectURL = vi.fn();
    click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
      nombres.push(this.download);
    });
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => click.mockRestore());

  it('guardarArchivo descarga el archivo con su nombre y libera el enlace temporal', () => {
    guardarArchivo(new Blob(['%PDF']), 'comprobante_prestamo_12.pdf');

    expect(nombres).toEqual(['comprobante_prestamo_12.pdf']);
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:prueba');
  });

  it('el comprobante del préstamo se pide como archivo (blob) y se guarda con el N° del préstamo', () => {
    TestBed.inject(PrestamoService).descargarComprobante(12).subscribe();

    const req = http.expectOne(`${API}/prestamos/12/comprobante`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['%PDF']));

    expect(nombres).toEqual(['comprobante_prestamo_12.pdf']);
  });

  // D32: el nombre del reporte lleva la fecha LOCAL. antes usaba toISOString() (hora UTC): a las 23:30 de Lima
  // ya es el dia siguiente en UTC y el archivo salia con la fecha de mañana
  it('el reporte descargado de noche lleva la fecha de hoy, no la de mañana', () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 10, 24, 23, 30)); // 24/11/2026 23:30, hora de la PC
    TestBed.inject(ReporteService).descargar('stock', 'excel', '', '').subscribe();
    http.expectOne(r => r.url === `${API}/reportes/stock/excel`).flush(new Blob(['x']));
    vi.useRealTimers();

    expect(nombres).toEqual(['reporte_stock_2026-11-24.xlsx']);
  });

  it('la constancia de la reserva se pide como archivo (blob) y se guarda con el N° de la reserva', () => {
    TestBed.inject(ReservaService).descargarConstancia(5).subscribe();

    const req = http.expectOne(`${API}/reservas/5/constancia`);
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['%PDF']));

    expect(nombres).toEqual(['constancia_reserva_5.pdf']);
  });
});
