-- CU16: Postulaciones del usuario autenticado a convocatorias.
CREATE TABLE postulacion_staff (
    id_postulacion BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_convocatoria BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE',
    fecha_postulacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_postulacion_convocatoria FOREIGN KEY (id_convocatoria)
        REFERENCES convocatoria_personal (id_convocatoria) ON DELETE RESTRICT,
    CONSTRAINT fk_postulacion_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
    CONSTRAINT uk_postulacion_convocatoria_usuario UNIQUE (id_convocatoria, id_usuario),
    CONSTRAINT ck_postulacion_estado CHECK (estado IN ('PENDIENTE', 'RETIRADA'))
);

CREATE INDEX idx_postulacion_usuario_fecha ON postulacion_staff (id_usuario, fecha_postulacion DESC, id_postulacion DESC);

INSERT INTO permiso (nombre, descripcion) VALUES
    ('CONVOCATORIA_POSTULARSE', 'Registrar, consultar y retirar postulaciones propias a convocatorias.');

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT rol.id_rol, permiso.id_permiso
FROM rol
CROSS JOIN permiso
WHERE rol.nombre IN ('STAFF', 'PARTICIPANTE')
  AND permiso.nombre = 'CONVOCATORIA_POSTULARSE';
