CREATE TABLE certificado (
    id_certificado BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_inscripcion BIGINT NOT NULL,
    id_organizacion BIGINT NOT NULL,
    codigo_verificacion VARCHAR(64) NOT NULL,
    hash_seguridad VARCHAR(128) NOT NULL,
    fecha_emision TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_certificado_inscripcion UNIQUE (id_inscripcion),
    CONSTRAINT uq_certificado_codigo UNIQUE (codigo_verificacion),
    CONSTRAINT fk_certificado_inscripcion_tenant FOREIGN KEY (id_inscripcion, id_organizacion)
        REFERENCES inscripcion (id_inscripcion, id_organizacion) ON DELETE RESTRICT,
    CONSTRAINT fk_certificado_organizacion FOREIGN KEY (id_organizacion)
        REFERENCES organizacion (id_organizacion) ON DELETE RESTRICT
);

CREATE INDEX idx_certificado_org_codigo ON certificado(id_organizacion, codigo_verificacion);
CREATE INDEX idx_certificado_org_inscripcion ON certificado(id_organizacion, id_inscripcion);

INSERT INTO permiso (nombre, descripcion)
VALUES 
    ('CERTIFICADO_EMITIR', 'Emitir certificados digitales de asistencia a participantes.'),
    ('CERTIFICADO_CONSULTAR', 'Consultar y descargar certificados digitales de asistencia.')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r CROSS JOIN permiso p
WHERE r.nombre IN ('ADMINISTRADOR', 'ORGANIZADOR')
  AND p.nombre IN ('CERTIFICADO_EMITIR', 'CERTIFICADO_CONSULTAR')
ON CONFLICT DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r CROSS JOIN permiso p
WHERE r.nombre IN ('PARTICIPANTE', 'STAFF', 'VOLUNTARIO')
  AND p.nombre = 'CERTIFICADO_CONSULTAR'
ON CONFLICT DO NOTHING;
