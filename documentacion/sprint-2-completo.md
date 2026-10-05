# Sprint 2

## 1. Sprint Planning

El Sprint 2 ampliará la plataforma con soporte SaaS multi-tenant y con el flujo operativo de participantes, inscripciones y personal de apoyo. La organización cliente y el aislamiento de datos serán una condición previa para las nuevas funcionalidades.

### Casos de uso seleccionados

| ID | Caso de uso | Actor principal | Plataforma |
|---|---|---|---|
| CU10 | Gestionar participantes | Administrador/Organizador | Web |
| CU11 | Inscribirse a un evento | Participante | Web/Móvil |
| CU12 | Consultar estado de inscripción | Participante/Organizador | Web/Móvil |
| CU13 | Cancelar inscripción | Participante | Web/Móvil |
| CU14 | Controlar cupos y aforo | Sistema/Organizador | Web/Móvil |
| CU16 | Postularse a una convocatoria | Participante/Staff/Voluntario | Web/Móvil |
| CU17 | Consultar, seleccionar y rechazar postulantes | Administrador/Organizador | Web |
| CU18 | Asignar turnos y tareas al Staff | Administrador/Organizador | Web |
| CU31 | Consultar bitácora y auditoría | Administrador/Auditor | Web |
| CU38 | Administrar organizaciones cliente | Administrador SaaS | Web |
| CU40 | Gestionar pertenencia de usuarios a una organización | Administrador SaaS | Web |

CU15 se considera una dependencia de Sprint 1: las convocatorias ya existen y Sprint 2 trabaja sobre ellas.

### 1.1. Objetivo del Sprint

Implementar una base SaaS multi-tenant que permita administrar organizaciones cliente, asociar usuarios a una organización, proteger el acceso mediante el contexto del tenant y aislar la información. Sobre esa base se desarrollarán las inscripciones, el control de cupos, las postulaciones y las asignaciones de Staff, incorporando bitácora mínima y pruebas de aislamiento.

### 1.2. Historias de usuario

#### HU2-01: Crear una organización cliente

| Campo | Descripción |
|---|---|
| ID | HU2-01 |
| CU | CU38 |
| Como | Administrador SaaS |
| Quiero | Crear y activar una organización cliente |
| Para | Permitir que la plataforma sea utilizada por diferentes organizaciones |
| Prioridad | Alta |
| Estimación | 5 PHU |
| Criterios de aceptación | La organización debe tener nombre y identificador único; debe iniciar activa; no se deben permitir duplicados; solo el Administrador SaaS puede crearla. |

#### HU2-02: Asociar usuarios a una organización

| Campo | Descripción |
|---|---|
| ID | HU2-02 |
| CU | CU40 |
| Como | Administrador SaaS |
| Quiero | Asociar usuarios a una organización con un rol |
| Para | Controlar quién puede operar dentro de cada tenant |
| Prioridad | Alta |
| Estimación | 5 PHU |
| Criterios de aceptación | Un usuario puede pertenecer a una o más organizaciones; la pertenencia debe tener estado y rol; no se deben duplicar membresías; un usuario no puede operar en una organización sin membresía activa. |

#### HU2-03: Aislar información por organización

| Campo | Descripción |
|---|---|
| ID | HU2-03 |
| CU | CU38/CU40 |
| Como | Sistema |
| Quiero | Resolver una organización activa en cada solicitud |
| Para | Impedir que un usuario consulte o modifique datos de otro tenant |
| Prioridad | Alta |
| Estimación | 8 PHU |
| Criterios de aceptación | Toda consulta de eventos, recursos, convocatorias, inscripciones y bitácora debe filtrarse por organización; una organización no puede acceder a registros de otra; las pruebas negativas deben devolver 403 o 404 según la política definida. |

#### HU2-04: Gestionar participantes

