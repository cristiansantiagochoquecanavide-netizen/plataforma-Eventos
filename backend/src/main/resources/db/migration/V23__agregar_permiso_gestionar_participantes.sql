INSERT INTO permiso (nombre, descripcion)
VALUES ('PARTICIPANTE_VER', 'Permite consultar y gestionar participantes e inscripciones de eventos')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r, permiso p
WHERE r.nombre IN ('ADMINISTRADOR', 'ORGANIZADOR')
  AND p.nombre = 'PARTICIPANTE_VER'
ON CONFLICT DO NOTHING;
