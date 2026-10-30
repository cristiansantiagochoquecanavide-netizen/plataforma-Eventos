# API Sprint 2

## Alcance y tenant activo

Base local: `http://localhost:8080/api`. Salvo autenticacion, salud y listado de organizaciones propias, las operaciones requieren `Authorization: Bearer <JWT>` y `X-Organization-Id: <organizationId>`. El segundo header identifica el tenant activo; el backend valida que la organizacion este activa y que el usuario tenga una membresia activa. Un `SAAS_ADMINISTRADOR` puede resolver organizaciones sin membresia.

La coleccion Postman relacionada es `documentacion/postman/Plataforma-Gestion-Eventos-Sprint-0.postman_collection.json`. Los IDs de los ejemplos se reemplazan con las variables `organizationId`, `usuarioId`, `convocatoriaId`, `postulacionId`, `asignacionId` y `accessToken`.

## Contrato de endpoints

| Metodo | Endpoint | Headers | Permiso / actor | Respuesta |
|---|---|---|---|---|
| GET | `/autenticacion/usuario-actual` | Bearer (+ tenant opcional) | Usuario autenticado | 200 usuario con roles globales y `perfilOrganizacionActiva` si la membresia tenant esta activa |
| GET | `/organizaciones/mias` | Bearer | Usuario autenticado | 200 lista |
| GET | `/roles` | Bearer | `SAAS_ADMINISTRADOR` global | 200 lista |
| GET/PUT | `/roles/{idRol}/permisos` | Bearer | `SAAS_ADMINISTRADOR` global | 200 |
| GET | `/permisos` | Bearer | `SAAS_ADMINISTRADOR` global | 200 lista |
| POST | `/organizaciones` | Bearer | `ORGANIZACION_GESTIONAR` o `SAAS_ADMINISTRADOR` | 201 |
| POST | `/organizaciones/{idOrganizacion}/membresias` | Bearer + tenant | Administrador de organizacion | 201 |
| GET | `/organizaciones/activa/miembros` | Bearer + tenant | Administrador de organizacion | 200 lista |
| PATCH | `/organizaciones/{idOrganizacion}/membresias/{idUsuario}/estado?activo=true` | Bearer + tenant | Administrador de organizacion | 204 |
| GET | `/usuarios?pagina=0&tamano=20` | Bearer + tenant | `USUARIO_VER` | 200 pagina |
| GET | `/usuarios/{idUsuario}` | Bearer + tenant | `USUARIO_VER` | 200 |
| POST | `/usuarios` | Bearer + tenant | `USUARIO_CREAR` | 201 |
| PUT | `/usuarios/{idUsuario}` | Bearer + tenant | `USUARIO_EDITAR` | 200 |
| PATCH | `/usuarios/{idUsuario}/estado` | Bearer + tenant | `USUARIO_CAMBIAR_ESTADO` | 200 |
| GET | `/categorias` | Bearer + tenant | `EVENTO_VER` | 200 lista |
| GET | `/categorias/{idCategoria}` | Bearer + tenant | `EVENTO_VER` | 200 |
| POST | `/categorias` | Bearer + tenant | `EVENTO_CREAR` | 201 |
| PUT | `/categorias/{idCategoria}` | Bearer + tenant | `EVENTO_EDITAR` | 200 |
| DELETE | `/categorias/{idCategoria}` | Bearer + tenant | `EVENTO_ELIMINAR` | 204 |
| GET | `/eventos?termino=&categoria=` | Bearer + tenant | `EVENTO_VER` | 200 lista |
| GET | `/eventos/{idEvento}` | Bearer + tenant | `EVENTO_VER` | 200 |
| GET | `/eventos/responsables-disponibles` | Bearer + tenant | `EVENTO_ASIGNAR_RESPONSABLE` | 200 lista |
| POST | `/eventos` | Bearer + tenant | `EVENTO_CREAR` | 201 |
| PUT | `/eventos/{idEvento}` | Bearer + tenant | `EVENTO_EDITAR` | 200 |
| PUT | `/eventos/{idEvento}/responsables` | Bearer + tenant | `EVENTO_ASIGNAR_RESPONSABLE` | 200 |
| GET | `/convocatorias?idEvento=&tipoPersonal=&estado=&termino=` | Bearer + tenant | `CONVOCATORIA_VER` | 200 lista |
| GET | `/convocatorias/{id}` | Bearer + tenant | `CONVOCATORIA_VER` | 200 |
| POST | `/convocatorias` | Bearer + tenant | `CONVOCATORIA_CREAR` | 201 |
| PATCH | `/convocatorias/{id}/cerrar` | Bearer + tenant | `CONVOCATORIA_CERRAR` | 200 |
| PATCH | `/convocatorias/{id}/cancelar` | Bearer + tenant | `CONVOCATORIA_CANCELAR` | 200 |
| GET | `/participantes?eventoId=&busqueda=&estado=&pagina=0&tamano=20` | Bearer + tenant | `PARTICIPANTE_VER` o `USUARIO_VER` | 200 pagina (CU10) |
| GET | `/participantes/{idParticipante}` | Bearer + tenant | `PARTICIPANTE_VER` o `USUARIO_VER` | 200 detalle e historial (CU10) |
| PATCH | `/participantes/{idParticipante}/estado` | Bearer + tenant | `PARTICIPANTE_GESTIONAR` o `USUARIO_CAMBIAR_ESTADO` | 200 (CU10) |
| GET | `/participantes/exportar?eventoId=&busqueda=&estado=` | Bearer + tenant | `PARTICIPANTE_VER` o `USUARIO_VER` | 200 archivo CSV (CU10) |
| POST | `/inscripciones/eventos/{idEvento}` | Bearer + tenant | `INSCRIPCION_CREAR` | 201 |
| GET | `/inscripciones/mis-inscripciones` | Bearer + tenant | `INSCRIPCION_VER_PROPIAS` | 200 lista |
| PATCH | `/inscripciones/{idInscripcion}/cancelar` | Bearer + tenant | `INSCRIPCION_CANCELAR` | 200 |
| GET | `/inscripciones/eventos/{idEvento}/disponibilidad` | Bearer + tenant | `EVENTO_VER` | 200 |
| POST | `/postulaciones-staff/convocatorias/{idConvocatoria}` | Bearer + tenant | `POSTULACION_STAFF_CREAR` | 201 |
| GET | `/postulaciones-staff/mias` | Bearer + tenant | Autenticado | 200 lista |
| GET | `/postulaciones-staff/convocatorias/{idConvocatoria}` | Bearer + tenant | `POSTULACION_STAFF_GESTIONAR` | 200 lista |
| PATCH | `/postulaciones-staff/{id}/aceptar` | Bearer + tenant | `POSTULACION_STAFF_GESTIONAR` | 200 |
| PATCH | `/postulaciones-staff/{id}/rechazar` | Bearer + tenant | `POSTULACION_STAFF_GESTIONAR` | 200 |
| POST | `/asignaciones-staff` | Bearer + tenant | `ASIGNACION_STAFF_GESTIONAR` | 201 |
| GET | `/asignaciones-staff` | Bearer + tenant | `ASIGNACION_STAFF_GESTIONAR` | 200 lista |
| GET | `/asignaciones-staff/mias` | Bearer + tenant | Autenticado | 200 lista |
| PATCH | `/asignaciones-staff/{id}/cancelar` | Bearer + tenant | `ASIGNACION_STAFF_GESTIONAR` | 200 |
| GET | `/admin/bitacora` | Bearer + tenant | `BITACORA_VER` o `ROLE_ADMINISTRADOR` | 200 pagina |

