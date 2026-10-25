-- Crear tabla inscripcion (CU11, CU12, CU13)
CREATE TABLE inscripcion (
    id_inscripcion BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_evento BIGINT NOT NULL,
    id_participante BIGINT NOT NULL,
    fecha_inscripcion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    codigo_qr VARCHAR(255) NOT NULL UNIQUE,
    estado VARCHAR(30) NOT NULL DEFAULT 'CONFIRMADA',
    CONSTRAINT fk_inscripcion_evento FOREIGN KEY (id_evento)
        REFERENCES evento (id_evento) ON DELETE CASCADE,
    CONSTRAINT fk_inscripcion_participante FOREIGN KEY (id_participante)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
    CONSTRAINT ck_inscripcion_estado CHECK (estado IN ('CONFIRMADA', 'CANCELADA', 'EN_ESPERA')),
    CONSTRAINT uq_inscripcion_evento_participante UNIQUE (id_evento, id_participante)
);

CREATE INDEX idx_inscripcion_evento ON inscripcion (id_evento);
CREATE INDEX idx_inscripcion_participante ON inscripcion (id_participante);
CREATE INDEX idx_inscripcion_codigo_qr ON inscripcion (codigo_qr);
CREATE INDEX idx_inscripcion_estado ON inscripcion (estado);

-- Insertar permisos para inscripciones
INSERT INTO permiso (nombre, descripcion) VALUES
    ('INSCRIPCION_CREAR', 'Inscribirse a eventos disponibles.'),
    ('INSCRIPCION_VER_PROPIAS', 'Consultar inscripciones propias.'),
    ('INSCRIPCION_CANCELAR', 'Cancelar inscripciones propias.')
ON CONFLICT (nombre) DO NOTHING;

-- Asignar permisos a PARTICIPANTE, STAFF, ORGANIZADOR y ADMINISTRADOR
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT rol.id_rol, permiso.id_permiso
FROM rol
CROSS JOIN permiso
WHERE rol.nombre IN ('PARTICIPANTE', 'STAFF', 'ORGANIZADOR', 'ADMINISTRADOR')
  AND permiso.nombre IN ('INSCRIPCION_CREAR', 'INSCRIPCION_VER_PROPIAS', 'INSCRIPCION_CANCELAR')
ON CONFLICT DO NOTHING;
