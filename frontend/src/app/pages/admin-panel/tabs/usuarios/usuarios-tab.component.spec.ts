import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { UsuariosTabComponent } from './usuarios-tab.component';

// RF12 (editar los datos de un usuario) y RF13 (darlo de baja: estado 'inactivo')
describe('pestaña Usuarios del admin (RF12 y RF13)', () => {
  const API = 'http://localhost:8080/api/usuarios';
  const valeria = { idUsuario: 5, nombre: 'Valeria', apellido: 'Flores', dni: '73456783',
                    correo: 'valeria@gmail.com', estado: 'activo', perfil: 'PRESTATARIO', turno: null };
  const maria = { idUsuario: 7, nombre: 'María', apellido: 'López', dni: '44444444',
                  correo: 'maria@alejandria.edu.pe', estado: 'inactivo', perfil: 'ENCARGADO', turno: 'tarde' };
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [UsuariosTabComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => vi.restoreAllMocks());

  function abrir() {
    const fixture = TestBed.createComponent(UsuariosTabComponent);
    fixture.detectChanges();
    http.expectOne(API).flush([valeria, maria]);
    fixture.detectChanges();
    return fixture;
  }

  function boton(fixture: ReturnType<typeof abrir>, fila: number, texto: string): HTMLButtonElement {
    const filas: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('tbody tr'));
    const b = Array.from(filas[fila].querySelectorAll('button')).find(x => x.textContent?.trim() === texto);
    if (!b) throw new Error(`No hay botón "${texto}" en la fila ${fila}`);
    return b as HTMLButtonElement;
  }

  it('lista a prestatarios y encargados con su perfil y estado', () => {
    const texto: string = abrir().nativeElement.textContent;
    expect(texto).toContain('Valeria Flores');
    expect(texto).toContain('Prestatario');
    expect(texto).toContain('Encargado');
    expect(texto).toContain('inactivo');
  });

  it('el activo tiene "Dar de baja" y el inactivo "Reactivar"', () => {
    const fixture = abrir();
    expect(boton(fixture, 0, 'Dar de baja')).toBeTruthy();
    expect(boton(fixture, 1, 'Reactivar')).toBeTruthy();
  });

  it('dar de baja pide confirmación y manda DELETE (la fila no se borra, queda inactivo)', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const fixture = abrir();

    boton(fixture, 0, 'Dar de baja').click();
    http.expectOne(r => r.method === 'DELETE' && r.url === `${API}/5`).flush({ ...valeria, estado: 'inactivo' });
    http.expectOne(API).flush([{ ...valeria, estado: 'inactivo' }, maria]);
    fixture.detectChanges();

    expect(window.confirm).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Se dio de baja a Valeria Flores'); // D9: sin género
  });

  it('si no confirma, no se da de baja', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    const fixture = abrir();

    boton(fixture, 0, 'Dar de baja').click();

    http.expectNone(r => r.method === 'DELETE');
  });

  it('muestra el mensaje del backend si no se puede dar de baja', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const fixture = abrir();

    boton(fixture, 0, 'Dar de baja').click();
    http.expectOne(`${API}/5`).flush(
      { mensaje: 'El prestatario tiene un libro prestado. Registre la devolución antes de darlo de baja' },
      { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('El prestatario tiene un libro prestado');
  });

  it('editar carga los datos y guarda con PUT', () => {
    const fixture = abrir();
    boton(fixture, 1, 'Editar').click();
    fixture.detectChanges();
    const componente = fixture.componentInstance;
    expect(componente.edicion?.turno).toBe('tarde');

    componente.edicion!.dni = '44444445';
    componente.guardarEdicion();

    const req = http.expectOne(r => r.method === 'PUT' && r.url === `${API}/7`);
    expect(req.request.body).toEqual({ nombre: 'María', apellido: 'López', dni: '44444445',
                                       correo: 'maria@alejandria.edu.pe', turno: 'tarde' });
    req.flush({ ...maria, dni: '44444445' });
    http.expectOne(API).flush([valeria, { ...maria, dni: '44444445' }]);
    fixture.detectChanges();
    expect(componente.edicion).toBeNull();
  });

  it('no manda la edición si el DNI no tiene 8 dígitos', () => {
    const fixture = abrir();
    boton(fixture, 0, 'Editar').click();
    const componente = fixture.componentInstance;

    componente.edicion!.dni = '123';
    componente.guardarEdicion();
    fixture.detectChanges();

    http.expectNone(r => r.method === 'PUT');
    expect(fixture.nativeElement.textContent).toContain('El DNI debe tener 8 dígitos');
  });

  it('reactivar manda PUT a /reactivar', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const fixture = abrir();

    boton(fixture, 1, 'Reactivar').click();

    http.expectOne(r => r.method === 'PUT' && r.url === `${API}/7/reactivar`).flush({ ...maria, estado: 'activo' });
    http.expectOne(API).flush([valeria, { ...maria, estado: 'activo' }]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Se reactivó a María López');
  });

  it('al pulsar Editar lleva el cursor al formulario (D10)', async () => {
    const fixture = abrir();
    document.body.appendChild(fixture.nativeElement); // focus solo funciona si esta en la pagina

    boton(fixture, 1, 'Editar').click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(document.activeElement?.id).toBe('u-nombre');
    fixture.nativeElement.remove();
  });
});
