CREATE TABLE IF NOT EXISTS categoria (
    id_categoria BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO categoria (nombre, descripcion) VALUES
    ('General', 'Categoria inicial para eventos existentes.')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO categoria (nombre, descripcion)
SELECT categorias_migradas.nombre, 'Categoria migrada desde eventos existentes.'
FROM (
    SELECT MIN(TRIM(e.categoria)) AS nombre
    FROM evento e
    WHERE e.categoria IS NOT NULL
      AND TRIM(e.categoria) <> ''
    GROUP BY LOWER(TRIM(e.categoria))
) categorias_migradas
WHERE NOT EXISTS (
    SELECT 1
    FROM categoria c
    WHERE LOWER(c.nombre) = LOWER(categorias_migradas.nombre)
)
ON CONFLICT (nombre) DO NOTHING;

ALTER TABLE evento
    ADD COLUMN IF NOT EXISTS id_categoria BIGINT;

UPDATE evento e
SET id_categoria = c.id_categoria
FROM categoria c
WHERE e.id_categoria IS NULL
  AND e.categoria IS NOT NULL
  AND LOWER(TRIM(e.categoria)) = LOWER(c.nombre);

UPDATE evento e
SET id_categoria = c.id_categoria
FROM categoria c
WHERE e.id_categoria IS NULL
  AND c.nombre = 'General';

ALTER TABLE evento
    ALTER COLUMN id_categoria SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_evento_categoria'
    ) THEN
        ALTER TABLE evento
            ADD CONSTRAINT fk_evento_categoria FOREIGN KEY (id_categoria)
                REFERENCES categoria (id_categoria) ON DELETE RESTRICT;
    END IF;
END $$;

DROP INDEX IF EXISTS idx_evento_categoria;
CREATE INDEX idx_evento_categoria ON evento (id_categoria);

ALTER TABLE evento
    DROP COLUMN IF EXISTS categoria;
