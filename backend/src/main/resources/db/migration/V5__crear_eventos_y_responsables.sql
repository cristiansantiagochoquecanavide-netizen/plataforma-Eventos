CREATE TABLE evento (
    id_evento BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(500),
    fecha_inicio TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_fin TIMESTAMP WITH TIME ZONE,
    estado VARCHAR(20) NOT NULL,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_evento_estado CHECK (estado IN ('PLANIFICADO', 'CANCELADO', 'FINALIZADO')),
    CONSTRAINT ck_evento_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);

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
CREATE INDEX idx_evento_fecha_inicio ON evento (fecha_inicio);

INSERT INTO permiso (nombre, descripcion) VALUES
    ('EVENTO_VER', 'Consultar eventos y responsables asignados.'),
    ('EVENTO_CREAR', 'Crear eventos.'),
    ('EVENTO_EDITAR', 'Actualizar datos de eventos.'),
    ('EVENTO_ASIGNAR_RESPONSABLE', 'Asignar responsables a eventos.');

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT rol.id_rol, permiso.id_permiso
FROM rol
CROSS JOIN permiso
WHERE rol.nombre = 'ADMINISTRADOR'
  AND permiso.nombre IN ('EVENTO_VER', 'EVENTO_CREAR', 'EVENTO_EDITAR', 'EVENTO_ASIGNAR_RESPONSABLE');
