/** Misma regla que ValidadorContrasena del backend (RNF04). */

/** BCrypt solo procesa 72 bytes: las tildes y la ñ ocupan 2. */
export const MAX_BYTES_CONTRASENA = 72;

// regla contra (front): los mismos 5 criterios del backend, se van marcando mientras escribe.
// la usan el registro del prestatario y el registro de encargados del admin
export function requisitosContrasena(c: string): { texto: string; cumple: boolean }[] {
  const bytes = new TextEncoder().encode(c).length;
  return [
    { texto: 'Entre 8 y 64 caracteres (máximo 72 bytes)', cumple: c.length >= 8 && c.length <= 64 && bytes <= MAX_BYTES_CONTRASENA }, // criterio 1
    { texto: 'Una letra minúscula', cumple: /\p{Ll}/u.test(c) },                    // criterio 2
    { texto: 'Una letra mayúscula', cumple: /\p{Lu}/u.test(c) },                    // criterio 3
    { texto: 'Un número', cumple: /\d/.test(c) },                                   // criterio 4
    { texto: 'Un símbolo especial (ej. @ # $ % !)', cumple: /[^\p{L}\d\s]/u.test(c) }, // criterio 5
  ];
}

export function contrasenaValida(c: string): boolean {
  return requisitosContrasena(c).every(r => r.cumple);
}
