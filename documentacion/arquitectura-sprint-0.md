# Arquitectura Tecnica - Sprint 0

## Alcance

Sprint 0 implementa exclusivamente la gestion de identidad y acceso:

- Registro, inicio y cierre de sesion.
- Renovacion de sesion y recuperacion de clave.
- Perfil propio.
- Administracion de usuarios.
- Roles y permisos.

Eventos, inscripciones, QR, asistencia, notificaciones, reportes e inteligencia artificial quedan pendientes para los Sprint 1 a 4.

## Estilo Arquitectonico

La plataforma utiliza una arquitectura cliente-servidor con un backend Spring Boot como fuente de verdad. El backend sera un monolito modular: cada modulo contiene sus responsabilidades de dominio y no se conecta directamente con las interfaces de otros modulos salvo mediante sus servicios publicos.

```text
Angular Web -------------------+
                                |
Flutter ------------------------+--> API REST Spring Boot --> PostgreSQL
                                |
FastAPI (futuro) <--------------+
```

Angular y Flutter nunca se conectan directamente a PostgreSQL. FastAPI no participa en la logica de Sprint 0; se preparara posteriormente con un endpoint de estado.

## Proyectos

```text
plataforma-gestion-eventos/
├── backend/                  # API REST y logica de negocio
├── frontend-web/             # Angular para administracion
├── aplicacion-movil/         # Flutter para participante y staff
├── servicio-ia/              # FastAPI, sin recomendaciones en Sprint 0
├── base-datos/               # Recursos auxiliares de datos
├── docker/                   # Recursos de contenedores
├── documentacion/            # Arquitectura, Postman y evidencias
├── .env.example
├── .gitignore
├── docker-compose.yml
└── README.md
```

Los directorios se crearan en las fases que los implementan. No se crearan tablas ni modulos funcionales de Sprint futuros.

## Backend

Paquete raiz propuesto: `com.plataformaeventos`.

```text
com.plataformaeventos/
├── configuracion/            # Propiedades, CORS y OpenAPI
├── seguridad/                # JWT, filtro y configuracion Security
├── autenticacion/            # Registro, sesion y recuperacion de clave
├── usuarios/                 # Administracion de usuarios
├── perfil/                   # Perfil del usuario autenticado
├── roles/                    # Gestion de roles
├── permisos/                 # Consulta y asignacion de permisos
└── excepciones/              # Respuestas de error centralizadas
```

La comunicacion HTTP se realiza mediante DTO. Las entidades JPA no se exponen como respuestas REST. Los controladores delegan en servicios; los repositorios solo acceden a datos.

## Clientes

Angular se organizara por funcionalidad y contara con un interceptor que agregue el JWT de acceso, un guard de autenticacion y un guard de permisos. Flutter centralizara las solicitudes REST y almacenara tokens mediante almacenamiento seguro.

Ambos clientes consumiran la misma API y no tendran privilegios propios: toda autorizacion se valida nuevamente en Spring Boot.

## Autenticacion y Autorizacion

- Claves de usuarios: BCrypt; nunca texto plano.
- Access token: JWT firmado con `JWT_SECRET`, con vigencia corta.
- Refresh token: valor aleatorio de alta entropia, persistido solo como hash y revocable.
- Renovacion: rotacion del refresh token al emitir una nueva sesion.
- Cierre de sesion: revocacion persistida del refresh token.
- Registro publico: asignacion exclusiva del rol `PARTICIPANTE` en el backend.
- Autorizacion: permisos asociados a roles mediante `usuario_rol` y `rol_permiso`.
- Usuario inactivo: no puede autenticarse ni operar en endpoints protegidos.

Los access token transportan la identidad del usuario. El backend resuelve sus autoridades actuales en cada solicitud autenticada para que cambios de roles o permisos surtan efecto sin esperar la expiracion del JWT.

## Dependencias Previstas

### Spring Boot

- Spring Web.
- Spring Data JPA.
- Spring Security.
- Bean Validation.
- Flyway PostgreSQL.
- Driver PostgreSQL.
- Biblioteca JWT.
- Springdoc OpenAPI.
- Spring Mail.
- Spring Boot Test y Spring Security Test.

### Angular

- Angular Router.
- Reactive Forms.
- HttpClient.

### Flutter

- Cliente HTTP.
- Almacenamiento seguro de tokens.

### FastAPI

- FastAPI.
- Uvicorn.

## Configuracion Externa

Ninguna credencial se versiona. El archivo `.env.example` documentara las siguientes variables cuando se cree la infraestructura:

```text
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
POSTGRES_HOST
POSTGRES_PORT
JWT_SECRET
JWT_ACCESS_EXPIRACION_MINUTOS
JWT_REFRESH_EXPIRACION_DIAS
ADMIN_CORREO
ADMIN_CLAVE
SMTP_HOST
SMTP_PORT
SMTP_USER
SMTP_PASSWORD
SMTP_FROM
CORS_ORIGENES
URL_FRONTEND_WEB
URL_APLICACION_MOVIL
```

## Integracion y Operacion

Docker Compose se implementara en la Fase 15. Contendra PostgreSQL, Spring Boot, Angular y FastAPI. Flutter se ejecutara localmente. La configuracion CORS aceptara solamente los origenes definidos en `CORS_ORIGENES`.

Swagger/OpenAPI documentara la API Spring Boot. La coleccion Postman se organizara en Autenticacion, Perfil, Usuarios, Roles y Permisos.

## Decisiones Diferidas

- Algoritmos y comunicacion funcional con FastAPI: PENDIENTE PARA SPRINT 4.
- Entidades de eventos, categorias, espacios, actividades y recursos: PENDIENTE PARA SPRINT 1.
- Inscripciones y gestion de staff: PENDIENTE PARA SPRINT 2.
- Asistencia y notificaciones: PENDIENTE PARA SPRINT 3.
- Certificados, bitacora e inteligencia artificial: PENDIENTE PARA SPRINT 4.