Los filtros de bitacora son `modulo`, `entidad`, `accion`, `exito`, `actor`, `desde`, `hasta`, `page`, `size` y `sort`. El resultado es una pagina Spring (`content`, `totalElements`, `totalPages`, etc.).

## Ejemplos JSON

Crear organizacion:

```json
{"nombre":"Organizacion Demo","slug":"organizacion-demo"}
```

Crear categoria:

```json
{"nombre":"Conferencia","descripcion":"Eventos academicos"}
```

Crear evento:

```json
{"nombre":"Evento Demo","descripcion":"Descripcion","fechaInicio":"2030-06-01T10:00:00Z","fechaFin":"2030-06-01T18:00:00Z","lugarPrincipal":"Auditorio","idCategoria":3,"idEspacio":1,"aforoMaximo":50,"estado":"PUBLICADO","responsables":[]}
```

Crear convocatoria:

```json
{"idEvento":10,"tipoPersonal":"STAFF","descripcion":"Apoyo de produccion","cantidadRequerida":2,"fechaLimite":"2030-05-15"}
```

Crear postulacion y asignacion:

```json
{"motivacion":"Experiencia en produccion de eventos"}
```

```json
{"idPostulacion":20,"tarea":"Registro de asistentes","turnoInicio":"2030-06-01T09:00:00Z","turnoFin":"2030-06-01T13:00:00Z","observaciones":"Presentarse antes"}
```

Ejemplo de respuesta de inscripcion:

```json
{"idInscripcion":30,"idEvento":10,"nombreEvento":"Evento Demo","estado":"CONFIRMADA","fechaInscripcion":"2030-05-01T12:00:00Z"}
```

Ejemplo de bitacora:

```json
{"id":40,"organizacion":1,"actor":2,"accion":"CREAR","modulo":"INSCRIPCIONES","entidad":"INSCRIPCION","idEntidad":30,"fecha":"2030-05-01T12:00:00Z","ip":"127.0.0.1","exito":true,"resultado":"Inscripcion creada"}
```

## Errores y seguridad

| Codigo | Significado |
|---|---|
| 400 | JSON invalido, validacion fallida, duplicado, aforo completo o transicion no permitida. |
| 401 | Falta el bearer JWT, es invalido, o no se pudo resolver el tenant activo. |
| 403 | El usuario no tiene el permiso requerido, membresia activa o acceso al tenant indicado. |
| 404 | El recurso no existe dentro de la organizacion activa. |

Las consultas y escrituras de eventos, categorias, convocatorias, inscripciones, postulaciones, asignaciones y bitacora se filtran por organizacion. No existe en Sprint 2 un endpoint de participantes independiente: la gestion de usuarios usa `/usuarios`, y las inscripciones propias usan el usuario autenticado.


