-- =====================================================================
-- MIGRACIÓN 03: cola de espera de reservas (RF04 y RF16)
-- Si un libro no tiene ejemplares, la reserva entra a la cola con el estado 'en_espera':
-- sin fecha de recojo y con su número en posicion_cola. Cuando se libera un ejemplar,
-- el primero de la cola pasa a 'pendiente' y recibe su fecha límite de recojo.
-- Ejecutar UNA vez en Neon (SQL Editor). Se puede volver a ejecutar sin problema.
-- =====================================================================

ALTER TABLE reserva DROP CONSTRAINT IF EXISTS chk_reserva_estado;
ALTER TABLE reserva ADD CONSTRAINT chk_reserva_estado
    CHECK (estado_reserva IN ('pendiente', 'en_espera', 'atendida', 'cancelada', 'vencida'));
