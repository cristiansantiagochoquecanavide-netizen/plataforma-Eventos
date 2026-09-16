CREATE TABLE bitacora (
 id_bitacora BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_organizacion BIGINT NOT NULL, id_actor BIGINT, accion VARCHAR(100) NOT NULL,
 modulo VARCHAR(100) NOT NULL, entidad VARCHAR(100), id_entidad BIGINT,
 fecha TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 ip VARCHAR(64), exito BOOLEAN NOT NULL, resultado VARCHAR(1000),
 FOREIGN KEY (id_organizacion) REFERENCES organizacion(id_organizacion) ON DELETE RESTRICT,
 FOREIGN KEY (id_actor) REFERENCES usuario(id_usuario) ON DELETE SET NULL
);
CREATE INDEX idx_bitacora_org_fecha ON bitacora(id_organizacion, fecha DESC);
