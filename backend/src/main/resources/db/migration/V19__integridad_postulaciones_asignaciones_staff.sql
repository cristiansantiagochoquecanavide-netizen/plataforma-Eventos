-- Normaliza filas creadas por V15 antes de endurecer las relaciones tenant-aware.
UPDATE postulacion_staff p
SET id_organizacion = c.id_organizacion
FROM convocatoria_personal c
WHERE c.id_convocatoria = p.id_convocatoria
  AND p.id_organizacion <> c.id_organizacion;

INSERT INTO usuario_organizacion (id_usuario, id_organizacion)
SELECT DISTINCT p.id_usuario, p.id_organizacion
FROM postulacion_staff p
WHERE NOT EXISTS (
    SELECT 1 FROM usuario_organizacion uo
    WHERE uo.id_usuario = p.id_usuario AND uo.id_organizacion = p.id_organizacion
);

UPDATE asignacion_staff a
SET id_organizacion = p.id_organizacion
FROM postulacion_staff p
WHERE p.id_postulacion = a.id_postulacion
  AND a.id_organizacion <> p.id_organizacion;

ALTER TABLE asignacion_staff ADD COLUMN id_usuario BIGINT;
UPDATE asignacion_staff a SET id_usuario = p.id_usuario
FROM postulacion_staff p WHERE p.id_postulacion = a.id_postulacion;
ALTER TABLE asignacion_staff ALTER COLUMN id_usuario SET NOT NULL;

-- Conserva el registro más antiguo cuando una instalación previa tenía duplicados.
WITH duplicadas AS (
    SELECT id_asignacion,
           row_number() OVER (PARTITION BY id_postulacion ORDER BY fecha_creacion, id_asignacion) AS posicion
    FROM asignacion_staff
    WHERE estado = 'ASIGNADA'
)
UPDATE asignacion_staff a
SET estado = 'CANCELADA'
FROM duplicadas d
WHERE a.id_asignacion = d.id_asignacion AND d.posicion > 1;

ALTER TABLE postulacion_staff
    DROP CONSTRAINT IF EXISTS uq_postulacion_usuario_convocatoria;
ALTER TABLE postulacion_staff
    ADD CONSTRAINT uq_postulacion_usuario_convocatoria UNIQUE (id_convocatoria, id_usuario, id_organizacion);

ALTER TABLE convocatoria_personal
    ADD CONSTRAINT uq_convocatoria_organizacion UNIQUE (id_convocatoria, id_organizacion);
ALTER TABLE postulacion_staff
    ADD CONSTRAINT fk_postulacion_convocatoria_tenant
        FOREIGN KEY (id_convocatoria, id_organizacion)
        REFERENCES convocatoria_personal (id_convocatoria, id_organizacion);
ALTER TABLE postulacion_staff
    ADD CONSTRAINT uq_postulacion_id_organizacion UNIQUE (id_postulacion, id_organizacion);
ALTER TABLE postulacion_staff
    ADD CONSTRAINT fk_postulacion_membresia
        FOREIGN KEY (id_usuario, id_organizacion)
        REFERENCES usuario_organizacion (id_usuario, id_organizacion);
ALTER TABLE asignacion_staff
    ADD CONSTRAINT fk_asignacion_postulacion_tenant
        FOREIGN KEY (id_postulacion, id_organizacion)
        REFERENCES postulacion_staff (id_postulacion, id_organizacion);
ALTER TABLE asignacion_staff
    ADD CONSTRAINT fk_asignacion_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario);
ALTER TABLE asignacion_staff
    ADD CONSTRAINT fk_asignacion_membresia FOREIGN KEY (id_usuario, id_organizacion)
        REFERENCES usuario_organizacion (id_usuario, id_organizacion);

CREATE UNIQUE INDEX uq_asignacion_staff_activa_postulacion
    ON asignacion_staff (id_postulacion) WHERE estado = 'ASIGNADA';
CREATE INDEX idx_postulacion_tenant_estado_fecha
    ON postulacion_staff (id_organizacion, estado, fecha_postulacion);
CREATE INDEX idx_asignacion_tenant_estado_fecha
    ON asignacion_staff (id_organizacion, estado, turno_inicio);
CREATE INDEX idx_asignacion_tenant_usuario
    ON asignacion_staff (id_organizacion, id_usuario, estado, turno_inicio);
