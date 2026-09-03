ALTER TABLE evento ADD COLUMN categoria VARCHAR(80);

UPDATE evento SET categoria = 'GENERAL' WHERE categoria IS NULL;

ALTER TABLE evento ALTER COLUMN categoria SET NOT NULL;

CREATE INDEX idx_evento_categoria ON evento (categoria);
