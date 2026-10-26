# CU24 Consulta de asistencia y aforo

El monitor está disponible en **Asistencia y aforo en vivo**, ruta web `/aforo`, para usuarios con `ASISTENCIA_CONSULTAR`. La migración V34 asigna este permiso a Administrador y Organizador; el administrador SaaS conserva el acceso previsto por `ContextoOrganizacion`.

## Alcance y decisiones de negocio

Se implementa asistencia **acumulada**, compatible con el registro único por inscripción de CU23. El sistema actual no registra salidas, movimientos internos ni sesiones de varias salas. Por ello, el monitor no afirma medir cuántas personas permanecen físicamente dentro. Cada tarjeta corresponde a un evento y su sala actualmente asignada. Dos eventos en una misma sala se muestran separados; sus conteos no se suman como ocupación simultánea de esa sala.

La categoría de entrada es un nuevo atributo opcional de `inscripcion`, independiente de la categoría del evento. Los registros existentes quedan sin clasificar; no se les inventa una categoría. Administrador y Organizador pueden escribir una categoría de hasta 80 caracteres en la nómina y guardarla. El campo admite sugerencias de las categorías existentes de su organización. Un valor vacío elimina la clasificación. Esto no crea tarifas, pagos ni nuevos tipos de inscripción.

## Métricas y filtros

| Indicador | Definición |
| --- | --- |
| Inscripciones consideradas | Confirmadas, más cualquier inscripción que ya tenga asistencia, aunque posteriormente haya sido cancelada. Excluye canceladas y en espera sin asistencia. |
| Asistentes registrados | Inscripciones consideradas con registro en `asistencia`. |
| Ausentes | Consideradas menos asistentes registrados. No significa “personas que ya salieron”. |
| Porcentaje de asistencia | Asistentes / inscripciones consideradas × 100; cero cuando el denominador es cero. |
| Ingresos en franja | Asistencias cuyo timestamp está en `[desde, hasta)`. |
| Aforo acumulado | Todas las asistencias del evento / capacidad efectiva × 100. No se reduce al filtrar categoría o franja. |
| Capacidad efectiva | El menor valor entre aforo máximo del evento y capacidad de su espacio, cuando ambos existen; el disponible cuando solo existe uno. Sin capacidad, no se calcula porcentaje. |

Evento y sala filtran los grupos mostrados. Categoría filtra los totales de asistencia y la nómina. La franja horaria filtra la nómina y el indicador “Ingresos en franja”, pero no redefine ausentes ni reduce el aforo. Sin franja, la nómina incluye presentes y ausentes; con franja, incluye solo registros con ingreso en ese intervalo. Los totales de varios eventos cuentan inscripciones, no personas únicas entre eventos.

Los umbrales son inclusivos: normal por debajo del 80%, precaución desde 80%, crítico desde 90%, capacidad alcanzada desde 100%. Los valores superiores al 100% se conservan en texto, aunque la barra se llena como máximo al 100%. Se priorizan estos criterios de aceptación frente a la mención aislada del 85% en el documento del sprint.

El navegador interpreta los controles de fecha en su zona local y envía ISO 8601 con zona. El backend compara instantes; las exportaciones incluyen el offset explícito de los timestamps.

## API y permisos

Todos los endpoints requieren JWT y la organización activa mediante `X-Organization-Id`, validados con los servicios existentes. La pertenencia y la suscripción de la organización se comprueban en el servidor. Los IDs ajenos no revelan datos.

| Método y ruta | Resultado |
| --- | --- |
| `GET /api/aforo/opciones` | Eventos y salas de la organización; categorías de entrada utilizadas. |
| `GET /api/aforo` | `resumen`, `nomina` y `actualizado`; nómina de 25 filas por página. |
| `GET /api/aforo/exportar?formato=csv` | CSV UTF-8 con BOM, escape de comillas y protección frente a fórmulas. |
| `GET /api/aforo/exportar?formato=pdf` | PDF con fuente incrustada, texto ajustado a columnas y encabezados repetidos. |
| `PATCH /api/aforo/inscripciones/{id}/categoria` | Cuerpo `{"categoria":"Estudiante"}`; clasificación auditada dentro del tenant. |

Parámetros de consulta y exportación: `eventoId`, `salaId`, `categoria`, `desde`, `hasta`. Omitir `categoria` significa todas; `categoria=` significa sin clasificar. La consulta acepta `pagina` desde cero. Las exportaciones contienen toda la nómina filtrada, no solo la página visible. Si superan 5000 registros, se rechazan con un mensaje para aplicar más filtros; no se entrega un archivo truncado.

