# CU27 — Consultar historial de participación y eventos asistidos

**Actor:** Participante  
**Plataformas:** Web / Móvil

El participante autenticado consulta únicamente sus propias inscripciones dentro de la organización activa. El historial muestra el evento, fecha, lugar, estado de la inscripción y si existe una asistencia registrada por el staff.

## API

`GET /api/inscripciones/mi-historial`

Requiere `Authorization: Bearer <token>`, `X-Organization-Id` y el permiso `INSCRIPCION_VER_PROPIAS`.

Cada elemento devuelve `idInscripcion`, `idEvento`, `nombreEvento`, `categoriaEvento`, `fechaInicioEvento`, `fechaFinEvento`, `lugarEvento`, `fechaInscripcion`, `estadoInscripcion`, `asistio` y `fechaAsistencia`.

## Clientes

- **Web:** menú `Mi historial`, ruta `/mi-historial`.
- **Móvil:** destino `Mi historial` junto a `Mis inscripciones`.

La autorización y el aislamiento por organización se validan en el backend; los clientes no pueden consultar el historial de otro participante.
