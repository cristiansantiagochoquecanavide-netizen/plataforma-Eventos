CREATE TABLE espacio (
    id_espacio BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    ubicacion VARCHAR(255) NOT NULL,
    capacidad INT NOT NULL,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_espacio_capacidad CHECK (capacidad > 0),
    CONSTRAINT uq_espacio_nombre_ubicacion UNIQUE (nombre, ubicacion)
);

ALTER TABLE evento ADD COLUMN id_espacio BIGINT;
ALTER TABLE evento ADD COLUMN aforo_maximo INT;

ALTER TABLE evento
    ADD CONSTRAINT fk_evento_espacio FOREIGN KEY (id_espacio)
        REFERENCES espacio (id_espacio) ON DELETE RESTRICT;

ALTER TABLE evento
    ADD CONSTRAINT ck_evento_aforo CHECK (aforo_maximo IS NULL OR aforo_maximo > 0);

CREATE INDEX idx_evento_espacio ON evento (id_espacio);
CREATE INDEX idx_espacio_ubicacion ON espacio (ubicacion);

INSERT INTO permiso (nombre, descripcion) VALUES
    ('ESPACIO_VER', 'Consultar espacios y salas.'),
    ('ESPACIO_CREAR', 'Registrar espacios y salas.'),
    ('ESPACIO_EDITAR', 'Actualizar espacios y salas.'),
    ('PROGRAMACION_VERIFICAR', 'Verificar disponibilidad y conflictos de programacion.')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT rol.id_rol, permiso.id_permiso
FROM rol
CROSS JOIN permiso
WHERE rol.nombre IN ('ADMINISTRADOR', 'ORGANIZADOR')
  AND permiso.nombre IN ('ESPACIO_VER', 'ESPACIO_CREAR', 'ESPACIO_EDITAR', 'PROGRAMACION_VERIFICAR')
ON CONFLICT DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT rol.id_rol, permiso.id_permiso
FROM rol
CROSS JOIN permiso
WHERE rol.nombre = 'ORGANIZADOR'
  AND permiso.nombre IN ('EVENTO_VER', 'EVENTO_CREAR', 'EVENTO_EDITAR', 'EVENTO_ASIGNAR_RESPONSABLE')
ON CONFLICT DO NOTHING;
