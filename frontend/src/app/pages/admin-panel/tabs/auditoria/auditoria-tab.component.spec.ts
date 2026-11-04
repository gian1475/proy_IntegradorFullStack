import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuditoriaTabComponent } from './auditoria-tab.component';

// IPI (1.2.2): el admin registra el conteo físico de cada título y ve el índice del mes
describe('pestaña Auditoría del admin (IPI)', () => {
  const API = 'http://localhost:8080/api';
  const libros = [
    { idLibro: 16, titulo: 'Rayuela', isbn: '9789681902001', anioPublicacion: 1963, editorial: 'Sudamericana', stock: 3, estadoLibro: 'disponible', autores: [] },
    { idLibro: 17, titulo: 'Cosmos', isbn: '9780345539434', anioPublicacion: 1980, editorial: 'Random House', stock: 0, estadoLibro: 'mantenimiento', autores: [] },
  ];
  const auditoria = { idAuditoria: 1, idLibro: 16, tituloLibro: 'Rayuela', isbn: '9789681902001',
                      fechaAuditoria: '2026-11-03T10:00:00', stockSistema: 3, conteoFisico: 3, coincide: true, administrador: 'Admin Principal' };
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AuditoriaTabComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  function abrir(resumen: object) {
    const fixture = TestBed.createComponent(AuditoriaTabComponent);
    fixture.detectChanges();
    http.expectOne(`${API}/libros`).flush(libros);
    http.expectOne(`${API}/auditorias`).flush(resumen);
    fixture.detectChanges();
    return fixture;
  }

  it('muestra el IPI del mes y las auditorías', () => {
    const texto: string = abrir({ ipi: 95, auditados: 20, coinciden: 19, auditorias: [auditoria] }).nativeElement.textContent;
    expect(texto).toContain('95 %');
    expect(texto).toContain('19 de 20 títulos');
    expect(texto).toContain('Admin Principal');
  });

  it('sin auditorías dice que no hay dato', () => {
    const texto: string = abrir({ ipi: null, auditados: 0, coinciden: 0, auditorias: [] }).nativeElement.textContent;
    expect(texto).toContain('Sin dato');
  });

  it('registra el conteo físico con POST y vuelve a cargar el IPI', () => {
    const fixture = abrir({ ipi: null, auditados: 0, coinciden: 0, auditorias: [] });
    const c = fixture.componentInstance;

    c.conteos[16] = 3;
    c.registrar(libros[0]);

    const req = http.expectOne(r => r.method === 'POST' && r.url === `${API}/auditorias`);
    expect(req.request.body).toEqual({ idLibro: 16, conteoFisico: 3 });
    req.flush(auditoria);
    http.expectOne(`${API}/libros`).flush(libros);
    http.expectOne(`${API}/auditorias`).flush({ ipi: 100, auditados: 1, coinciden: 1, auditorias: [auditoria] });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Rayuela: coincide con el sistema');
  });

  it('no manda un conteo vacío o negativo', () => {
    const fixture = abrir({ ipi: null, auditados: 0, coinciden: 0, auditorias: [] });
    const c = fixture.componentInstance;

    c.conteos[16] = -1;
    c.registrar(libros[0]);
    c.conteos[17] = null;
    c.registrar(libros[1]);
    fixture.detectChanges();

    http.expectNone(r => r.method === 'POST');
    expect(fixture.nativeElement.textContent).toContain('Escriba cuántos ejemplares contó (0 o más)');
  });
});