| Campo | Descripción |
|---|---|
| ID | HU2-04 |
| CU | CU10 |
| Como | Administrador u Organizador |
| Quiero | Consultar participantes asociados a mi organización |
| Para | Supervisar la participación en los eventos |
| Prioridad | Alta |
| Estimación | 5 PHU |
| Criterios de aceptación | Solo se muestran participantes con relación válida con la organización; la consulta debe permitir búsqueda y paginación; los datos privados deben estar protegidos. |

#### HU2-05: Inscribirse a un evento

| Campo | Descripción |
|---|---|
| ID | HU2-05 |
| CU | CU11 |
| Como | Participante |
| Quiero | Inscribirme a un evento de mi organización |
| Para | Confirmar mi participación |
| Prioridad | Alta |
| Estimación | 8 PHU |
| Criterios de aceptación | El evento debe estar publicado y pertenecer al tenant activo; debe existir cupo; no se permite inscripción duplicada; la inscripción queda registrada con su estado; la operación se audita. |

#### HU2-06: Consultar inscripción

| Campo | Descripción |
|---|---|
| ID | HU2-06 |
| CU | CU12 |
| Como | Participante u Organizador |
| Quiero | Consultar el estado de una inscripción |
| Para | Conocer si la participación está confirmada o cancelada |
| Prioridad | Alta |
| Estimación | 3 PHU |
| Criterios de aceptación | El participante solo consulta sus inscripciones; el organizador solo consulta inscripciones de su organización; se muestra evento, fecha, estado y fecha de inscripción. |

#### HU2-07: Cancelar inscripción

| Campo | Descripción |
|---|---|
| ID | HU2-07 |
| CU | CU13 |
| Como | Participante |
| Quiero | Cancelar una inscripción activa |
| Para | Liberar mi cupo cuando no asistiré |
| Prioridad | Alta |
| Estimación | 5 PHU |
| Criterios de aceptación | Solo se puede cancelar una inscripción propia y activa; el cupo se libera una sola vez; la operación queda auditada; una inscripción cancelada no se puede cancelar nuevamente. |

#### HU2-08: Controlar cupos

| Campo | Descripción |
|---|---|
| ID | HU2-08 |
| CU | CU14 |
| Como | Sistema |
| Quiero | Validar cupos durante la inscripción |
| Para | Evitar sobrepasar el aforo del evento |
| Prioridad | Alta |
| Estimación | 8 PHU |
| Criterios de aceptación | El conteo debe considerar únicamente inscripciones activas; dos solicitudes concurrentes no pueden superar el cupo; al agotarse el cupo se rechaza la inscripción con un mensaje claro. |

#### HU2-09: Postularse a una convocatoria

| Campo | Descripción |
|---|---|
| ID | HU2-09 |
| CU | CU16 |
| Como | Participante, Staff o Voluntario |
| Quiero | Postularme a una convocatoria abierta |
| Para | Ser considerado para apoyar un evento |
| Prioridad | Alta |
| Estimación | 5 PHU |
| Criterios de aceptación | La convocatoria debe pertenecer al tenant activo y estar abierta; la fecha límite no debe vencer; no se permite postulación duplicada; el estado inicial es PENDIENTE. |

#### HU2-10: Seleccionar postulantes

| Campo | Descripción |
|---|---|
| ID | HU2-10 |
| CU | CU17 |
| Como | Administrador u Organizador |
| Quiero | Aprobar o rechazar postulaciones |
| Para | Seleccionar personal para una convocatoria |
| Prioridad | Alta |
| Estimación | 8 PHU |
| Criterios de aceptación | Solo usuarios autorizados pueden decidir; una convocatoria cerrada no acepta cambios; se debe respetar la cantidad requerida; la decisión queda auditada. |

#### HU2-11: Asignar Staff

