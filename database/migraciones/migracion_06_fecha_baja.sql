-- =====================================================================
-- MIGRACIÓN 06: fecha de las eliminaciones lógicas (reporte de bajas lógicas, semana 16)
-- El profe pidió que el reporte de registros eliminados se filtre por periodo (día, semana, mes,
-- de tal a tal fecha) y que cuente cuántos se eliminaron. Para eso se guarda CUÁNDO se dio de baja:
--   libro.fecha_baja         -> al dar de baja un libro (estado 'baja')
--   usuario.fecha_baja       -> al dar de baja un usuario (estado 'inactivo'); vuelve a NULL al reactivarlo
--   prestamo.fecha_anulacion -> al anular un préstamo (estado 'anulado')
-- Las bajas anteriores a esta migración quedan con NULL: no se inventa la fecha.
--   (los préstamos anulados sin fecha usan la fecha del préstamo en el reporte)
-- No toca ninguna fila. Ejecutar en Neon (SQL Editor) ANTES de arrancar el backend nuevo:
-- con ddl-auto=validate, el backend no arranca si falta alguna columna.
-- Se puede volver a ejecutar sin problema.
-- =====================================================================

ALTER TABLE libro    ADD COLUMN IF NOT EXISTS fecha_baja      TIMESTAMP NULL;
ALTER TABLE usuario  ADD COLUMN IF NOT EXISTS fecha_baja      TIMESTAMP NULL;
ALTER TABLE prestamo ADD COLUMN IF NOT EXISTS fecha_anulacion TIMESTAMP NULL;

-- comprobación: deben salir las tres columnas
SELECT table_name, column_name, data_type FROM information_schema.columns
WHERE (table_name = 'libro' AND column_name = 'fecha_baja')
   OR (table_name = 'usuario' AND column_name = 'fecha_baja')
   OR (table_name = 'prestamo' AND column_name = 'fecha_anulacion');
