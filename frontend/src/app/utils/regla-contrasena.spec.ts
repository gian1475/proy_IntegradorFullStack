import { contrasenaValida, requisitosContrasena } from './regla-contrasena';

// RNF04: la misma regla del backend (ValidadorContrasena), también en bytes
describe('regla de contraseña (front)', () => {
  it('acepta una contraseña que cumple los 5 criterios', () => {
    expect(contrasenaValida('Biblioteca#2026')).toBe(true);
    expect(contrasenaValida('Ñandú#2026')).toBe(true);
  });

  it('marca cada criterio que falta', () => {
    const sinSimbolo = requisitosContrasena('Abcdefg12');
    expect(sinSimbolo[4].cumple).toBe(false);
    expect(contrasenaValida('123456')).toBe(false);
  });

  it('rechaza 64 caracteres que pasan los 72 bytes', () => {
    expect(contrasenaValida('Aa1!' + 'ñ'.repeat(60))).toBe(false); // 124 bytes
    expect(contrasenaValida('Aa1!' + 'ñ'.repeat(34))).toBe(true);  // 72 bytes justos
  });
});
