ALTER TABLE categoria ADD COLUMN id_organizacion BIGINT;
UPDATE categoria SET id_organizacion = (SELECT id_organizacion FROM organizacion WHERE slug = 'principal');
ALTER TABLE categoria ALTER COLUMN id_organizacion SET NOT NULL;
ALTER TABLE categoria ADD CONSTRAINT fk_categoria_organizacion FOREIGN KEY (id_organizacion) REFERENCES organizacion(id_organizacion);
ALTER TABLE categoria DROP CONSTRAINT IF EXISTS categoria_nombre_key;
CREATE UNIQUE INDEX uk_categoria_organizacion_nombre ON categoria(id_organizacion, lower(nombre));
CREATE INDEX idx_categoria_organizacion ON categoria(id_organizacion);

CREATE TABLE usuario_organizacion_rol (
    id_usuario BIGINT NOT NULL,
    id_organizacion BIGINT NOT NULL,
    id_rol BIGINT NOT NULL,
    PRIMARY KEY (id_usuario, id_organizacion, id_rol),
    FOREIGN KEY (id_usuario, id_organizacion) REFERENCES usuario_organizacion(id_usuario, id_organizacion) ON DELETE CASCADE,
    FOREIGN KEY (id_rol) REFERENCES rol(id_rol) ON DELETE RESTRICT
);
INSERT INTO usuario_organizacion_rol (id_usuario, id_organizacion, id_rol)
SELECT uo.id_usuario, uo.id_organizacion, ur.id_rol
FROM usuario_organizacion uo JOIN usuario_rol ur ON ur.id_usuario = uo.id_usuario;
CREATE INDEX idx_usuario_organizacion_rol_rol ON usuario_organizacion_rol(id_rol);
