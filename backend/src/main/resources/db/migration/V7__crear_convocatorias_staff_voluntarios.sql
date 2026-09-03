-- CU15: Gestion de Convocatorias de Staff y Voluntarios.
-- Registra necesidades de personal por evento, conserva su estado y habilita
-- la consulta, el cierre y la cancelacion sin eliminar el historial.
CREATE TABLE convocatoria_personal (
    id_convocatoria BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_evento BIGINT NOT NULL,
    tipo_personal VARCHAR(15) NOT NULL,
    descripcion VARCHAR(1000) NOT NULL,
    cantidad_requerida INT NOT NULL,
    fecha_limite DATE NOT NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'ABIERTA',
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_convocatoria_evento FOREIGN KEY (id_evento)
        REFERENCES evento (id_evento) ON DELETE RESTRICT,
    CONSTRAINT ck_convocatoria_tipo
        CHECK (tipo_personal IN ('STAFF', 'VOLUNTARIO')),
    CONSTRAINT ck_convocatoria_cantidad
        CHECK (cantidad_requerida > 0),
    CONSTRAINT ck_convocatoria_estado
        CHECK (estado IN ('ABIERTA', 'CERRADA', 'CANCELADA')),
    CONSTRAINT ck_convocatoria_descripcion
        CHECK (LENGTH(TRIM(descripcion)) > 0)
);

CREATE INDEX idx_convocatoria_evento ON convocatoria_personal (id_evento);
CREATE INDEX idx_convocatoria_estado ON convocatoria_personal (estado);
CREATE INDEX idx_convocatoria_tipo ON convocatoria_personal (tipo_personal);
CREATE INDEX idx_convocatoria_fecha_limite ON convocatoria_personal (fecha_limite);

INSERT INTO permiso (nombre, descripcion) VALUES
    ('CONVOCATORIA_VER', 'Consultar convocatorias de staff y voluntarios.'),
    ('CONVOCATORIA_CREAR', 'Registrar convocatorias asociadas a eventos.'),
    ('CONVOCATORIA_CERRAR', 'Cerrar convocatorias abiertas.'),
    ('CONVOCATORIA_CANCELAR', 'Cancelar convocatorias abiertas.');

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT rol.id_rol, permiso.id_permiso
FROM rol
CROSS JOIN permiso
WHERE rol.nombre IN ('ADMINISTRADOR', 'ORGANIZADOR')
  AND permiso.nombre IN (
      'CONVOCATORIA_VER',
      'CONVOCATORIA_CREAR',
      'CONVOCATORIA_CERRAR',
      'CONVOCATORIA_CANCELAR'
  );

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT rol.id_rol, permiso.id_permiso
FROM rol
CROSS JOIN permiso
WHERE rol.nombre IN ('STAFF', 'PARTICIPANTE')
  AND permiso.nombre = 'CONVOCATORIA_VER';
