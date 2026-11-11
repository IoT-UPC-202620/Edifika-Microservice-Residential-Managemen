-- US01 E2: un titular (OWNER) ACTIVE por unidad, como máximo.
-- `ddl-auto=update` no crea índices parciales; ejecutar a mano en cada entorno
-- (documentado en el ticket de verificación US01).
--
-- Predicado con `relation_type = 'OWNER' OR relation_type IS NULL` porque la app
-- (UserUnit#getRelationType) trata las filas legacy NULL como OWNER; indexar solo
-- 'OWNER' dejaría un hueco para dos titulares ACTIVE legacy en la misma unidad.
-- Filas TENANT/POSSESSOR no cuentan como titular y no bloquean.
CREATE UNIQUE INDEX IF NOT EXISTS ux_user_unit_titular_activo
    ON user_unit (id_unit)
    WHERE status = 'ACTIVE'
      AND end_date IS NULL
      AND (relation_type = 'OWNER' OR relation_type IS NULL);
