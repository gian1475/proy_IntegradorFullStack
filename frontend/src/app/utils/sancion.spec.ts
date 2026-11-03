import { incidenciasPosibles, textoSancion } from './sancion';

// RF06: qué incidencia corresponde a cada préstamo (las mismas reglas que IncidenciaService) y cómo se ve la sanción
describe('incidencias y sanciones (RF06)', () => {
  const ahora = new Date(2026, 10, 3, 10, 0); // 03/11/2026 10:00

  const prestamo = (estado: string, limite: string, devolucion: string | null = null) => ({
    estadoPrestamo: estado,
    fechaDevolucionPrevista: limite,
    fechaDevolucionReal: devolucion,
  });

  it('un préstamo en curso que pasó su hora límite tiene retraso y puede perderse', () => {
    expect(incidenciasPosibles(prestamo('vencido', '2026-11-03T09:00:00'), ahora)).toEqual(['retraso', 'pérdida']);
  });

  it('un préstamo en curso dentro de su hora solo puede perderse', () => {
    expect(incidenciasPosibles(prestamo('activo', '2026-11-03T12:00:00'), ahora)).toEqual(['pérdida']);
  });

  it('un préstamo devuelto tarde tiene retraso y daño; devuelto a tiempo, solo daño', () => {
    expect(incidenciasPosibles(prestamo('devuelto', '2026-11-02T18:00:00', '2026-11-02T18:40:00'), ahora))
      .toEqual(['retraso', 'daño']);
    expect(incidenciasPosibles(prestamo('devuelto', '2026-11-02T18:00:00', '2026-11-02T17:30:00'), ahora))
      .toEqual(['daño']);
  });

  it('no ofrece una incidencia que el préstamo ya tiene registrada', () => {
    const devueltoTarde = prestamo('devuelto', '2026-11-02T18:00:00', '2026-11-02T18:40:00');
    expect(incidenciasPosibles(devueltoTarde, ahora, ['retraso'])).toEqual(['daño']);
  });

  it('un préstamo anulado no tiene incidencias', () => {
    expect(incidenciasPosibles(prestamo('anulado', '2026-11-03T09:00:00'), ahora)).toEqual([]);
  });

  it('describe cada sanción para la tabla', () => {
    expect(textoSancion({ tipoSancion: 'advertencia', estadoSancion: 'cumplida', monto: null, finSancion: null }))
      .toBe('Advertencia');
    expect(textoSancion({ tipoSancion: 'suspensión', estadoSancion: 'activa', monto: null, finSancion: '2026-11-10T20:00:00' }))
      .toBe('Suspensión hasta el 10/11 20:00');
    expect(textoSancion({ tipoSancion: 'multa', estadoSancion: 'activa', monto: 25, finSancion: null }))
      .toBe('Multa de S/ 25.00 (pendiente de pago)');
    expect(textoSancion({ tipoSancion: 'multa', estadoSancion: 'cumplida', monto: 25, finSancion: '2026-11-04T11:00:00' }))
      .toBe('Multa de S/ 25.00 (pagada el 04/11)');
  });
});
