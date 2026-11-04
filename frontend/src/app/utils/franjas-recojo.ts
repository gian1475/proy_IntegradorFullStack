/**
 * RF04 y RF15: franjas de recojo de una reserva. La reserva guarda hasta cuándo se puede recoger el libro,
 * que es el final de la franja elegida (por ejemplo, "14:30 - 17:30" -> 17:30).
 * Las usan el formulario de reservar y "Cambiar fecha" (prestatario y admin).
 */
export const FRANJAS_RECOJO = [
  { valor: '08:30 - 11:30', texto: '08:30 - 11:30 (Turno Mañana)' },
  { valor: '11:30 - 14:30', texto: '11:30 - 14:30 (Turno Mediodía)' },
  { valor: '14:30 - 17:30', texto: '14:30 - 17:30 (Turno Tarde)' },
  { valor: '17:30 - 20:00', texto: '17:30 - 20:00 (Turno Noche)' },
];

/** "2026-10-30" + "14:30 - 17:30" -> "2026-10-30T17:30:00" (lo que recibe el backend). */
export function limiteDeRecojo(fecha: string, franja: string): string {
  const horaFin = franja.split(' - ')[1];
  return `${fecha}T${horaFin}:00`;
}

/** "2026-10-30T17:30:00" -> "2026-10-30" (para el input type="date"). */
export function fechaDelLimite(limite: string): string {
  return limite.slice(0, 10);
}

/** "2026-10-30T17:30:00" -> "14:30 - 17:30". Si no coincide con ninguna, la primera. */
export function franjaDelLimite(limite: string): string {
  const hora = limite.slice(11, 16);
  return (FRANJAS_RECOJO.find(f => f.valor.endsWith(hora)) ?? FRANJAS_RECOJO[0]).valor;
}
