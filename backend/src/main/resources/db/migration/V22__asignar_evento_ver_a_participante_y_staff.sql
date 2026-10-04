-- Asignar permiso EVENTO_VER a PARTICIPANTE y STAFF para permitir consulta del catalogo de eventos (CU7, CU11)
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
CROSS JOIN permiso p
WHERE r.nombre IN ('PARTICIPANTE', 'STAFF', 'VOLUNTARIO')
  AND p.nombre = 'EVENTO_VER'
ON CONFLICT DO NOTHING;
