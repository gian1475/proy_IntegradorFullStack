import { FRANJAS_RECOJO, fechaDelLimite, franjaDelLimite, limiteDeRecojo } from './franjas-recojo';

// RF04 y RF15: la reserva guarda hasta cuándo se puede recoger = el final de la franja elegida
describe('franjas de recojo de una reserva (RF04 y RF15)', () => {
  it('son las 4 franjas de 3 horas, sin pasar de las 20:00', () => {
    expect(FRANJAS_RECOJO.map(f => f.valor)).toEqual(['08:30 - 11:30', '11:30 - 14:30', '14:30 - 17:30', '17:30 - 20:00']);
  });

  it('el límite de recojo es el final de la franja', () => {
    expect(limiteDeRecojo('2026-10-30', '14:30 - 17:30')).toBe('2026-10-30T17:30:00');
  });

  it('al editar, recupera la fecha y la franja de una reserva', () => {
    expect(fechaDelLimite('2026-10-30T17:30:00')).toBe('2026-10-30');
    expect(franjaDelLimite('2026-10-30T17:30:00')).toBe('14:30 - 17:30');
    expect(franjaDelLimite('2026-10-30T20:00:00')).toBe('17:30 - 20:00');
  });

  it('si la hora no es el final de una franja, propone la primera', () => {
    expect(franjaDelLimite('2026-10-30T16:00:00')).toBe('08:30 - 11:30');
  });
});
