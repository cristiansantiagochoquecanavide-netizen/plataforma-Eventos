ALTER TABLE evento
    DROP CONSTRAINT IF EXISTS ck_evento_estado;

ALTER TABLE evento
    ADD CONSTRAINT ck_evento_estado
        CHECK (estado IN ('BORRADOR', 'PLANIFICADO', 'PUBLICADO', 'EN_CURSO', 'FINALIZADO', 'CANCELADO'));

CREATE TABLE evento_responsable (
    id_evento BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    CONSTRAINT pk_evento_responsable PRIMARY KEY (id_evento, id_usuario),
    CONSTRAINT fk_evento_responsable_evento FOREIGN KEY (id_evento)
        REFERENCES evento (id_evento) ON DELETE RESTRICT,
    CONSTRAINT fk_evento_responsable_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT
);

CREATE INDEX idx_evento_responsable_usuario ON evento_responsable (id_usuario);

INSERT INTO permiso (nombre, descripcion) VALUES
    ('EVENTO_CREAR', 'Crear eventos.'),
    ('EVENTO_EDITAR', 'Actualizar datos de eventos.'),
    ('EVENTO_ASIGNAR_RESPONSABLE', 'Asignar responsables a eventos.')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT rol.id_rol, permiso.id_permiso
FROM rol
CROSS JOIN permiso
WHERE rol.nombre = 'ADMINISTRADOR'
  AND permiso.nombre IN ('EVENTO_VER', 'EVENTO_CREAR', 'EVENTO_EDITAR', 'EVENTO_ASIGNAR_RESPONSABLE')
ON CONFLICT DO NOTHING;
