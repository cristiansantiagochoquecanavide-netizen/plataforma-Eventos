CREATE TABLE suscripcion_organizacion (
    id_organizacion BIGINT PRIMARY KEY,
    plan VARCHAR(24) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    fecha_inicio TIMESTAMPTZ NOT NULL,
    fecha_fin TIMESTAMPTZ,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_suscripcion_organizacion FOREIGN KEY (id_organizacion)
        REFERENCES organizacion(id_organizacion),
    CONSTRAINT chk_suscripcion_plan CHECK (plan IN ('BASICO', 'PROFESIONAL', 'EMPRESARIAL')),
    CONSTRAINT chk_suscripcion_estado CHECK (estado IN ('ACTIVA', 'SUSPENDIDA', 'VENCIDA')),
    CONSTRAINT chk_suscripcion_fechas CHECK (fecha_fin IS NULL OR fecha_fin > fecha_inicio)
);

INSERT INTO suscripcion_organizacion (id_organizacion, plan, estado, fecha_inicio)
SELECT id_organizacion, 'BASICO', 'ACTIVA', CURRENT_TIMESTAMP
FROM organizacion;

CREATE INDEX idx_suscripcion_estado_fin ON suscripcion_organizacion(estado, fecha_fin);