| Campo | Descripción |
|---|---|
| ID | HU2-11 |
| CU | CU18 |
| Como | Administrador u Organizador |
| Quiero | Asignar personas aprobadas a turnos y tareas |
| Para | Organizar el trabajo operativo del evento |
| Prioridad | Alta |
| Estimación | 8 PHU |
| Criterios de aceptación | Solo se asignan postulaciones aprobadas; el turno pertenece al evento correcto; no se permiten conflictos incompatibles; el Staff puede consultar sus asignaciones; la operación queda auditada. |

#### HU2-12: Consultar bitácora

| Campo | Descripción |
|---|---|
| ID | HU2-12 |
| CU | CU31 |
| Como | Administrador o Auditor |
| Quiero | Consultar operaciones de mi organización |
| Para | Mantener trazabilidad del sistema |
| Prioridad | Alta |
| Estimación | 5 PHU |
| Criterios de aceptación | Cada registro contiene organización, usuario, acción, módulo, fecha, IP y resultado; la consulta permite filtrar; los usuarios sin permiso no pueden acceder. |

### 1.3. Contexto del Sistema

Sprint 2 agrega la dimensión de organización a la arquitectura existente. Angular, Flutter y Spring Boot seguirán utilizando una API común. El backend resolverá el tenant a partir de la sesión autenticada y de la membresía activa del usuario. Ningún cliente podrá seleccionar libremente una organización para acceder a sus datos.

### 1.3.1 Separación de administración SaaS y tenant

La administración global de roles y permisos queda reservada al rol global `SAAS_ADMINISTRADOR`. Ese rol se asigna al usuario configurado por `ADMIN_CORREO` y se evalúa como authority global. El rol `ADMINISTRADOR` queda disponible como rol de membresía de una organización para administrar operaciones del tenant activo, sin acceso a `/roles` ni `/permisos`.

El contrato de usuario actual separa ambos planos: `roles`/`permisos` representan el perfil global y `perfilOrganizacionActiva` representa exclusivamente la membresia activa enviada con `X-Organization-Id`. Angular refresca ese endpoint al cambiar de organizacion para recalcular menu y guards con permisos tenant vigentes.

Los flujos de membresía rechazan `SAAS_ADMINISTRADOR`; solo pueden asignar roles tenant como `ADMINISTRADOR`, `ORGANIZADOR`, `STAFF` o `PARTICIPANTE`. Para demos locales, `admin@eventos.local` representa al administrador SaaS y los organizadores demo representan administradores tenant.

El flujo funcional será:

```text
Administrador SaaS crea organización
        ↓
Usuario obtiene membresía y rol
        ↓
Usuario inicia sesión y selecciona tenant permitido
        ↓
Backend crea contexto de organización
        ↓
Inscripciones, postulaciones y asignaciones se filtran por tenant
        ↓
Las operaciones relevantes se registran en bitácora
```

### 1.4. Sprint Backlog

