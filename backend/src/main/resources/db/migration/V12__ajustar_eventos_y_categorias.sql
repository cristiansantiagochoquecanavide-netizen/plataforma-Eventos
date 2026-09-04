ALTER TABLE evento
    ALTER COLUMN fecha_fin DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_categoria_nombre_normalizado
    ON categoria (LOWER(nombre));
