CREATE TABLE postulacion_staff (
 id_postulacion BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_convocatoria BIGINT NOT NULL, id_usuario BIGINT NOT NULL, id_organizacion BIGINT NOT NULL,
 motivacion VARCHAR(1000), estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
 fecha_postulacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (id_convocatoria) REFERENCES convocatoria_personal(id_convocatoria) ON DELETE RESTRICT,
 FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
 FOREIGN KEY (id_organizacion) REFERENCES organizacion(id_organizacion) ON DELETE RESTRICT,
 CONSTRAINT ck_postulacion_estado CHECK (estado IN ('PENDIENTE','ACEPTADA','RECHAZADA')),
 CONSTRAINT uq_postulacion_usuario_convocatoria UNIQUE (id_convocatoria,id_usuario)
);
CREATE TABLE asignacion_staff (
 id_asignacion BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_postulacion BIGINT NOT NULL, id_organizacion BIGINT NOT NULL,
 tarea VARCHAR(255) NOT NULL, turno_inicio TIMESTAMP WITH TIME ZONE, turno_fin TIMESTAMP WITH TIME ZONE,
 observaciones VARCHAR(1000), estado VARCHAR(20) NOT NULL DEFAULT 'ASIGNADA',
 fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (id_postulacion) REFERENCES postulacion_staff(id_postulacion) ON DELETE RESTRICT,
 FOREIGN KEY (id_organizacion) REFERENCES organizacion(id_organizacion) ON DELETE RESTRICT,
 CONSTRAINT ck_asignacion_staff_estado CHECK (estado IN ('ASIGNADA','CANCELADA')),
 CONSTRAINT ck_asignacion_staff_turno CHECK (turno_fin IS NULL OR turno_inicio IS NULL OR turno_fin > turno_inicio)
);
CREATE INDEX idx_postulacion_org ON postulacion_staff(id_organizacion);
CREATE INDEX idx_asignacion_org ON asignacion_staff(id_organizacion);
