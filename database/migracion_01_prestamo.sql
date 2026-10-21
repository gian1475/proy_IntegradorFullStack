-- =====================================================================
-- MIGRACIÓN 01: préstamos en sala y eliminación lógica
-- Ejecutar UNA vez en Neon (SQL Editor) ANTES de levantar el backend.
-- Se puede volver a ejecutar sin problema (usa IF EXISTS / IF NOT EXISTS).
-- =====================================================================

-- PRESTAMO: ampliación (una sola vez) y estado físico en la devolución
ALTER TABLE prestamo ADD COLUMN IF NOT EXISTS ampliado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE prestamo ADD COLUMN IF NOT EXISTS estado_fisico_devolucion VARCHAR(20) NULL;

ALTER TABLE prestamo DROP CONSTRAINT IF EXISTS chk_prestamo_estado_fisico;
ALTER TABLE prestamo ADD CONSTRAINT chk_prestamo_estado_fisico
    CHECK (estado_fisico_devolucion IS NULL OR estado_fisico_devolucion IN ('bueno', 'dañado'));

-- PRESTAMO: estado 'anulado' para la eliminación lógica
ALTER TABLE prestamo DROP CONSTRAINT IF EXISTS chk_prestamo_estado;
ALTER TABLE prestamo ADD CONSTRAINT chk_prestamo_estado
    CHECK (estado_prestamo IN ('activo', 'devuelto', 'vencido', 'anulado'));

-- LIBRO: estado 'baja' para la eliminación lógica
ALTER TABLE libro DROP CONSTRAINT IF EXISTS chk_libro_estado;
ALTER TABLE libro ADD CONSTRAINT chk_libro_estado
    CHECK (estado_libro IN ('disponible', 'prestado', 'reservado', 'mantenimiento', 'baja'));
