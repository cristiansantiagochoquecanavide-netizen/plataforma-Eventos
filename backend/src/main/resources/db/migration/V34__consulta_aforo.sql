INSERT INTO permiso (nombre, descripcion)
VALUES ('ASISTENCIA_CONSULTAR', 'Consultar y exportar asistencia y monitor de aforo.')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso FROM rol r CROSS JOIN permiso p
WHERE r.nombre IN ('ADMINISTRADOR', 'ORGANIZADOR') AND p.nombre = 'ASISTENCIA_CONSULTAR'
ON CONFLICT DO NOTHING;

CREATE INDEX idx_inscripcion_tenant_evento_estado ON inscripcion(id_organizacion, id_evento, estado);

ALTER TABLE inscripcion ADD COLUMN categoria_entrada VARCHAR(80);
