CREATE TABLE usuario (
    id_usuario BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    correo VARCHAR(254) NOT NULL UNIQUE,
    clave_hash VARCHAR(100) NOT NULL,
    estado VARCHAR(10) NOT NULL,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_usuario_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE TABLE rol (
    id_rol BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    estado VARCHAR(10) NOT NULL,
    CONSTRAINT ck_rol_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE TABLE permiso (
    id_permiso BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
);

CREATE TABLE usuario_rol (
    id_usuario BIGINT NOT NULL,
    id_rol BIGINT NOT NULL,
    CONSTRAINT pk_usuario_rol PRIMARY KEY (id_usuario, id_rol),
    CONSTRAINT fk_usuario_rol_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
    CONSTRAINT fk_usuario_rol_rol FOREIGN KEY (id_rol)
        REFERENCES rol (id_rol) ON DELETE RESTRICT
);

CREATE TABLE rol_permiso (
    id_rol BIGINT NOT NULL,
    id_permiso BIGINT NOT NULL,
    CONSTRAINT pk_rol_permiso PRIMARY KEY (id_rol, id_permiso),
    CONSTRAINT fk_rol_permiso_rol FOREIGN KEY (id_rol)
        REFERENCES rol (id_rol) ON DELETE RESTRICT,
    CONSTRAINT fk_rol_permiso_permiso FOREIGN KEY (id_permiso)
        REFERENCES permiso (id_permiso) ON DELETE RESTRICT
);

CREATE TABLE token_recuperacion (
    id_token BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMP WITH TIME ZONE NOT NULL,
    utilizado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_uso TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_token_recuperacion_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
    CONSTRAINT ck_token_recuperacion_fechas
        CHECK (fecha_expiracion > fecha_creacion),
    CONSTRAINT ck_token_recuperacion_uso
        CHECK ((utilizado AND fecha_uso IS NOT NULL) OR (NOT utilizado AND fecha_uso IS NULL))
);

CREATE TABLE token_refresco (
    id_token BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMP WITH TIME ZONE NOT NULL,
    revocado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_revocacion TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_token_refresco_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
    CONSTRAINT ck_token_refresco_fechas
        CHECK (fecha_expiracion > fecha_creacion),
    CONSTRAINT ck_token_refresco_revocacion
        CHECK ((revocado AND fecha_revocacion IS NOT NULL) OR (NOT revocado AND fecha_revocacion IS NULL))
);

CREATE INDEX idx_usuario_rol_rol ON usuario_rol (id_rol);
CREATE INDEX idx_rol_permiso_permiso ON rol_permiso (id_permiso);
CREATE INDEX idx_token_recuperacion_usuario_estado
    ON token_recuperacion (id_usuario, utilizado, fecha_expiracion);
CREATE INDEX idx_token_recuperacion_expiracion
    ON token_recuperacion (fecha_expiracion);
CREATE INDEX idx_token_refresco_usuario_estado
    ON token_refresco (id_usuario, revocado, fecha_expiracion);
CREATE INDEX idx_token_refresco_expiracion
    ON token_refresco (fecha_expiracion);