| ID | Tarea | Tipo | Estimación | Responsable | Estado |
|---|---|---|---:|---|---|
| SP2-1 | Diseñar interfaz de participantes | Diseño | 3 h | Equipo | Completado |
| SP2-2 | Implementar consulta de participantes | Desarrollo | 6 h | Equipo | Completado |
| SP2-3 | Probar gestión de participantes | Pruebas | 3 h | Equipo | Completado |
| SP2-4 | Diseñar interfaces web y móvil de inscripción | Diseño | 3 h | Equipo | Completado |
| SP2-5 | Implementar inscripción | Desarrollo | 7 h | Equipo | Completado |
| SP2-6 | Implementar consulta de inscripción | Desarrollo | 4 h | Equipo | Completado |
| SP2-7 | Implementar cancelación | Desarrollo | 4 h | Equipo | Completado |
| SP2-8 | Implementar cupos, aforo y control de duplicados | Desarrollo | 6 h | Equipo | Completado |
| SP2-9 | Probar inscripción, cancelación y concurrencia | Pruebas | 5 h | Equipo | Completado |
| SP2-10 | Diseñar interfaz móvil de postulación | Diseño | 3 h | Equipo | Completado |
| SP2-11 | Implementar postulación | Desarrollo | 5 h | Equipo | Completado |
| SP2-12 | Probar postulaciones | Pruebas | 3 h | Equipo | Completado |
| SP2-13 | Diseñar consulta y selección de postulantes | Diseño | 3 h | Equipo | Completado |
| SP2-14 | Implementar selección y rechazo | Desarrollo | 6 h | Equipo | Completado |
| SP2-15 | Probar selección y rechazo | Pruebas | 3 h | Equipo | Completado |
| SP2-16 | Diseñar asignación de turnos y tareas | Diseño | 3 h | Equipo | Completado |
| SP2-17 | Implementar asignaciones de Staff | Desarrollo | 7 h | Equipo | Completado |
| SP2-18 | Probar asignaciones | Pruebas | 3 h | Equipo | Completado |
| SP2-19 | Actualizar RF, RNF, CU, reglas y trazabilidad | Análisis | 4 h | Equipo | Completado |
| SP2-20 | Validar el incremento real de Sprint 1 | Análisis | 3 h | Equipo | Completado |
| SP2-21 | Diseñar organizaciones, membresías y tenant activo | Datos | 6 h | Equipo | Completado |
| SP2-22 | Implementar organizaciones y estado | Desarrollo | 6 h | Equipo | Completado |
| SP2-23 | Implementar membresías usuario-organización | Desarrollo | 6 h | Equipo | Completado |
| SP2-24 | Resolver contexto de tenant en autenticación | Seguridad | 8 h | Equipo | Completado |
| SP2-25 | Aplicar aislamiento en consultas y escrituras | Seguridad | 10 h | Equipo | Completado |
| SP2-26 | Migrar dominios existentes al contexto de organización | Migración | 8 h | Equipo | Completado |
| SP2-27 | Definir permisos de Sprint 2 y permisos SaaS | Seguridad | 4 h | Equipo | Completado |
| SP2-28 | Implementar bitácora con organización y resultado | Desarrollo | 8 h | Equipo | Completado |
| SP2-29 | Implementar consulta y filtros de bitácora | Desarrollo | 5 h | Equipo | Completado |
| SP2-31 | Probar aislamiento entre organizaciones | Pruebas | 8 h | Equipo | Completado |
| SP2-32 | Probar permisos, bitácora e inscripciones | Pruebas | 6 h | Equipo | Completado |
| SP2-33 | Integrar web, móvil, backend y PostgreSQL | Integración | 6 h | Equipo | Completado |
| SP2-34 | Actualizar OpenAPI, Postman y documentación | Documentación | 4 h | Equipo | Completado |
| SP2-35 | Preparar evidencia del Sprint Review | Gestión | 3 h | Equipo | Pendiente |

SP2-30, grupos de usuarios, se mantiene fuera del Sprint 2 y pasa al Sprint 4.

### 1.5. Equipo Scrum

| Rol | Integrante |
|---|---|
| Product Owner | Cervantes Arancibia Roberto Carlos |
| Scrum Master | Espinoza Sanchez Matteo |
| Development Team | Aranibar Pérez Kevin Ariel |
| Development Team | Carvallo Andreu Carlos Rodrigo |
| Development Team | Choque Canavides Cristian Santiago |
| Development Team | Justiniano Coimbra Raúl Rodrigo |

## 2. Proceso por Historia de Usuario

### 2.1. Diseño

#### 2.1.1. Diseño de la Arquitectura

La arquitectura conserva el monolito modular Spring Boot, Angular, Flutter, PostgreSQL y FastAPI. El tenant se resolverá en Spring Boot y se aplicará en la capa de servicio y repositorio.

```text
Angular Web -----------+
                       +--> API Spring Boot --> Contexto Tenant --> PostgreSQL
Flutter ---------------+                              |
                                                      +--> Bitácora
FastAPI (futuro) ------+
```