Las consultas agregan en SQL y los listados se paginan. La instantánea de resumen y nómina se obtiene en una transacción de lectura repetible. No existe una tabla de contadores paralelos: `asistencia` sigue siendo la fuente de verdad, por lo que no se necesita reconciliar incrementos manuales. No se modificó el bloqueo ni la restricción única de CU23.

## Canal en vivo

- Endpoint: `/api/ws-aforo`, WebSocket nativo con STOMP; `ws` en desarrollo y `wss` bajo HTTPS.
- `CONNECT` lleva `Authorization: Bearer <token>` y `X-Organization-Id`. El JWT no viaja en la URL.
- Destino permitido: `/topic/aforo/{organizacionId}` de la organización autorizada de la sesión.
- Mensaje: `{"eventoId":123}`. Es un aviso para recargar la instantánea REST; no transmite la nómina ni datos personales. `eventoId:0` señala un cambio en la configuración de espacios.
- Los listeners de persistencia detectan altas y cambios en asistencia, inscripción, evento y espacio. El aviso se emite en `AFTER_COMMIT`; no se publica tras rollback.
- Se rechazan publicaciones `SEND` del cliente y suscripciones a otros destinos. La sesión y permisos se vuelven a validar antes de cada entrega.
- El broker simple no genera receipts de suscripción. El interceptor confirma `SUBSCRIBE` después de que el broker lo procese. Solo entonces el cliente marca “En vivo” y obtiene una nueva instantánea; así se recuperan los cambios ocurridos durante la desconexión.
- El frontend agrupa avisos durante 200 ms, sustituye la instantánea en lugar de incrementar contadores y cancela peticiones anteriores al cambiar filtros. Desconecta y limpia datos al cambiar de organización o cerrar la sesión.
- Hay heartbeats de 10 segundos y reintentos de conexión cada 4 segundos. Al expirar el JWT se reconecta usando el interceptor REST de renovación existente. No se usa polling para simular tiempo real.

La configuración está preparada para **una instancia de backend**. Varias réplicas requieren un broker compartido y coordinación de eventos; no basta con balancear conexiones. La configuración Nginx incluida admite Upgrade de WebSocket. Otros proxies o balanceadores deben permitir `/api/ws-aforo` y mantener conexiones persistentes. `seguridad.cors.origenes` debe incluir el origen web autorizado.

Si un aviso no puede enviarse después de un commit, no se revierte ni se presenta como fallido el check-in ya guardado. Se registra la incidencia y la siguiente actualización, reconexión o botón Actualizar recupera la instantánea.

## Ejecución

1. Iniciar Docker Desktop si se utilizará la demo en contenedores.
2. Desde la raíz del repositorio, ejecutar `docker compose -f docker-compose.yml -f docker-compose.local.yml up --build -d` usando la configuración local existente. No borrar volúmenes.
3. El backend aplicará V34 mediante Flyway. El frontend requiere la dependencia nueva `@stomp/stompjs`, incluida en el lockfile.
4. Iniciar sesión con Administrador u Organizador y seleccionar la organización. Si la sesión ya estaba abierta antes de migrar, volver a iniciar sesión para actualizar los permisos del perfil.
5. Abrir **Asistencia y aforo en vivo** en el menú.

Para desarrollo sin Docker: instalar dependencias del frontend con `npm ci`, iniciar backend y PostgreSQL con la configuración habitual y ejecutar `npm start` en `frontend-web`.

## Verificación y demostración PT-S3-04

