import { alertaPrestamo, MINUTOS_AVISO } from './alerta-prestamo';

// RF05: alertas en el panel del encargado cuando faltan 15 minutos o cuando el préstamo ya venció
describe('alertaPrestamo (RF05)', () => {
  const ahora = new Date(2026, 9, 28, 10, 0, 0); // 28/10/2026 10:00 hora local

  const enMinutos = (min: number) => new Date(ahora.getTime() + min * 60_000);

  it('el aviso es a los 15 minutos', () => {
    expect(MINUTOS_AVISO).toBe(15);
  });

  it('con 2 horas por delante está a tiempo', () => {
    const a = alertaPrestamo(enMinutos(120), ahora);
    expect(a.tipo).toBe('a-tiempo');
    expect(a.texto).toBe('Faltan 2 h 0 min');
  });

  it('con 16 minutos todavía está a tiempo', () => {
    expect(alertaPrestamo(enMinutos(16), ahora).tipo).toBe('a-tiempo');
  });

  it('con 15 minutos o menos está por vencer', () => {
    const a = alertaPrestamo(enMinutos(15), ahora);
    expect(a.tipo).toBe('por-vencer');
    expect(a.texto).toBe('Faltan 15 min');
    expect(alertaPrestamo(enMinutos(1), ahora).tipo).toBe('por-vencer');
  });

  it('si ya pasó la hora límite está vencido', () => {
    const a = alertaPrestamo(enMinutos(-5), ahora);
    expect(a.tipo).toBe('vencido');
    expect(a.texto).toBe('Venció hace 5 min');
  });

  it('un préstamo que acaba de vencer también está vencido', () => {
    const a = alertaPrestamo(new Date(ahora.getTime() - 10_000), ahora);
    expect(a.tipo).toBe('vencido');
    expect(a.texto).toBe('Venció hace 1 min');
  });

  it('entiende la fecha tal como la manda el backend (sin zona horaria = hora local)', () => {
    const a = alertaPrestamo('2026-10-28T10:10:00', ahora);
    expect(a.tipo).toBe('por-vencer');
    expect(a.texto).toBe('Faltan 10 min');
  });
});