Reglas arquitectónicas:

1. El frontend no podrá enviar un `organizationId` para saltarse la organización activa.
2. El backend verificará la membresía antes de crear el contexto tenant.
3. Todas las entidades de negocio pertenecerán a una organización.
4. Los repositorios deberán filtrar por organización.
5. La bitácora conservará el tenant incluso cuando el usuario sea eliminado o desactivado.

#### 2.1.2. Diseño de Datos

| Tabla | Propósito |
|---|---|
| `organizacion` | Catálogo de organizaciones cliente. |
| `usuario_organizacion` | Membresía, estado y rol del usuario dentro de una organización. |
| `evento` | Se agrega `id_organizacion`. |
| `espacio` | Se agrega `id_organizacion`. |
| `recurso` | Se agrega `id_organizacion`. |
| `convocatoria_personal` | Se agrega `id_organizacion` o se deriva desde el evento. |
| `inscripcion` | Se valida contra el tenant del evento y participante. |
| `postulacion_staff` | Postulación dentro de la organización de la convocatoria. |
| `asignacion_staff` | Persona aprobada, turno, tarea y evento asignado. |
| `bitacora` | Acción, usuario, organización, módulo, registro, IP, fecha y resultado. |

Modelo conceptual:

```text
organizacion 1 ---- N usuario_organizacion N ---- 1 usuario
organizacion 1 ---- N evento
evento 1 ---------- N inscripcion
evento 1 ---------- N convocatoria_personal
convocatoria 1 --- N postulacion_staff
postulacion 1 ---- 0..1 asignacion_staff
organizacion 1 ---- N bitacora
```

Migraciones previstas:

| Migración | Contenido |
|---|---|
| V14 | Crear `organizacion` y `usuario_organizacion`. |
| V15 | Crear postulaciones y asignaciones de Staff. |
| V16 | Crear `bitacora`. |
| V17 | Hacer obligatoria la organizacion en modulos existentes. |
| V18 | Crear roles por membresia y categorias tenant. |
| V19 | Agregar integridad a postulaciones y asignaciones. |
| V20 | Agregar integridad de usuario en asignaciones Staff. |
| V21 | Optimizar indices de bitacora. |

#### 2.1.3. Diseño de la Lógica de Negocio

| Operación | Regla principal |
|---|---|
| Crear organización | Solo Administrador SaaS; nombre y slug únicos. |
| Activar membresía | El usuario debe existir y el rol debe ser válido. |
| Resolver tenant | La sesión debe contener una organización permitida. |
| Consultar evento | El evento debe pertenecer al tenant activo. |
| Inscribirse | Usuario y evento deben pertenecer al mismo tenant. |
| Postularse | La convocatoria debe estar abierta y pertenecer al tenant. |
| Asignar Staff | Solo postulaciones aprobadas del mismo tenant. |
| Consultar bitácora | Solo registros del tenant permitido, salvo Administrador SaaS. |

#### 2.1.4. Diagrama de Secuencia

```text
Usuario -> API: Solicita eventos
API -> Seguridad: Obtiene usuario autenticado
Seguridad -> Membresía: Verifica organización activa
Membresía -> API: Devuelve tenant autorizado
API -> Repositorio: Busca eventos por tenant
Repositorio -> API: Devuelve únicamente registros aislados
API -> Usuario: Respuesta filtrada
```

#### 2.1.5. Diagrama de Tiempo

```text
Login       |---- sesión válida -------------------------------|
Tenant      |---- membresía activa ----------------------------|
Inscripción |          | validar cupo | guardar | auditar -----|
Postulación |                        | validar | guardar | auditar |
Asignación  |                                      | guardar | auditar |
```

#### 2.1.6. Diagrama de Estado

Inscripción:

```text
CONFIRMADA ---- cancelar ----> CANCELADA
```

Postulación:

