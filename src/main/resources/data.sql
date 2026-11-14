-- Datos de prueba locales (solo corre con SEED_MODE=always). Idempotente.
-- Edificios 1-2, unidades 1-5; id_user = IDs de usuario de IAM.

INSERT INTO building (id_building, address, city, district, name) VALUES
  (1, 'Av. Los Olivos 123', 'Lima', 'San Isidro', 'Residencial Los Olivos'),
  (2, 'Av. Larco 456',      'Lima', 'Miraflores', 'Torre Miraflores')
ON CONFLICT DO NOTHING;

INSERT INTO unit (id_unit, covered_area, distribution_percentage, floor, participation_percentage, status, total_area, unit_number, id_building) VALUES
  (1,  80.0, 30.0, 1, 30.0, 'OCCUPIED',   90.0, 101, 1),
  (2,  75.0, 30.0, 1, 30.0, 'OCCUPIED',   85.0, 102, 1),
  (3,  95.0, 40.0, 2, 40.0, 'OCCUPIED',  105.0, 201, 1),
  (4, 110.0, 50.0, 3, 50.0, 'OCCUPIED',  120.0, 301, 2),
  (5, 110.0, 50.0, 3, 50.0, 'AVAILABLE', 120.0, 302, 2)
ON CONFLICT DO NOTHING;

INSERT INTO user_unit (id_user_unit, end_date, failure_reason, id_user, relation_type, resident_email, start_date, status, id_unit) VALUES
  (1, NULL, NULL, 2, 'OWNER',  'propietario@edifika.com', '2025-01-01 00:00:00', 'ACTIVE', 1),
  (2, NULL, NULL, 3, 'OWNER',  'ana.perez@edifika.com',   '2025-03-01 00:00:00', 'ACTIVE', 2),
  (3, NULL, NULL, 4, 'TENANT', 'luis.gomez@edifika.com',  '2026-02-01 00:00:00', 'ACTIVE', 3),
  (4, NULL, NULL, 5, 'OWNER',  'maria.rojas@edifika.com', '2024-06-01 00:00:00', 'ACTIVE', 4)
ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('building', 'id_building'), (SELECT MAX(id_building) FROM building));
SELECT setval(pg_get_serial_sequence('unit', 'id_unit'), (SELECT MAX(id_unit) FROM unit));
SELECT setval(pg_get_serial_sequence('user_unit', 'id_user_unit'), (SELECT MAX(id_user_unit) FROM user_unit));