## Administrador SaaS vs administrador tenant

`SAAS_ADMINISTRADOR` es un rol global guardado en `usuario_rol` y se emite como authority para proteger la administracion de roles y permisos globales (`/roles` y `/permisos`). `ADMINISTRADOR` sigue existiendo como rol de membresia en `usuario_organizacion_rol`: permite administrar miembros y operaciones dentro del tenant activo, pero no habilita administracion global SaaS.

`GET /autenticacion/usuario-actual` mantiene `roles` y `permisos` como perfil global del usuario. Si la solicitud incluye `X-Organization-Id` y el usuario tiene una membresia activa en ese tenant, agrega `perfilOrganizacionActiva` con `idOrganizacion`, roles tenant, permisos tenant y `administrador`. Si el usuario tiene el rol global `SAAS_ADMINISTRADOR` pero no una membresia en la organizacion seleccionada, devuelve un perfil administrativo virtual para esa solicitud (sin crear una membresia persistida), coherente con el bypass global de `ContextoOrganizacion`. Los demas usuarios sin membresia reciben el campo ausente/nulo.

`GET /organizaciones/mias` devuelve las membresias activas propias para usuarios normales y todas las organizaciones activas para `SAAS_ADMINISTRADOR`, para permitir cambiar el contexto SaaS sin limitarse a `principal`.

El inicializador asigna `SAAS_ADMINISTRADOR` al `ADMIN_CORREO` configurado y mantiene su membresia `ADMINISTRADOR` en la organizacion `principal` para las operaciones tenant. No se puede asignar `SAAS_ADMINISTRADOR` desde los flujos de membresia de organizacion.

Credenciales demo/locales del administrador SaaS: `ADMIN_CORREO=admin@eventos.local` y `ADMIN_CLAVE` segun el entorno local. Las credenciales de organizadores demo listadas abajo son administradores del tenant, no administradores SaaS.

## Visibilidad de modulos y privilegios de interfaz

La politica efectiva de `/componentes-ui/actual/privilegios` parte de los permisos del perfil de la organizacion activa; no requiere que cada usuario pertenezca a un grupo UI para ver sus modulos autorizados. Los permisos de grupos pueden reducir estados de interfaz (por ejemplo, dejar un formulario visible pero de solo lectura), pero no pueden ampliar permisos del backend. La administracion global de roles se basa exclusivamente en `SAAS_ADMINISTRADOR`; `ROL_VER` tenant no habilita ese modulo.

El backend sigue siendo la autoridad para cada operacion: ocultar o habilitar controles en la interfaz no sustituye `@PreAuthorize` ni la validacion del tenant.

## Demo multi-tenant local

El seeder demo esta deshabilitado por defecto. Para activarlo solo en un entorno local o de clase, iniciar el backend con `DEMO_SEED_ENABLED=true` o configurar `demo.seed.enabled=true`. Las credenciales siguientes son unicamente locales/demo; no deben usarse en produccion:

| Tenant | Slug deterministico | Usuario | Correo | Clave |
|---|---|---|---|---|
| Universidad Norte Demo | `demo-universidad-norte` | Organizador/Admin tenant | `demo.norte.organizador@plataformaeventos.local` | `Demo123!` |
| Universidad Norte Demo | `demo-universidad-norte` | Staff | `demo.norte.staff@plataformaeventos.local` | `Demo123!` |
| Universidad Norte Demo | `demo-universidad-norte` | Participante | `demo.norte.participante@plataformaeventos.local` | `Demo123!` |
| Centro Cultural Sur Demo | `demo-centro-cultural-sur` | Organizador/Admin tenant | `demo.sur.organizador@plataformaeventos.local` | `Demo123!` |
| Centro Cultural Sur Demo | `demo-centro-cultural-sur` | Staff | `demo.sur.staff@plataformaeventos.local` | `Demo123!` |
| Centro Cultural Sur Demo | `demo-centro-cultural-sur` | Participante | `demo.sur.participante@plataformaeventos.local` | `Demo123!` |

Cada tenant incluye categoria, espacio, evento futuro publicado, convocatoria abierta, postulacion staff aceptada, asignacion staff e inscripcion de participante. El seeder es idempotente: usa slugs, correos y nombres deterministicos para no duplicar datos al reiniciar.

### Recorrido de aislamiento para el instructor

1. Iniciar el backend con `DEMO_SEED_ENABLED=true`.
2. Autenticarse con el organizador norte y obtener sus organizaciones en `GET /organizaciones/mias`; guardar el `id` de `demo-universidad-norte`.
3. Consultar `GET /eventos` con `X-Organization-Id` del tenant norte: debe aparecer `Jornada Docente Demo Norte`.
4. Repetir la consulta con el `X-Organization-Id` del tenant sur usando el mismo token norte: debe responder 403 porque ese usuario no pertenece al tenant sur.
5. Autenticarse con el organizador sur y consultar con el tenant sur: debe aparecer `Festival Comunitario Demo Sur` y no el evento norte.
