ALTER TABLE token_recuperacion
    ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN fecha_inactivacion TIMESTAMP WITH TIME ZONE;

UPDATE token_recuperacion
SET activo = FALSE,
    fecha_inactivacion = COALESCE(fecha_uso, CURRENT_TIMESTAMP)
WHERE utilizado = TRUE;

ALTER TABLE token_recuperacion
    ADD CONSTRAINT ck_token_recuperacion_activo
        CHECK ((activo AND fecha_inactivacion IS NULL) OR (NOT activo AND fecha_inactivacion IS NOT NULL));

CREATE INDEX idx_token_recuperacion_usuario_activo
    ON token_recuperacion (id_usuario, activo, fecha_expiracion);