1. Preparar un evento de prueba con una sala y capacidad efectiva de 10; contar con al menos 10 inscripciones confirmadas y un staff con asignación activa. Si el control de cupos impide preparar el escenario, usar un fixture de prueba aislado; no modificar datos reales para forzarlo.
2. Abrir el monitor como Organizador y confirmar el indicador “En vivo”. Abrir el registro de asistencia con el staff en otro navegador o en la app móvil existente.
3. Registrar ingresos y comprobar que cambian los conteos y porcentajes sin recargar. Comparar el resultado con la nómina.
4. Al llegar a 8, 9 y 10 ingresos, verificar respectivamente precaución, crítico y capacidad alcanzada.
5. Intentar repetir un check-in. Debe mantenerse la negativa de CU23 y no crecer el conteo. Ejecutar también registros desde dos clientes para comprobar el comportamiento concurrente contra PostgreSQL.
6. Clasificar algunas entradas, filtrar sala/categoría/franja y comprobar los límites de hora. Verificar que el aforo total no baja artificialmente por esos filtros.
7. Exportar CSV y PDF; comparar la nómina completa filtrada, comprobar acentos y varias páginas.
8. Desconectar temporalmente la red del monitor, registrar ingresos desde otro cliente y reconectar. Debe recuperar la instantánea vigente sin duplicar registros.
9. Cambiar a otra organización y confirmar limpieza de filtros, datos y conexión. Un participante sin permiso no debe poder abrir la consulta ni descargar archivos.

## Pruebas automatizadas y límites de verificación

- `AforoServiceTest`: consultas reales en H2, filtros combinados, rango semiabierto, tenant, categoría sin clasificar, ausentes, eventos vacíos, capacidad, paginación y límites de alertas.
- `AforoExportadorTest`: escape CSV, caracteres acentuados, PDF de 64 filas y extracción del texto de varias páginas. Genera imágenes de revisión en `backend/target/cu24-qa`; se revisaron las seis páginas.
- `AforoNotificadorTest`: transacciones reales de prueba, emisión solo después del commit y ausencia de envío tras rollback.
- `AforoWebSocketTest`: servidor local y conexiones WebSocket/STOMP reales, confirmación de suscripción, aislamiento entre tenants, reconexión, token inválido/expirado, bloqueo de publicaciones del cliente y autorización HTTP de consultas/exportaciones. La identidad y el contexto se simulan; las pruebas existentes cubren el servicio real de organizaciones.
- `aforo.component.spec.ts`: vista, sobreocupación, agrupación de avisos, paginación, filtros, cancelación de respuestas antiguas y cambio de organización.

Comandos: `backend\mvnw.cmd test` desde `backend`; `npm test -- --watch=false --browsers=ChromeHeadless` y `npm run build` desde `frontend-web`.

Resultado de la comprobación local del 26/10/2026: 207 pruebas del backend aprobadas, 90 pruebas del frontend aprobadas y compilación de producción Angular correcta.

No se afirma la latencia inferior a 150 ms declarada en el documento: no se realizó esa medición. Tampoco se considera probado el ingreso concurrente desde dispositivos físicos ni el flujo desde Flutter.

### Demo integrada verificada

El 26/10/2026 se levantó el proyecto Compose `cu24-demo`, con volúmenes separados. La base local anterior tenía una contraseña distinta de la configuración disponible; sus datos y contraseña se conservaron. La demo usa PostgreSQL 17 y las migraciones Flyway hasta V34, backend, Nginx, web, servicio IA y KMS local.

Se corrigió un error de compilación desde cero causado por usar el nombre completo de `Isolation` junto con un parámetro llamado `org`; ahora se utiliza un import explícito. Después pasó nuevamente la suite limpia de 207 pruebas del backend.

Se verificaron mediante la API real de la demo: login, consulta y agregaciones PostgreSQL, suscripción STOMP atravesando Nginx, un check-in del usuario demo Norte, incremento exacto del contador, rechazo del duplicado con 409, exportaciones CSV/PDF y rechazo de consultas de otra organización o de un staff sin permiso. Estas comprobaciones se hicieron con clientes HTTP/STOMP, no mediante un dispositivo Flutter. Queda pendiente validar una migración sobre la base anterior con sus credenciales y probar desde el dispositivo móvil.

Para uso sencillo, abrir Docker Desktop y ejecutar `iniciar-cu24-demo.cmd` con doble clic. Para detener conservando los datos, ejecutar `detener-cu24-demo.cmd`. Ambos usan `-p cu24-demo` para mantener la demo separada. No iniciar simultáneamente otro stack que utilice los mismos puertos 4200, 8080, 5432 y 8000.

Acceso: `http://localhost:4200/iniciar-sesion`. Organizador para probar: `demo.sur.organizador@plataformaeventos.local`; staff en ventana privada: `demo.sur.staff@plataformaeventos.local`. Clave local de ambos: `Demo123!`. Seleccionar Centro Cultural Sur Demo. Abrir el monitor con el organizador y Registrar asistencia con el staff. La inscripción Sur se dejó pendiente para esta prueba manual; el check-in automatizado se realizó en Norte.
