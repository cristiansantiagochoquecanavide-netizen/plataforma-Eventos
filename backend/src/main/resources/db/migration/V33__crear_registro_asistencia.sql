ALTER TABLE inscripcion ADD CONSTRAINT uq_inscripcion_id_organizacion
    UNIQUE (id_inscripcion, id_organizacion);

CREATE TABLE asistencia (
    id_asistencia BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_inscripcion BIGINT NOT NULL,
    id_organizacion BIGINT NOT NULL,
    id_usuario_staff BIGINT NOT NULL,
    fecha_hora TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_asistencia_inscripcion UNIQUE (id_inscripcion),
    CONSTRAINT fk_asistencia_inscripcion_tenant FOREIGN KEY (id_inscripcion, id_organizacion)
        REFERENCES inscripcion (id_inscripcion, id_organizacion) ON DELETE RESTRICT,
    CONSTRAINT fk_asistencia_organizacion FOREIGN KEY (id_organizacion)
        REFERENCES organizacion (id_organizacion) ON DELETE RESTRICT,
    CONSTRAINT fk_asistencia_staff FOREIGN KEY (id_usuario_staff)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
    CONSTRAINT fk_asistencia_staff_tenant FOREIGN KEY (id_usuario_staff, id_organizacion)
        REFERENCES usuario_organizacion (id_usuario, id_organizacion) ON DELETE RESTRICT
);

CREATE INDEX idx_asistencia_organizacion_fecha
    ON asistencia (id_organizacion, fecha_hora DESC);

INSERT INTO permiso (nombre, descripcion)
VALUES ('ASISTENCIA_REGISTRAR', 'Registrar asistencia de participantes en eventos asignados.')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r CROSS JOIN permiso p
WHERE r.nombre IN ('ADMINISTRADOR', 'ORGANIZADOR', 'STAFF', 'VOLUNTARIO')
  AND p.nombre = 'ASISTENCIA_REGISTRAR'
ON CONFLICT DO NOTHING;
