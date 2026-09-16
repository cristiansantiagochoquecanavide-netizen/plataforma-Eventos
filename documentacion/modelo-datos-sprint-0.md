# Modelo de Datos - Sprint 0

## Alcance

Este modelo implementa unicamente identidad, autenticacion y autorizacion. No incorpora tablas de eventos, actividades, inscripciones, QR, asistencia, recursos, notificaciones, certificados, bitacora ni inteligencia artificial.

## Diagrama Relacional

```text
usuario 1 ----- N token_recuperacion
usuario 1 ----- N token_refresco
usuario N ----- M rol
rol     N ----- M permiso

usuario ---< usuario_rol >--- rol ---< rol_permiso >--- permiso
```

La relacion directa `usuario.id_rol` no existe. Un usuario puede tener varios roles y cada rol puede asignarse a varios usuarios.

## Tablas

### usuario

| Columna | Tipo PostgreSQL | Regla |
|---|---|---|
| id_usuario | BIGINT | PK, identidad |
| nombres | VARCHAR(100) | NOT NULL |
| apellidos | VARCHAR(100) | NOT NULL |
| correo | VARCHAR(254) | NOT NULL, UNIQUE |
| clave_hash | VARCHAR(100) | NOT NULL; hash BCrypt |
| estado | VARCHAR(10) | NOT NULL; ACTIVO o INACTIVO |
| fecha_creacion | TIMESTAMP WITH TIME ZONE | NOT NULL, valor por defecto actual |
| fecha_actualizacion | TIMESTAMP WITH TIME ZONE | NOT NULL, valor por defecto actual |

Restricciones:

- `estado IN ('ACTIVO', 'INACTIVO')`.
- El correo se normaliza a minusculas en el servicio antes de persistirlo.
- La longitud de `clave_hash` permite hashes BCrypt actuales.

### rol

| Columna | Tipo PostgreSQL | Regla |
|---|---|---|
| id_rol | BIGINT | PK, identidad |
| nombre | VARCHAR(50) | NOT NULL, UNIQUE |
| descripcion | VARCHAR(255) | NULL |
| estado | VARCHAR(10) | NOT NULL |

Restriccion: `estado IN ('ACTIVO', 'INACTIVO')`.

Roles iniciales: `ADMINISTRADOR`, `ORGANIZADOR`, `STAFF` y `PARTICIPANTE`.

### permiso

| Columna | Tipo PostgreSQL | Regla |
|---|---|---|
| id_permiso | BIGINT | PK, identidad |
| nombre | VARCHAR(100) | NOT NULL, UNIQUE |
| descripcion | VARCHAR(255) | NULL |

Permisos iniciales: `PERFIL_VER`, `PERFIL_EDITAR`, `USUARIO_VER`, `USUARIO_CREAR`, `USUARIO_EDITAR`, `USUARIO_CAMBIAR_ESTADO`, `ROL_VER`, `ROL_CREAR`, `ROL_EDITAR`, `PERMISO_VER` y `PERMISO_ASIGNAR`.

### usuario_rol

| Columna | Tipo PostgreSQL | Regla |
|---|---|---|
| id_usuario | BIGINT | PK, FK a usuario.id_usuario |
| id_rol | BIGINT | PK, FK a rol.id_rol |

La PK compuesta `(id_usuario, id_rol)` evita asignaciones duplicadas y satisface la restriccion `UNIQUE(id_usuario, id_rol)`.

### rol_permiso

| Columna | Tipo PostgreSQL | Regla |
|---|---|---|
| id_rol | BIGINT | PK, FK a rol.id_rol |
| id_permiso | BIGINT | PK, FK a permiso.id_permiso |

La PK compuesta `(id_rol, id_permiso)` evita asignaciones duplicadas y satisface la restriccion `UNIQUE(id_rol, id_permiso)`.

### token_recuperacion

| Columna | Tipo PostgreSQL | Regla |
|---|---|---|
| id_token | BIGINT | PK, identidad |
| id_usuario | BIGINT | NOT NULL, FK a usuario.id_usuario |
| token_hash | VARCHAR(64) | NOT NULL, UNIQUE; SHA-256 hexadecimal |
| fecha_creacion | TIMESTAMP WITH TIME ZONE | NOT NULL, valor por defecto actual |
| fecha_expiracion | TIMESTAMP WITH TIME ZONE | NOT NULL |
| utilizado | BOOLEAN | NOT NULL, valor por defecto FALSE |
| fecha_uso | TIMESTAMP WITH TIME ZONE | NULL |

