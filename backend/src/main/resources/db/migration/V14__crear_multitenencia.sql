CREATE TABLE organizacion (
    id_organizacion BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuario_organizacion (
    id_usuario BIGINT NOT NULL,
    id_organizacion BIGINT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_usuario, id_organizacion),
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    FOREIGN KEY (id_organizacion) REFERENCES organizacion(id_organizacion) ON DELETE CASCADE
);

INSERT INTO organizacion (nombre, slug) VALUES ('Organizacion principal', 'principal');
INSERT INTO usuario_organizacion (id_usuario, id_organizacion)
SELECT u.id_usuario, o.id_organizacion FROM usuario u CROSS JOIN organizacion o WHERE o.slug = 'principal';

ALTER TABLE evento ADD COLUMN id_organizacion BIGINT;
ALTER TABLE espacio ADD COLUMN id_organizacion BIGINT;
ALTER TABLE recurso ADD COLUMN id_organizacion BIGINT;
ALTER TABLE inscripcion ADD COLUMN id_organizacion BIGINT;
ALTER TABLE convocatoria_personal ADD COLUMN id_organizacion BIGINT;
UPDATE evento SET id_organizacion = (SELECT id_organizacion FROM organizacion WHERE slug = 'principal');
UPDATE espacio SET id_organizacion = (SELECT id_organizacion FROM organizacion WHERE slug = 'principal');
UPDATE recurso SET id_organizacion = (SELECT id_organizacion FROM organizacion WHERE slug = 'principal');
UPDATE inscripcion SET id_organizacion = (SELECT id_organizacion FROM organizacion WHERE slug = 'principal');
UPDATE convocatoria_personal SET id_organizacion = (SELECT id_organizacion FROM organizacion WHERE slug = 'principal');
ALTER TABLE evento ADD CONSTRAINT fk_evento_organizacion FOREIGN KEY (id_organizacion) REFERENCES organizacion(id_organizacion);
ALTER TABLE espacio ADD CONSTRAINT fk_espacio_organizacion FOREIGN KEY (id_organizacion) REFERENCES organizacion(id_organizacion);
ALTER TABLE recurso ADD CONSTRAINT fk_recurso_organizacion FOREIGN KEY (id_organizacion) REFERENCES organizacion(id_organizacion);
ALTER TABLE inscripcion ADD CONSTRAINT fk_inscripcion_organizacion FOREIGN KEY (id_organizacion) REFERENCES organizacion(id_organizacion);
ALTER TABLE convocatoria_personal ADD CONSTRAINT fk_convocatoria_organizacion FOREIGN KEY (id_organizacion) REFERENCES organizacion(id_organizacion);
CREATE INDEX idx_usuario_organizacion_org ON usuario_organizacion(id_organizacion);
CREATE INDEX idx_evento_organizacion ON evento(id_organizacion);
CREATE INDEX idx_espacio_organizacion ON espacio(id_organizacion);
CREATE INDEX idx_recurso_organizacion ON recurso(id_organizacion);
CREATE INDEX idx_inscripcion_organizacion ON inscripcion(id_organizacion);
CREATE INDEX idx_convocatoria_organizacion ON convocatoria_personal(id_organizacion);

INSERT INTO permiso (nombre, descripcion) VALUES
 ('ORGANIZACION_GESTIONAR', 'Gestionar organizaciones y membresias.'),
 ('POSTULACION_STAFF_CREAR', 'Postularse a convocatorias de staff.'),
 ('POSTULACION_STAFF_GESTIONAR', 'Gestionar postulaciones de staff.'),
 ('ASIGNACION_STAFF_GESTIONAR', 'Gestionar asignaciones de staff.'),
 ('BITACORA_VER', 'Consultar la bitacora de auditoria.') ON CONFLICT (nombre) DO NOTHING;
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso FROM rol r CROSS JOIN permiso p
WHERE r.nombre IN ('ADMINISTRADOR','ORGANIZADOR') AND p.nombre IN ('ORGANIZACION_GESTIONAR','POSTULACION_STAFF_GESTIONAR','ASIGNACION_STAFF_GESTIONAR','BITACORA_VER') ON CONFLICT DO NOTHING;
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso FROM rol r CROSS JOIN permiso p
WHERE r.nombre IN ('STAFF','VOLUNTARIO','PARTICIPANTE') AND p.nombre = 'POSTULACION_STAFF_CREAR' ON CONFLICT DO NOTHING;
