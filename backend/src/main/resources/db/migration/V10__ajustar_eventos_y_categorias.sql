ALTER TABLE evento
    ALTER COLUMN fecha_fin DROP NOT NULL;

CREATE UNIQUE INDEX uk_categoria_nombre_normalizado
    ON categoria (LOWER(nombre));
