-- CU17: decision sobre postulaciones CU16 y registro del equipo seleccionado.
ALTER TABLE postulacion_staff
    DROP CONSTRAINT ck_postulacion_estado;

ALTER TABLE postulacion_staff
    ADD CONSTRAINT ck_postulacion_estado
    CHECK (estado IN ('PENDIENTE', 'RETIRADA', 'SELECCIONADA', 'RECHAZADA'));

CREATE INDEX idx_postulacion_convocatoria_fecha
    ON postulacion_staff (id_convocatoria, fecha_postulacion, id_postulacion);

CREATE TABLE asignacion_staff (
    id_asignacion BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_postulacion BIGINT NOT NULL,
    fecha_asignacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_asignacion_staff_postulacion FOREIGN KEY (id_postulacion)
        REFERENCES postulacion_staff (id_postulacion) ON DELETE RESTRICT,
    CONSTRAINT uk_asignacion_staff_postulacion UNIQUE (id_postulacion)
);

INSERT INTO permiso (nombre, descripcion) VALUES
    ('CONVOCATORIA_GESTIONAR_POSTULACIONES', 'Revisar, seleccionar y rechazar postulaciones del evento a cargo.');

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT rol.id_rol, permiso.id_permiso
FROM rol
CROSS JOIN permiso
WHERE rol.nombre IN ('ADMINISTRADOR', 'ORGANIZADOR', 'STAFF')
  AND permiso.nombre = 'CONVOCATORIA_GESTIONAR_POSTULACIONES';