```text
PENDIENTE ---- aprobar ----> ACEPTADA
PENDIENTE ---- rechazar ---> RECHAZADA
```

Convocatoria existente:

```text
ABIERTA ---- cerrar ----> CERRADA
ABIERTA ---- cancelar -> CANCELADA
```

Membresía:

```text
PENDIENTE ---- activar ----> ACTIVA
ACTIVA ------- suspender -> INACTIVA
```

#### 2.1.7. Diagrama de Navegación

Web SaaS:

```text
Login
  └── Selector de organización
       ├── Panel de organización
       ├── Participantes
       ├── Inscripciones
       ├── Convocatorias
       ├── Postulaciones
       ├── Asignaciones
       └── Bitácora
```

Móvil:

```text
Login
  └── Eventos de la organización activa
       ├── Detalle de evento
       ├── Inscribirse
       ├── Mis inscripciones
       └── Mis postulaciones/asignaciones
```

### 2.2. Implementación

Componentes esperados:

| Componente | Artefactos |
|---|---|
| Backend | Entidades, repositorios, servicios, DTOs y controladores de organización, membresías, asignaciones y bitácora. |
| Seguridad | Resolución de tenant, validación de membresía y permisos por organización. |
| Base de datos | Migraciones Flyway V14-V21, índices y restricciones tenant. |
| Angular | Selector de organización, pantallas de participantes, postulaciones, asignaciones y bitácora. |
| Flutter | Inscripción, consulta de inscripción y postulación utilizando la organización activa. |
| Documentación | OpenAPI, colección Postman, modelo de datos y matriz de trazabilidad. |

### 2.3. Pruebas

#### 2.3.1. Plan de pruebas

| Código | CU/HU | Prueba | Resultado esperado |
|---|---|---|---|
| PT-S2-01 | CU38/HU2-01 | Crear organización válida | La organización queda creada y activa. |
| PT-S2-02 | CU40/HU2-02 | Asociar usuario a organización | La membresía queda activa con rol válido. |
| PT-S2-03 | HU2-03 | Consultar datos de otra organización | El acceso es bloqueado. |
| PT-S2-04 | CU11/HU2-05 | Inscripción con cupo | La inscripción se confirma y se audita. |
| PT-S2-05 | CU14/HU2-08 | Inscripción sin cupo | La operación se rechaza sin alterar el cupo. |
| PT-S2-06 | CU13/HU2-07 | Cancelar inscripción | El estado cambia y el cupo se libera una vez. |
| PT-S2-07 | CU16/HU2-09 | Postulación duplicada | El sistema rechaza la segunda postulación. |
| PT-S2-08 | CU17/HU2-10 | Aprobar postulante | La postulación cambia a ACEPTADA y se audita. |
| PT-S2-09 | CU18/HU2-11 | Asignar Staff aprobado | Se registra turno/tarea sin conflicto. |
| PT-S2-10 | CU31/HU2-12 | Filtrar bitácora | Solo aparecen registros permitidos del tenant. |
| PT-S2-11 | Seguridad | Usuario sin permiso | La API devuelve 403. |
| PT-S2-12 | Integración | Web y móvil consumen el tenant correcto | Ambos clientes reciben datos aislados. |

#### 2.3.2. Criterios generales de aceptación

- Todas las reglas se validan en backend.
- Las operaciones críticas tienen permisos específicos.
- Los datos de una organización no son visibles desde otra.
- Las inscripciones respetan cupos y duplicados.
- Las postulaciones respetan estado y fecha límite.
- Las asignaciones solo utilizan postulaciones aprobadas.
- Las acciones relevantes se registran en bitácora.
- Las migraciones se ejecutan sobre una base limpia y existente.
- Las pruebas unitarias, integración y aislamiento son satisfactorias.
- OpenAPI, Postman y esta documentación se actualizan.

## 3. Daily Scrum

