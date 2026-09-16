# CU17: Seleccionar Staff y voluntarios

## Integracion con CU16

CU16 crea las postulaciones de cada usuario en `postulacion_staff`, asociadas a `convocatoria_personal` y `usuario`. CU17 continúa ese registro: no crea postulaciones nuevas ni modifica los endpoints de aplicación. Añade los estados `SELECCIONADA` y `RECHAZADA` a los estados CU16 `PENDIENTE` y `RETIRADA`.

Una selección crea un registro único en `asignacion_staff`, relacionado directamente con la postulación. El tipo de equipo (`STAFF` o `VOLUNTARIO`) se obtiene de `convocatoria_personal.tipo_personal`; no se cambia el rol global de la cuenta del usuario.

## Permisos

`CONVOCATORIA_GESTIONAR_POSTULACIONES` permite acceder a las operaciones CU17 y se asigna a `ADMINISTRADOR`, `ORGANIZADOR` y `STAFF`. Salvo el administrador, el usuario debe estar registrado como responsable del evento asociado a la convocatoria. La autorización se verifica tanto en la API como en el servicio.

## API

```http
GET   /api/convocatorias/{id}/postulaciones
PATCH /api/postulaciones/{id}/seleccionar
PATCH /api/postulaciones/{id}/rechazar
```

La consulta devuelve identidad básica del postulante (nombre, apellido y correo), convocatoria, evento, tipo de personal solicitado, estado y fechas de postulación. No incluye contraseñas ni datos de autenticación.

## Reglas

- Solo se revisan y procesan postulaciones existentes.
- Seleccionar requiere una postulación `PENDIENTE` y una convocatoria `ABIERTA`.
- Rechazar requiere una postulación `PENDIENTE`; el responsable puede resolverla aunque la convocatoria esté cerrada o cancelada.
- Solo se seleccionan cuentas activas.
- Una selección agrega una sola asignación por postulación y no supera `cantidad_requerida`.
- El bloqueo de convocatoria serializa las selecciones con el cierre y entre sí; la clave única de `asignacion_staff` protege duplicados en la base de datos.
- Los estados `SELECCIONADA`, `RECHAZADA` y `RETIRADA` son finales.

## Interfaz

La revisión está integrada en la tabla existente de convocatorias CU15. Los encargados abren la lista de postulantes, consultan su ficha y confirman selección o rechazo. En convocatorias cerradas se conserva la consulta histórica; solo se ofrece rechazar postulaciones pendientes.

## Pruebas

Las pruebas unitarias cubren listado, selección, rechazo, duplicados, cierre, límite de plazas y autorización del responsable. La integración con PostgreSQL aplica Flyway y comprueba la persistencia con `CU17_INTEGRACION=true` y una base temporal configurada con las variables habituales de backend.
