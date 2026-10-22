-- =====================================================================
-- MIGRACIÓN 02: monto de las multas (RF06)
-- La sanción de tipo 'multa' necesita guardar cuánto se cobra.
-- Ejecutar UNA vez en Neon (SQL Editor). Se puede volver a ejecutar sin problema.
-- =====================================================================

ALTER TABLE sancion ADD COLUMN IF NOT EXISTS monto DECIMAL(8,2) NULL;

ALTER TABLE sancion DROP CONSTRAINT IF EXISTS chk_sancion_monto;
ALTER TABLE sancion ADD CONSTRAINT chk_sancion_monto
    CHECK (monto IS NULL OR monto >= 0);
