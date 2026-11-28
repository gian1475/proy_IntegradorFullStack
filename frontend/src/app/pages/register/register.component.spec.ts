import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { RegisterComponent } from './register.component';

// ver contraseña: un solo boton muestra la contraseña y su confirmacion, para compararlas (28/11)
describe('Registro: ver la contraseña, teclado numérico y autocompletar', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
  });

  function abrir() {
    const fixture = TestBed.createComponent(RegisterComponent);
    fixture.detectChanges();
    return fixture;
  }

  const tipo = (el: HTMLElement, id: string) => (el.querySelector(`#${id}`) as HTMLInputElement).type;
  const botonVer = (el: HTMLElement) => el.querySelector('button[aria-controls="contrasena confirmar"]') as HTMLButtonElement;

  it('las dos contraseñas empiezan ocultas', () => {
    const el: HTMLElement = abrir().nativeElement;
    expect(tipo(el, 'contrasena')).toBe('password');
    expect(tipo(el, 'confirmar')).toBe('password');
    expect(botonVer(el).textContent?.trim()).toBe('Ver');
  });

  it('"Ver" muestra la contraseña y la confirmación a la vez; "Ocultar" las esconde', () => {
    const fixture = abrir();
    const el: HTMLElement = fixture.nativeElement;

    botonVer(el).click();
    fixture.detectChanges();
    expect(tipo(el, 'contrasena')).toBe('text');
    expect(tipo(el, 'confirmar')).toBe('text');
    expect(botonVer(el).textContent?.trim()).toBe('Ocultar');

    botonVer(el).click();
    fixture.detectChanges();
    expect(tipo(el, 'contrasena')).toBe('password');
    expect(tipo(el, 'confirmar')).toBe('password');
  });

  it('el botón "Ver" no envía el formulario (type="button")', () => {
    expect(botonVer(abrir().nativeElement).type).toBe('button');
  });

  it('el DNI abre el teclado numérico en el celular', () => {
    const el: HTMLElement = abrir().nativeElement;
    expect(el.querySelector('#dni')?.getAttribute('inputmode')).toBe('numeric');
  });

  it('el navegador sabe qué es cada campo (autocomplete) y ofrece guardar la contraseña nueva', () => {
    const el: HTMLElement = abrir().nativeElement;
    expect(el.querySelector('#nombre')?.getAttribute('autocomplete')).toBe('given-name');
    expect(el.querySelector('#apellido')?.getAttribute('autocomplete')).toBe('family-name');
    expect(el.querySelector('#correo')?.getAttribute('autocomplete')).toBe('email');
    expect(el.querySelector('#contrasena')?.getAttribute('autocomplete')).toBe('new-password');
    expect(el.querySelector('#confirmar')?.getAttribute('autocomplete')).toBe('new-password');
  });
});
