-- Phase 3 audit encryption envelope. Existing plaintext rows remain for explicit later backfill.
ALTER TABLE bitacora ADD COLUMN IF NOT EXISTS payload_ciphertext TEXT;
ALTER TABLE bitacora ADD COLUMN IF NOT EXISTS payload_dek_cifrada TEXT;
ALTER TABLE bitacora ADD COLUMN IF NOT EXISTS payload_nonce VARCHAR(64);
ALTER TABLE bitacora ADD COLUMN IF NOT EXISTS crypto_version INTEGER;
ALTER TABLE bitacora ADD COLUMN IF NOT EXISTS idx_actor VARCHAR(64);
ALTER TABLE bitacora ADD COLUMN IF NOT EXISTS idx_ip VARCHAR(64);
ALTER TABLE bitacora ADD COLUMN IF NOT EXISTS idx_accion VARCHAR(64);
ALTER TABLE bitacora ADD COLUMN IF NOT EXISTS idx_modulo VARCHAR(64);
ALTER TABLE bitacora ADD COLUMN IF NOT EXISTS idx_entidad VARCHAR(64);
ALTER TABLE bitacora ADD COLUMN IF NOT EXISTS idx_id_entidad VARCHAR(64);
ALTER TABLE bitacora ADD COLUMN IF NOT EXISTS idx_exito VARCHAR(64);

-- New encrypted rows keep only id_bitacora, id_organizacion and fecha as required plaintext routing/order metadata.
ALTER TABLE bitacora ALTER COLUMN id_actor DROP NOT NULL;
ALTER TABLE bitacora ALTER COLUMN accion DROP NOT NULL;
ALTER TABLE bitacora ALTER COLUMN modulo DROP NOT NULL;
ALTER TABLE bitacora ALTER COLUMN entidad DROP NOT NULL;
ALTER TABLE bitacora ALTER COLUMN exito DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_bitacora_org_fecha_v27 ON bitacora(id_organizacion, fecha DESC);
CREATE INDEX IF NOT EXISTS idx_bitacora_org_idx_modulo_fecha ON bitacora(id_organizacion, idx_modulo, fecha DESC);
CREATE INDEX IF NOT EXISTS idx_bitacora_org_idx_entidad_fecha ON bitacora(id_organizacion, idx_entidad, fecha DESC);
CREATE INDEX IF NOT EXISTS idx_bitacora_org_idx_accion_fecha ON bitacora(id_organizacion, idx_accion, fecha DESC);
CREATE INDEX IF NOT EXISTS idx_bitacora_org_idx_exito_fecha ON bitacora(id_organizacion, idx_exito, fecha DESC);
CREATE INDEX IF NOT EXISTS idx_bitacora_org_idx_actor_fecha ON bitacora(id_organizacion, idx_actor, fecha DESC);
