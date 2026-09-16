# CU16: cuentas y pruebas manuales en Flutter

## Cuentas existentes

El proyecto ya crea estas cuentas activas mediante `AdministradorInicializador.crearUsuariosDePrueba()`:

| Correo | Contraseña inicial | Rol |
| --- | --- | --- |
| `staff@eventos.local` | `Staff123` | STAFF |
| `participante@eventos.local` | `Participante123` | PARTICIPANTE |

Ambos roles reciben `PERFIL_VER`, `PERFIL_EDITAR`, `CONVOCATORIA_VER` y `CONVOCATORIA_POSTULARSE`, mediante V3, V7 y V9.

No es necesario crear `staff.prueba@eventos.com`. El inicializador solo crea las cuentas de prueba si no existen: no restaura su contraseña ni su estado cuando ya existen y fueron modificadas.

## Levantar los servicios

Desde la carpeta `plataforma-gestion-eventos`:

```powershell
docker compose up -d --build backend
docker compose ps
```

Compose levanta también PostgreSQL y Mailpit. Flyway aplica las migraciones pendientes y el inicializador crea las cuentas que falten. No es necesario borrar ni recrear el volumen de la base de datos.

Desde `aplicacion-movil`, para un emulador Android:

```powershell
flutter run --dart-define=API_URL=http://10.0.2.2:8080/api
```

En un teléfono físico, sustituir `10.0.2.2` por la IP local del equipo donde corre el backend y usar la misma red.

## Recorrido manual

1. Iniciar sesión con `staff@eventos.local` / `Staff123`.
2. Abrir la pestaña **Convocatorias**.
3. Seleccionar una convocatoria ABIERTA, con fecha límite vigente, a la que esa cuenta aún no se haya postulado.
4. Comprobar evento, tipo de personal, descripción, cantidad y fecha límite.
5. Pulsar **Postularme**. Esperar la confirmación y `Tu postulacion: PENDIENTE`; el botón Postularme debe desaparecer.
6. Volver al listado y reabrir el detalle: el estado debe persistir.
7. Opcionalmente retirar la postulación y confirmar: debe aparecer RETIRADA y no permitir una nueva postulación a la misma convocatoria.

Para repetir el alta, utilizar otra convocatoria o la cuenta PARTICIPANTE. El retiro conserva la restricción de una postulación por usuario y convocatoria.

## Casos y resultados esperados

| Caso | Resultado |
| --- | --- |
| Listar abiertas | GET `/api/convocatorias?estado=ABIERTA`: 200 |
| Consultar detalle | GET `/api/convocatorias/{id}`: 200 |
| Sin postulación previa | GET `/api/convocatorias/{id}/postulacion`: 204 |
| Primera postulación | POST `/api/convocatorias/{id}/postulaciones`, cuerpo `{}`: 201 y PENDIENTE |
| Consultar postulación propia | GET `/api/convocatorias/{id}/postulacion`: 200 |
| Listar propias | GET `/api/postulaciones/mias`: 200 |
| Duplicado | Nuevo POST al mismo id: 400; conservar estado actual |
| Convocatoria cerrada o vencida | POST: 400; la interfaz no debe ofrecer un alta válida |
| Sin token | Rutas protegidas: 401 |
| Token de acceso vencido | Flutter intenta renovar; si el refresco es inválido, vuelve al login |
| Retirar una propia pendiente | PATCH `/api/postulaciones/{id}/retirar`: 200 y RETIRADA |
| Retirar postulación ajena | PATCH: 404 |

Flutter envía el JWT automáticamente. No se envía el identificador del usuario en el cuerpo del POST.
