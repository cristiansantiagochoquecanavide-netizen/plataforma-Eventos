-- V23: Permisos para Gestión de Participantes (CU10)
INSERT INTO permiso (nombre, descripcion) VALUES
    ('PARTICIPANTE_VER', 'Consultar el padrón de participantes y sus historiales de inscripciones.'),
    ('PARTICIPANTE_GESTIONAR', 'Administrar el estado y la pertenencia de los participantes en la organización.')
ON CONFLICT (nombre) DO NOTHING;

-- Asignar permisos a ADMINISTRADOR y ORGANIZADOR dentro de la organización
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
CROSS JOIN permiso p
WHERE r.nombre IN ('ADMINISTRADOR', 'ORGANIZADOR')
  AND p.nombre IN ('PARTICIPANTE_VER', 'PARTICIPANTE_GESTIONAR')
ON CONFLICT DO NOTHING;
