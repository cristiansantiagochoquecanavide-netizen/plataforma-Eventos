-- Indices adicionales para filtros de auditoria. No modifica ni elimina datos.
UPDATE bitacora SET entidad = modulo WHERE entidad IS NULL;
ALTER TABLE bitacora ALTER COLUMN entidad SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_bitacora_org_modulo_fecha ON bitacora(id_organizacion, modulo, fecha DESC);
CREATE INDEX IF NOT EXISTS idx_bitacora_org_entidad_fecha ON bitacora(id_organizacion, entidad, fecha DESC);
CREATE INDEX IF NOT EXISTS idx_bitacora_org_accion_fecha ON bitacora(id_organizacion, accion, fecha DESC);
CREATE INDEX IF NOT EXISTS idx_bitacora_org_exito_fecha ON bitacora(id_organizacion, exito, fecha DESC);
CREATE INDEX IF NOT EXISTS idx_bitacora_org_actor_fecha ON bitacora(id_organizacion, id_actor, fecha DESC);
