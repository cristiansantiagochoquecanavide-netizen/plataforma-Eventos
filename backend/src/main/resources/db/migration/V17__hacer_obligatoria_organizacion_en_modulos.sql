ALTER TABLE evento ALTER COLUMN id_organizacion SET NOT NULL;
ALTER TABLE espacio ALTER COLUMN id_organizacion SET NOT NULL;
ALTER TABLE recurso ALTER COLUMN id_organizacion SET NOT NULL;
ALTER TABLE inscripcion ALTER COLUMN id_organizacion SET NOT NULL;
ALTER TABLE convocatoria_personal ALTER COLUMN id_organizacion SET NOT NULL;
