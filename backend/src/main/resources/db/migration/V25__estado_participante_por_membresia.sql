ALTER TABLE usuario_organizacion
    ADD COLUMN estado_participante VARCHAR(10) NOT NULL DEFAULT 'ACTIVO';

UPDATE usuario_organizacion
SET estado_participante = 'ACTIVO'
WHERE estado_participante IS NULL;

ALTER TABLE usuario_organizacion
    ADD CONSTRAINT chk_usuario_organizacion_estado_participante
    CHECK (estado_participante IN ('ACTIVO', 'INACTIVO'));
