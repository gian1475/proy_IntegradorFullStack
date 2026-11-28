import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { LoginComponent } from './login.component';

// ver contraseña: el boton "Ver" cambia el campo de password a text y "Ocultar" lo regresa (pedido de los usuarios, 28/11)
describe('Login: ver la contraseña y autocompletar', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
  });

  function abrir() {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    return fixture;
  }

  function botonVer(el: HTMLElement): HTMLButtonElement {
    return el.querySelector('button[aria-controls="password"]') as HTMLButtonElement;
  }

  it('la contraseña empieza oculta y el botón dice "Ver"', () => {
    const el: HTMLElement = abrir().nativeElement;
    expect((el.querySelector('#password') as HTMLInputElement).type).toBe('password');
    expect(botonVer(el).textContent?.trim()).toBe('Ver');
  });

  it('"Ver" muestra la contraseña y "Ocultar" la vuelve a esconder', () => {
    const fixture = abrir();
    const el: HTMLElement = fixture.nativeElement;

    botonVer(el).click();
    fixture.detectChanges();
    expect((el.querySelector('#password') as HTMLInputElement).type).toBe('text');
    expect(botonVer(el).textContent?.trim()).toBe('Ocultar');

    botonVer(el).click();
    fixture.detectChanges();
    expect((el.querySelector('#password') as HTMLInputElement).type).toBe('password');
  });

  it('el botón "Ver" no envía el formulario (type="button")', () => {
    const el: HTMLElement = abrir().nativeElement;
    expect(botonVer(el).type).toBe('button');
  });

  it('el navegador puede recordar el correo y la contraseña (autocomplete)', () => {
    const el: HTMLElement = abrir().nativeElement;
    expect(el.querySelector('#username')?.getAttribute('autocomplete')).toBe('username');
    expect(el.querySelector('#password')?.getAttribute('autocomplete')).toBe('current-password');
  });
});
