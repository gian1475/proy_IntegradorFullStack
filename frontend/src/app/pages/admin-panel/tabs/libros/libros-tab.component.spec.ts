import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LibrosTabComponent } from './libros-tab.component';

// D15: mientras llega la lista de libros se muestra "Cargando...", no "No hay libros registrados"
describe('pestaña Libros del admin', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [LibrosTabComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  it('muestra "Cargando..." mientras espera la lista', () => {
    const fixture = TestBed.createComponent(LibrosTabComponent);
    fixture.detectChanges();

    const texto: string = fixture.nativeElement.textContent;
    expect(texto).toContain('Cargando...');
    expect(texto).not.toContain('No hay libros registrados');
  });

  it('si la lista llega vacía, dice que no hay libros', () => {
    const fixture = TestBed.createComponent(LibrosTabComponent);
    fixture.detectChanges();

    http.expectOne('http://localhost:8080/api/libros').flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No hay libros registrados');
  });
});
