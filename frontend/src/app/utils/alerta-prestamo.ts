/** RF05: se avisa cuando faltan 15 minutos o menos para la hora límite. */
export const MINUTOS_AVISO = 15;

export type TipoAlerta = 'a-tiempo' | 'por-vencer' | 'vencido';

export interface AlertaPrestamo {
  tipo: TipoAlerta;
  texto: string;
}

// alertas 15 min: dice si un prestamo esta a tiempo, por vencer (15 min o menos) o vencido, y cuanto falta.
// la fecha del backend viene sin zona horaria ("2026-10-28T12:30:00"), js la toma como hora local (Lima)
export function alertaPrestamo(horaLimite: string | Date, ahora: Date): AlertaPrestamo {
  const limite = typeof horaLimite === 'string' ? new Date(horaLimite) : horaLimite;
  const diferenciaMs = limite.getTime() - ahora.getTime();

  if (diferenciaMs <= 0) {
    const minutos = Math.max(1, Math.floor(-diferenciaMs / 60_000));
    return { tipo: 'vencido', texto: `Venció hace ${formatear(minutos)}` };
  }

  const minutos = Math.ceil(diferenciaMs / 60_000);
  return {
    tipo: minutos <= MINUTOS_AVISO ? 'por-vencer' : 'a-tiempo',
    texto: `Faltan ${formatear(minutos)}`,
  };
}

/** 80 -> "1 h 20 min"; 12 -> "12 min" */
function formatear(minutos: number): string {
  return minutos >= 60 ? `${Math.floor(minutos / 60)} h ${minutos % 60} min` : `${minutos} min`;
}
