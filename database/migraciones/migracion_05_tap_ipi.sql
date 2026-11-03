-- =====================================================================
-- MIGRACIÓN 05: datos para los indicadores TAP e IPI (apartado 1.2.2 del informe)
-- TAP (tiempo promedio de atención por préstamo): prestamo.inicio_atencion guarda cuándo empezó
--   el encargado a registrar el préstamo; fecha_prestamo es la confirmación.
--   TAP = promedio de (fecha_prestamo - inicio_atencion). Los préstamos sin ese dato no cuentan.
-- IPI (índice de precisión del inventario): tabla nueva auditoria_inventario con el conteo físico
--   que registra el administrador y el stock que tenía el sistema en ese momento.
--   IPI = títulos cuyo último conteo coincide / títulos auditados x 100.
-- No toca ninguna fila. Ejecutar en Neon (SQL Editor) ANTES de arrancar el backend nuevo:
-- con ddl-auto=validate, el backend no arranca si falta la columna o la tabla.
-- Se puede volver a ejecutar sin problema.
-- =====================================================================

-- TAP
ALTER TABLE prestamo ADD COLUMN IF NOT EXISTS inicio_atencion TIMESTAMP NULL;
ALTER TABLE prestamo DROP CONSTRAINT IF EXISTS chk_prestamo_inicio_atencion;
ALTER TABLE prestamo ADD CONSTRAINT chk_prestamo_inicio_atencion
    CHECK (inicio_atencion IS NULL OR inicio_atencion <= fecha_prestamo);

-- IPI
CREATE TABLE IF NOT EXISTS auditoria_inventario (
    id_auditoria        SERIAL PRIMARY KEY,
    id_libro            INTEGER NOT NULL,
    id_administrador    INTEGER NOT NULL,
    fecha_auditoria     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    stock_sistema       INTEGER NOT NULL,
    conteo_fisico       INTEGER NOT NULL,
    CONSTRAINT fk_auditoria_libro
        FOREIGN KEY (id_libro) REFERENCES libro(id_libro)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_auditoria_administrador
        FOREIGN KEY (id_administrador) REFERENCES administrador(id_usuario)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_auditoria_stock CHECK (stock_sistema >= 0),
    CONSTRAINT chk_auditoria_conteo CHECK (conteo_fisico >= 0)
);

CREATE INDEX IF NOT EXISTS idx_auditoria_fecha ON auditoria_inventario (fecha_auditoria);

-- comprobación: debe mostrar la columna y la tabla
SELECT column_name, data_type FROM information_schema.columns
WHERE table_name = 'prestamo' AND column_name = 'inicio_atencion';
SELECT COUNT(*) AS auditorias FROM auditoria_inventario;
