-- V36: Entradas multicategoria (General, VIP, etc.), costos de espacios/recursos y finanzas de eventos

CREATE TABLE tipo_entrada (
    id_tipo_entrada BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_evento BIGINT NOT NULL,
    id_organizacion BIGINT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    precio NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    cupo_maximo INT NOT NULL DEFAULT 100,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tipo_entrada_evento FOREIGN KEY (id_evento)
        REFERENCES evento (id_evento) ON DELETE CASCADE,
    CONSTRAINT fk_tipo_entrada_organizacion FOREIGN KEY (id_organizacion)
        REFERENCES organizacion (id_organizacion) ON DELETE CASCADE,
    CONSTRAINT ck_tipo_entrada_precio CHECK (precio >= 0),
    CONSTRAINT ck_tipo_entrada_cupo CHECK (cupo_maximo >= 0)
);

CREATE INDEX idx_tipo_entrada_evento ON tipo_entrada (id_evento);
CREATE INDEX idx_tipo_entrada_organizacion ON tipo_entrada (id_organizacion);

-- Campos financieros en inscripcion
ALTER TABLE inscripcion ADD COLUMN id_tipo_entrada BIGINT;
ALTER TABLE inscripcion ADD COLUMN monto_pagado NUMERIC(10, 2) NOT NULL DEFAULT 0.00;

ALTER TABLE inscripcion
    ADD CONSTRAINT fk_inscripcion_tipo_entrada FOREIGN KEY (id_tipo_entrada)
        REFERENCES tipo_entrada (id_tipo_entrada) ON DELETE SET NULL;

ALTER TABLE inscripcion
    ADD CONSTRAINT ck_inscripcion_monto CHECK (monto_pagado >= 0);

CREATE INDEX idx_inscripcion_tipo_entrada ON inscripcion (id_tipo_entrada);

-- Costo de alquiler en espacio y costo unitario en recurso
ALTER TABLE espacio ADD COLUMN costo_alquiler NUMERIC(10, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE espacio ADD CONSTRAINT ck_espacio_costo CHECK (costo_alquiler >= 0);

ALTER TABLE recurso ADD COLUMN costo_unitario NUMERIC(10, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE recurso ADD CONSTRAINT ck_recurso_costo CHECK (costo_unitario >= 0);

-- Compatibilidad hacia atras: crear categoria 'General' (0.00 Bs) para eventos existentes
INSERT INTO tipo_entrada (id_evento, id_organizacion, nombre, precio, cupo_maximo, descripcion)
SELECT e.id_evento, e.id_organizacion, 'General', 0.00, COALESCE(e.aforo_maximo, 100), 'Entrada General'
FROM evento e
WHERE e.id_organizacion IS NOT NULL;

UPDATE inscripcion i
SET id_tipo_entrada = te.id_tipo_entrada,
    monto_pagado = 0.00
FROM tipo_entrada te
WHERE te.id_evento = i.id_evento AND te.nombre = 'General';