| Día | Trabajo realizado | Trabajo siguiente | Impedimentos |
|---|---|---|---|
| Daily 1 | Revisar modelo SaaS y alcance del Sprint | Validar entidades y membresías | Por registrar |
| Daily 2 | Implementar organizaciones y membresías | Implementar contexto tenant | Por registrar |
| Daily 3 | Implementar aislamiento en dominios | Integrar inscripciones | Por registrar |
| Daily 4 | Implementar postulaciones y asignaciones | Completar bitácora | Por registrar |
| Daily 5 | Ejecutar pruebas de aislamiento | Corregir incidencias | Por registrar |
| Daily 6 | Integrar clientes web y móvil | Preparar demostración | Por registrar |
| Daily 7 | Validar criterios de aceptación | Preparar Sprint Review | Por registrar |

## 4. Sprint Review

La demostración debe cubrir:

1. Creación de dos organizaciones.
2. Asociación de usuarios a cada organización.
3. Creación o consulta de eventos aislados por organización.
4. Inscripción de un participante.
5. Control de cupos y cancelación.
6. Postulación a una convocatoria.
7. Selección y asignación de Staff.
8. Consulta de bitácora.
9. Intento de acceso cruzado rechazado.

| Elemento | Resultado |
|---|---|
| Objetivo | Cumplido cuando existe aislamiento multi-tenant y flujo operativo de Sprint 2. |
| Evidencias | Capturas, pruebas Postman, pruebas automatizadas, migraciones y demo web/móvil. |
| Validación | Product Owner y equipo Scrum. |

## 5. Sprint Retrospective

| Tema | Registro |
|---|---|
| Qué salió bien | Por completar durante la retrospectiva. |
| Qué no salió bien | Por completar durante la retrospectiva. |
| Qué se mejorará | Por completar durante la retrospectiva. |
| Acciones para el siguiente Sprint | Por completar durante la retrospectiva. |

## 6. Burn Down y Burn Up

Registrar diariamente las horas o puntos completados de `SP2-1` a `SP2-35`, diferenciando tareas de SaaS, funcionales, pruebas y documentación.

## 7. Gráfica de Esfuerzo

Comparar el esfuerzo estimado y real de:

| Área | Estimación inicial |
|---|---:|
| Multi-tenancy y seguridad | 48 h |
| Inscripciones y cupos | 38 h |
| Postulaciones y asignaciones | 36 h |
| Bitácora | 13 h |
| Integración, pruebas y documentación | 27 h |

Las horas son una estimación inicial y deben ajustarse durante Sprint Planning.

## 8. Scrum TaskBoard

| To Do | Doing | Done |
|---|---|---|
| Tareas pendientes del Sprint Backlog | Tareas actualmente en desarrollo | Tareas aceptadas por el equipo y Product Owner |

Una tarea solo podrá pasar a `Done` cuando cumpla los criterios de aceptación, tenga pruebas asociadas y se encuentre integrada en la rama correspondiente.


## 9. Datos demo multi-tenant locales

Para demostraciones de clase existe un inicializador separado, opt-in e idempotente. Esta deshabilitado por defecto y se activa solo con `DEMO_SEED_ENABLED=true` (`demo.seed.enabled=true`). Las credenciales son unicamente locales/demo y no son secretos de produccion.

Credenciales: todos los usuarios demo usan `Demo123!`. Los correos son `demo.norte.organizador@plataformaeventos.local`, `demo.norte.staff@plataformaeventos.local`, `demo.norte.participante@plataformaeventos.local`, `demo.sur.organizador@plataformaeventos.local`, `demo.sur.staff@plataformaeventos.local` y `demo.sur.participante@plataformaeventos.local`.

El recorrido sugerido para probar aislamiento es iniciar con el seeder activo, iniciar sesion como organizador norte, consultar eventos con el tenant norte, intentar usar el mismo token contra el tenant sur y verificar el 403. Luego iniciar sesion como organizador sur y comprobar que solo ve los datos sur.
