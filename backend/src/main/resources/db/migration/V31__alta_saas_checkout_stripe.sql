ALTER TABLE suscripcion_organizacion
    ADD COLUMN stripe_customer_id VARCHAR(255),
    ADD COLUMN stripe_subscription_id VARCHAR(255),
    ADD COLUMN ciclo_facturacion VARCHAR(16) NOT NULL DEFAULT 'MENSUAL',
    ADD COLUMN moneda VARCHAR(3) NOT NULL DEFAULT 'BOB',
    ADD COLUMN importe_mensual_minor BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN stripe_ultimo_evento_en TIMESTAMPTZ;

CREATE UNIQUE INDEX uk_suscripcion_stripe_subscription
    ON suscripcion_organizacion(stripe_subscription_id)
    WHERE stripe_subscription_id IS NOT NULL;

CREATE TABLE solicitud_alta_saas (
    id UUID PRIMARY KEY,
    nombre_empresa VARCHAR(150) NOT NULL,
    slug_sugerido VARCHAR(100),
    nombres_contacto VARCHAR(100) NOT NULL,
    apellidos_contacto VARCHAR(100) NOT NULL,
    correo_contacto VARCHAR(254) NOT NULL,
    plan VARCHAR(24) NOT NULL,
    estado VARCHAR(32) NOT NULL,
    token_verificacion_hash VARCHAR(64) NOT NULL UNIQUE,
    token_verificacion_expira TIMESTAMPTZ NOT NULL,
    fecha_email_verificado TIMESTAMPTZ,
    stripe_checkout_session_id VARCHAR(255) UNIQUE,
    stripe_checkout_url TEXT,
    stripe_customer_id VARCHAR(255),
    stripe_subscription_id VARCHAR(255),
    fecha_pago_confirmado TIMESTAMPTZ,
    id_organizacion BIGINT REFERENCES organizacion(id_organizacion),
    id_usuario_propietario BIGINT REFERENCES usuario(id_usuario),
    token_activacion_hash VARCHAR(64) UNIQUE,
    token_activacion_expira TIMESTAMPTZ,
    fecha_activacion TIMESTAMPTZ,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_solicitud_alta_plan CHECK (plan IN ('BASICO', 'PROFESIONAL', 'EMPRESARIAL')),
    CONSTRAINT ck_solicitud_alta_estado CHECK (estado IN (
        'EMAIL_PENDIENTE', 'CHECKOUT_PENDIENTE', 'ACTIVACION_PENDIENTE',
        'ACTIVA', 'PAGO_FALLIDO', 'CANCELADA', 'EXPIRADA'
    ))
);

CREATE INDEX idx_solicitud_alta_estado_fecha ON solicitud_alta_saas(estado, fecha_creacion DESC);
CREATE INDEX idx_solicitud_alta_correo_fecha ON solicitud_alta_saas(correo_contacto, fecha_creacion DESC);
CREATE UNIQUE INDEX uk_solicitud_alta_correo_en_curso
    ON solicitud_alta_saas (LOWER(correo_contacto))
    WHERE estado IN ('EMAIL_PENDIENTE', 'CHECKOUT_PENDIENTE', 'ACTIVACION_PENDIENTE');

CREATE TABLE stripe_webhook_evento (
    stripe_event_id VARCHAR(255) PRIMARY KEY,
    tipo VARCHAR(120) NOT NULL,
    fecha_recibido TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
