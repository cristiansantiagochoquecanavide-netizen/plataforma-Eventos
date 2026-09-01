CREATE TABLE categoria (
    id_categoria BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO categoria (nombre, descripcion) VALUES
    ('General', 'Categoria inicial para eventos existentes.');

ALTER TABLE evento
    ADD COLUMN id_categoria BIGINT;

UPDATE evento
SET id_categoria = (
    SELECT id_categoria
    FROM categoria
    WHERE nombre = 'General'
)
WHERE id_categoria IS NULL;

ALTER TABLE evento
    ALTER COLUMN id_categoria SET NOT NULL;

ALTER TABLE evento
    ADD CONSTRAINT fk_evento_categoria FOREIGN KEY (id_categoria)
        REFERENCES categoria (id_categoria) ON DELETE RESTRICT;

CREATE INDEX idx_evento_categoria ON evento (id_categoria);
