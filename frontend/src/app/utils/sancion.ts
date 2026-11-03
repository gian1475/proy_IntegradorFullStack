/** RF06: reglas de incidencias y sanciones que usa la pestaña "Incidencias" del encargado. */

export type TipoIncidencia = 'retraso' | 'daño' | 'pérdida';

/** Lo que hace falta de un préstamo para saber qué incidencia le corresponde. */
export interface PrestamoParaIncidencia {
  estadoPrestamo: string;
  fechaDevolucionPrevista: string;
  fechaDevolucionReal: string | null;
}

// tipos de incidencia (front): las mismas reglas que IncidenciaService del backend, para mostrar solo las q corresponden.
// retraso -> paso su hora limite; daño -> ya se devolvio; perdida -> el libro sigue fuera.
// yaRegistradas: las q el prestamo ya tiene (el backend no deja repetir el mismo tipo)
export function incidenciasPosibles(p: PrestamoParaIncidencia, ahora: Date, yaRegistradas: string[] = []): TipoIncidencia[] {
  if (p.estadoPrestamo === 'anulado') return [];
  const enCurso = p.estadoPrestamo === 'activo' || p.estadoPrestamo === 'vencido';
  // la fecha del backend viene sin zona horaria: js la toma como hora local (Lima)
  const fin = p.fechaDevolucionReal ? new Date(p.fechaDevolucionReal) : ahora;
  const posibles: TipoIncidencia[] = [];
  if (fin.getTime() > new Date(p.fechaDevolucionPrevista).getTime()) posibles.push('retraso');
  if (!enCurso) posibles.push('daño');
  if (enCurso) posibles.push('pérdida');
  return posibles.filter(t => !yaRegistradas.includes(t));
}

/** Lo que hace falta de una sanción para describirla. */
export interface SancionResumen {
  tipoSancion: string | null;
  estadoSancion: string | null;
  monto: number | null;
  finSancion: string | null;
}

/** "Suspensión hasta el 10/11 20:00", "Multa de S/ 25.00 (pendiente de pago)", "Advertencia". */
export function textoSancion(s: SancionResumen): string {
  switch (s.tipoSancion) {
    case 'advertencia':
      return 'Advertencia';
    case 'suspensión':
      return s.finSancion ? `Suspensión hasta el ${diaMes(s.finSancion)} ${hora(s.finSancion)}` : 'Suspensión';
    case 'multa': {
      const monto = `Multa de S/ ${Number(s.monto ?? 0).toFixed(2)}`;
      if (s.estadoSancion === 'activa') return `${monto} (pendiente de pago)`;
      if (s.estadoSancion === 'cumplida' && s.finSancion) return `${monto} (pagada el ${diaMes(s.finSancion)})`;
      return monto;
    }
    default:
      return 'Sin sanción';
  }
}

function diaMes(fecha: string): string {
  const d = new Date(fecha);
  return `${dos(d.getDate())}/${dos(d.getMonth() + 1)}`;
}

function hora(fecha: string): string {
  const d = new Date(fecha);
  return `${dos(d.getHours())}:${dos(d.getMinutes())}`;
}

function dos(n: number): string {
  return String(n).padStart(2, '0');
}
