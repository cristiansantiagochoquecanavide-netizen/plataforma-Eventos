-- Crear tabla evento
CREATE TABLE evento (
    id_evento BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(500),
    fecha_inicio TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_fin TIMESTAMP WITH TIME ZONE NOT NULL,
    lugar_principal VARCHAR(150),
    estado VARCHAR(20) NOT NULL DEFAULT 'PUBLICADO',
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_evento_fechas CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT ck_evento_estado CHECK (estado IN ('BORRADOR', 'PUBLICADO', 'EN_CURSO', 'FINALIZADO', 'CANCELADO'))
);

CREATE INDEX idx_evento_estado ON evento (estado);
CREATE INDEX idx_evento_fechas ON evento (fecha_inicio, fecha_fin);

-- Crear tabla asignacion_recurso (CU20)
CREATE TABLE asignacion_recurso (
    id_asignacion BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_evento BIGINT NOT NULL,
    id_recurso BIGINT NOT NULL,
    sala_espacio VARCHAR(100) NOT NULL,
    cantidad_asignada INT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    observaciones VARCHAR(255),
    fecha_asignacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_devolucion TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_asignacion_evento FOREIGN KEY (id_evento)
        REFERENCES evento (id_evento) ON DELETE RESTRICT,
    CONSTRAINT fk_asignacion_recurso FOREIGN KEY (id_recurso)
        REFERENCES recurso (id_recurso) ON DELETE RESTRICT,
    CONSTRAINT ck_asignacion_cantidad CHECK (cantidad_asignada > 0),
    CONSTRAINT ck_asignacion_estado CHECK (estado IN ('ACTIVA', 'DEVUELTA', 'CANCELADA'))
);

CREATE INDEX idx_asignacion_evento ON asignacion_recurso (id_evento);
CREATE INDEX idx_asignacion_recurso ON asignacion_recurso (id_recurso);
CREATE INDEX idx_asignacion_estado ON asignacion_recurso (estado);

-- Insertar permisos para asignacion de recursos y consulta de eventos
INSERT INTO permiso (nombre, descripcion) VALUES
    ('EVENTO_VER', 'Consultar eventos del sistema.'),
    ('ASIGNACION_RECURSO_VER', 'Consultar recursos asignados a eventos y salas.'),
    ('ASIGNACION_RECURSO_CREAR', 'Asignar recursos a eventos y salas.'),
    ('ASIGNACION_RECURSO_LIBERAR', 'Liberar o devolver recursos asignados.');

-- Asignar permisos a ADMINISTRADOR
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT rol.id_rol, permiso.id_permiso
FROM rol
CROSS JOIN permiso
WHERE rol.nombre = 'ADMINISTRADOR'
  AND permiso.nombre IN ('EVENTO_VER', 'ASIGNACION_RECURSO_VER', 'ASIGNACION_RECURSO_CREAR', 'ASIGNACION_RECURSO_LIBERAR');

-- Asignar permisos a ORGANIZADOR
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT rol.id_rol, permiso.id_permiso
FROM rol
CROSS JOIN permiso
WHERE rol.nombre = 'ORGANIZADOR'
  AND permiso.nombre IN ('EVENTO_VER', 'ASIGNACION_RECURSO_VER', 'ASIGNACION_RECURSO_CREAR', 'ASIGNACION_RECURSO_LIBERAR');

-- Insertar eventos iniciales para pruebas de asignación
INSERT INTO evento (nombre, descripcion, fecha_inicio, fecha_fin, lugar_principal, estado) VALUES
    ('Congreso Internacional de Tecnología 2026', 'Congreso anual sobre desarrollo de software, arquitectura cloud e inteligencia artificial.', CURRENT_TIMESTAMP + INTERVAL '5 days', CURRENT_TIMESTAMP + INTERVAL '7 days', 'Centro de Convenciones Norte', 'PUBLICADO'),
    ('Seminario de Inteligencia Artificial & Big Data', 'Taller y ponencias sobre modelos generativos y ciencia de datos aplicada.', CURRENT_TIMESTAMP + INTERVAL '12 days', CURRENT_TIMESTAMP + INTERVAL '13 days', 'Campus Universitario - Pabellón A', 'PUBLICADO'),
    ('Hackathon Innovación Abierta 2026', 'Competencia de 48 horas de desarrollo e innovación para estudiantes.', CURRENT_TIMESTAMP + INTERVAL '20 days', CURRENT_TIMESTAMP + INTERVAL '22 days', 'Auditorio Central', 'PUBLICADO');
