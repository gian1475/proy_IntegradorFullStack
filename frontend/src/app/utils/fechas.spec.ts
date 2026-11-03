import { fechaCorta, fechaInput, horaCorta, horaDe24, diaMes } from './fechas';

// L3: las fechas se arman con formatDate de @angular/common (antes, a mano con padStart y toLocale...)
describe('formato de fechas (formatDate de Angular)', () => {
  // Angular separa "p. m." con un espacio fino (\u202f): en pantalla se ve igual que un espacio normal
  const espacios = (texto: string) => texto.replace(/\s/g, ' ');
  const tarde = '2026-11-02T18:21:00';

  it('fecha para un input type="date" (AAAA-MM-DD)', () => {
    expect(fechaInput(new Date(2026, 0, 5))).toBe('2026-01-05');
  });

  it('hora corta como en Perú (12 horas)', () => {
    expect(espacios(horaCorta(tarde))).toBe('06:21 p. m.');
    expect(espacios(horaCorta('2026-11-02T09:05:00'))).toBe('09:05 a. m.');
  });

  it('fecha corta como en Perú', () => {
    expect(fechaCorta(tarde)).toBe('2/11/2026');
  });

  it('día/mes y hora de 24 horas para las sanciones', () => {
    expect(diaMes(tarde)).toBe('02/11');
    expect(horaDe24('2026-11-10T20:00:00')).toBe('20:00');
  });
});
