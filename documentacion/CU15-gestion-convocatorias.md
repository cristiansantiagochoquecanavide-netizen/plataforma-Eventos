# CU15: Gestión de Convocatorias de Staff y Voluntarios

## Qué implementa

CU15 permite registrar, consultar, cerrar y cancelar convocatorias de personal. Cada convocatoria pertenece obligatoriamente a un evento e indica si requiere `STAFF` o `VOLUNTARIO`, la descripción de las funciones, la cantidad de personas y la fecha límite.

Las convocatorias se conservan para mantener trazabilidad. No se eliminan físicamente y siguen este flujo:

```text
ABIERTA ── cerrar ───> CERRADA
    └──── cancelar ──> CANCELADA
```

`CERRADA` y `CANCELADA` son estados finales.

## Reglas y validaciones

- El evento debe existir y no puede estar cancelado ni finalizado.
- La descripción es obligatoria y admite hasta 1000 caracteres.
- La cantidad requerida debe ser mayor que cero.
- La fecha límite no puede estar en el pasado ni superar la fecha de inicio del evento.
- Toda convocatoria se crea en estado `ABIERTA`.
- Solamente una convocatoria abierta puede cerrarse o cancelarse.
- Las restricciones se validan en el frontend, backend y base de datos según corresponda.

## Autorización

- `ADMINISTRADOR` y `ORGANIZADOR`: consultar, crear, cerrar y cancelar.
- `STAFF` y `PARTICIPANTE`: consultar.

Permisos incorporados: `CONVOCATORIA_VER`, `CONVOCATORIA_CREAR`, `CONVOCATORIA_CERRAR` y `CONVOCATORIA_CANCELAR`.

## API

```http
GET   /api/convocatorias
GET   /api/convocatorias/{id}
POST  /api/convocatorias
PATCH /api/convocatorias/{id}/cerrar
PATCH /api/convocatorias/{id}/cancelar
```

La consulta acepta los filtros opcionales `idEvento`, `tipoPersonal`, `estado` y `termino`.

Ejemplo de registro:

```json
{
  "idEvento": 1,
  "tipoPersonal": "VOLUNTARIO",
  "descripcion": "Apoyo en acreditación y orientación de asistentes",
  "cantidadRequerida": 10,
  "fechaLimite": "2026-09-02"
}
```

## Interfaz web

La ruta `/convocatorias` incluye listado, búsqueda y filtros. Administradores y organizadores también disponen del formulario de registro y de las acciones para cerrar o cancelar convocatorias abiertas.
