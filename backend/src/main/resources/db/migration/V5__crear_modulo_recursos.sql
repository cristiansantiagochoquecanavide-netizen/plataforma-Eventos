CREATE TABLE recurso (
    id_recurso BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    cantidad_total INT NOT NULL DEFAULT 1,
    cantidad_disponible INT NOT NULL DEFAULT 1,
    estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
    ubicacion VARCHAR(150),
    descripcion VARCHAR(255),
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_recurso_tipo CHECK (tipo IN ('AUDIOVISUAL', 'MOBILIARIO', 'EQUIPO_COMPUTO', 'ESPACIO', 'OTRO')),
    CONSTRAINT ck_recurso_estado CHECK (estado IN ('DISPONIBLE', 'EN_USO', 'MANTENIMIENTO', 'DE_BAJA')),
    CONSTRAINT ck_recurso_cantidades CHECK (cantidad_total >= 0 AND cantidad_disponible >= 0 AND cantidad_disponible <= cantidad_total)
);

CREATE INDEX idx_recurso_tipo ON recurso (tipo);
CREATE INDEX idx_recurso_estado ON recurso (estado);
CREATE INDEX idx_recurso_codigo ON recurso (codigo);

INSERT INTO permiso (nombre, descripcion) VALUES
    ('RECURSO_VER', 'Consultar y listar recursos del sistema.'),
    ('RECURSO_CREAR', 'Registrar nuevos recursos en el sistema.'),
    ('RECURSO_EDITAR', 'Actualizar informacion de recursos existentes.'),
    ('RECURSO_CAMBIAR_ESTADO', 'Cambiar el estado operativo de un recurso.');

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT rol.id_rol, permiso.id_permiso
FROM rol
CROSS JOIN permiso
WHERE rol.nombre = 'ADMINISTRADOR'
  AND permiso.nombre IN ('RECURSO_VER', 'RECURSO_CREAR', 'RECURSO_EDITAR', 'RECURSO_CAMBIAR_ESTADO');

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT rol.id_rol, permiso.id_permiso
FROM rol
CROSS JOIN permiso
WHERE rol.nombre = 'ORGANIZADOR'
  AND permiso.nombre IN ('RECURSO_VER', 'RECURSO_CREAR', 'RECURSO_EDITAR', 'RECURSO_CAMBIAR_ESTADO');
