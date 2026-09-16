# API Sprint 2

## Alcance y tenant activo

Base local: `http://localhost:8080/api`. Salvo autenticacion, salud y listado de organizaciones propias, las operaciones requieren `Authorization: Bearer <JWT>` y `X-Organization-Id: <organizationId>`. El segundo header identifica el tenant activo; el backend valida que la organizacion este activa y que el usuario tenga una membresia activa. Un `SAAS_ADMINISTRADOR` puede resolver organizaciones sin membresia.

La coleccion Postman relacionada es `documentacion/postman/Plataforma-Gestion-Eventos-Sprint-0.postman_collection.json`. Los IDs de los ejemplos se reemplazan con las variables `organizationId`, `usuarioId`, `convocatoriaId`, `postulacionId`, `asignacionId` y `accessToken`.

## Contrato de endpoints

| Metodo | Endpoint | Headers | Permiso / actor | Respuesta |
|---|---|---|---|---|
| GET | `/organizaciones/mias` | Bearer | Usuario autenticado | 200 lista |
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