Restricciones:

- `fecha_expiracion > fecha_creacion`.
- Si `utilizado` es verdadero, `fecha_uso` debe tener valor; si es falso, `fecha_uso` debe ser nulo.
- El token original no se persiste ni se devuelve en consultas administrativas.

### token_refresco

| Columna | Tipo PostgreSQL | Regla |
|---|---|---|
| id_token | BIGINT | PK, identidad |
| id_usuario | BIGINT | NOT NULL, FK a usuario.id_usuario |
| token_hash | VARCHAR(64) | NOT NULL, UNIQUE; SHA-256 hexadecimal |
| fecha_creacion | TIMESTAMP WITH TIME ZONE | NOT NULL, valor por defecto actual |
| fecha_expiracion | TIMESTAMP WITH TIME ZONE | NOT NULL |
| revocado | BOOLEAN | NOT NULL, valor por defecto FALSE |
| fecha_revocacion | TIMESTAMP WITH TIME ZONE | NULL |

Restricciones:

- `fecha_expiracion > fecha_creacion`.
- Si `revocado` es verdadero, `fecha_revocacion` debe tener valor; si es falso, `fecha_revocacion` debe ser nulo.

## Llaves Foraneas y Cardinalidades

| Tabla hija | FK | Tabla padre | Cardinalidad | Eliminacion |
|---|---|---|---|---|
| usuario_rol | id_usuario | usuario | N:1 | RESTRICT |
| usuario_rol | id_rol | rol | N:1 | RESTRICT |
| rol_permiso | id_rol | rol | N:1 | RESTRICT |
| rol_permiso | id_permiso | permiso | N:1 | RESTRICT |
| token_recuperacion | id_usuario | usuario | N:1 | RESTRICT |
| token_refresco | id_usuario | usuario | N:1 | RESTRICT |

Sprint 0 no realiza eliminacion fisica de usuarios. La administracion utiliza el estado `INACTIVO`, por lo que `RESTRICT` protege el historial de tokens y las asignaciones de seguridad.

## Indices

Las restricciones `UNIQUE` y PK generan sus propios indices. Se agregaran los siguientes indices no redundantes:

| Indice | Tabla | Columnas | Uso |
|---|---|---|---|
| idx_usuario_rol_rol | usuario_rol | id_rol | Consultar usuarios por rol |
| idx_rol_permiso_permiso | rol_permiso | id_permiso | Consultar roles por permiso |
| idx_token_recuperacion_usuario_estado | token_recuperacion | id_usuario, utilizado, fecha_expiracion | Validar y limpiar recuperaciones |
| idx_token_recuperacion_expiracion | token_recuperacion | fecha_expiracion | Limpieza de tokens vencidos |
| idx_token_refresco_usuario_estado | token_refresco | id_usuario, revocado, fecha_expiracion | Gestionar sesiones activas |
| idx_token_refresco_expiracion | token_refresco | fecha_expiracion | Limpieza de tokens vencidos |

## Datos Iniciales y Administrador

Flyway insertara los cuatro roles y los once permisos base. La asignacion inicial de permisos para `ADMINISTRADOR` tambien quedara versionada por Flyway.

El usuario administrador inicial no se crea mediante SQL: un inicializador Spring Boot leera `ADMIN_CORREO` y `ADMIN_CLAVE`, aplicara BCrypt a la clave y asignara el rol `ADMINISTRADOR` solo si el usuario aun no existe.

## Evolucion por Sprint

| Sprint | Tablas futuras |
|---|---|
| Sprint 1 | categoria, evento, espacio, actividad, recurso, evento_recurso, actividad_recurso, convocatoria_staff |
| Sprint 2 | inscripcion, postulacion_staff, asignacion_staff |
| Sprint 3 | asistencia, notificacion |
| Sprint 4 | preferencia_usuario, consulta_evento, certificado, bitacora |

Estas tablas no seran creadas ni referenciadas por las migraciones de Sprint 0.

La migracion V16 de CU17 implementa `asignacion_staff` y la relacion unica con `postulacion_staff`. Cada asignacion hereda el evento y el tipo de personal solicitado desde la convocatoria asociada; el tipo no se duplica en la asignacion.
