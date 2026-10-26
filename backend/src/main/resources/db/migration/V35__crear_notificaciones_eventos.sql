CREATE TABLE notificaciones_eventos (
    id_notificacion BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_evento BIGINT NOT NULL REFERENCES evento(id_evento) ON DELETE CASCADE,
    id_inscripcion BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    id_organizacion BIGINT NOT NULL REFERENCES organizacion(id_organizacion) ON DELETE CASCADE,
    tipo_notificacion VARCHAR(40) NOT NULL,
    canal_envio VARCHAR(20) NOT NULL,
    asunto VARCHAR(150) NOT NULL,
    cuerpo_mensaje TEXT NOT NULL,
    fecha_programada TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_envio TIMESTAMP WITH TIME ZONE,
    estado_envio VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
        CHECK (estado_envio IN ('PENDIENTE', 'PROCESANDO', 'ENVIADO', 'FALLIDO', 'CANCELADO')),
    id_mensaje_externo VARCHAR(150),
    clave_deduplicacion VARCHAR(180) NOT NULL,
    ultimo_error VARCHAR(500),
    intentos INTEGER NOT NULL DEFAULT 0 CHECK (intentos >= 0),
    CONSTRAINT fk_notificacion_inscripcion_tenant
        FOREIGN KEY (id_inscripcion, id_organizacion)
        REFERENCES inscripcion (id_inscripcion, id_organizacion) ON DELETE CASCADE,
    CONSTRAINT fk_notificacion_usuario_tenant
        FOREIGN KEY (id_usuario, id_organizacion)
        REFERENCES usuario_organizacion (id_usuario, id_organizacion) ON DELETE CASCADE,
    CONSTRAINT uq_notificacion_tenant_deduplicacion
        UNIQUE (id_organizacion, clave_deduplicacion)
);

CREATE INDEX idx_notificaciones_evento_tenant_fecha
    ON notificaciones_eventos (id_organizacion, id_evento, fecha_programada DESC);

CREATE INDEX idx_notificaciones_pendientes
    ON notificaciones_eventos (fecha_programada, id_notificacion)
    WHERE estado_envio = 'PENDIENTE';
