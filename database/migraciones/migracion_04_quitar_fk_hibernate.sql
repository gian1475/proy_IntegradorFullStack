-- =====================================================================
-- MIGRACIÓN 04: quitar las llaves foráneas que creó Hibernate
-- Antes del 21/10 Hibernate estaba en modo 'update' y creó solo 3 llaves foráneas extra
-- (con nombres como fk8s1j7k9hshgq64d77enp1xrh5) de prestamo y reserva hacia usuario.
-- Repiten lo que ya controlan las llaves del script, que son más estrictas:
--   prestamo.id_prestatario -> prestatario, prestamo.id_encargado -> encargado,
--   reserva.id_prestatario  -> prestatario.
-- No están en schema_postgresql.sql. Con esta migración Neon queda igual al script.
-- No toca ninguna fila. Se puede volver a ejecutar sin problema.
-- =====================================================================

DO $$
DECLARE
    r RECORD;
BEGIN
    -- busca las llaves de prestamo y reserva que apuntan a usuario (las del script apuntan a sus subtipos)
    FOR r IN
        SELECT conrelid::regclass AS tabla, conname
        FROM pg_constraint
        WHERE contype = 'f'
          AND conrelid IN ('prestamo'::regclass, 'reserva'::regclass)
          AND confrelid = 'usuario'::regclass
    LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', r.tabla, r.conname);
        RAISE NOTICE 'Llave quitada: %.%', r.tabla, r.conname;
    END LOOP;
END $$;
