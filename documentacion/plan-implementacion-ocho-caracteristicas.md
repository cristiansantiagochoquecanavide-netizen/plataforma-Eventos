# Plan por fases: ocho características del proyecto

## Objetivo

Completar y demostrar las ocho características del documento `8 puntos Sprint_2.pdf`, aprovechando lo que ya existe y registrando la evidencia de aceptación. El alcance de “solución universal” se limita al dominio del producto: organizaciones que gestionan eventos, con aislamiento multi-tenant; no implica soportar industrias arbitrarias.

## Situación de partida

### Actualización posterior: autoservicio SaaS con Stripe (2026-10-05)

La decisión inicial de administración manual sin pasarela descrita abajo queda supersedida para la iteración actual: el usuario confirmó catálogo mensual en BOB y Stripe para el piloto. Se implementó el flujo público de alta, verificación de correo, Checkout y activación tras webhook, junto con los límites indicados por el usuario. La implementación local compila; backend pasó 164 pruebas y Angular el build de producción más 49 pruebas. La ruta, configuración, limitaciones de Stripe Bolivia/BOB y pendientes operativos están en `documentacion/onboarding-saas-stripe.md`. No se ha configurado una cuenta Stripe, procesado un cobro ni desplegado infraestructura; hace falta confirmar elegibilidad comercial/moneda y probar el ciclo end-to-end en test antes de considerarlo listo para producción.

- Ya existen backend, web y móvil; API multi-tenant; gestión de usuarios, roles, permisos y grupos; privilegios de interfaz; bitácora con cifrado de payload; exportación CSV de participantes; e infraestructura declarativa inicial para AWS.
- Backend: 159 pruebas aprobadas en la última ejecución. Frontend: build de producción y 49 pruebas aprobadas. Flutter: 16 pruebas aprobadas y análisis estático sin issues. Compose principal con override local validado.
- Ya se verificaron localmente aislamiento HTTP con dos tenants, exportaciones/correo SMTP en Mailpit, restauración/backfill de 16 filas históricas y gate SaaS suspendido/vencido/reactivación. Restan evaluación de usabilidad/paridad de clientes, recorrido visual administrativo y despliegue cloud.
- Compatibilidad Flyway verificada en restauración: V22 conserva el SQL/checksum histórico de `distinguir_administrador_saas`; la asignación nueva de `EVENTO_VER` quedó en V30. Una base que ya hubiera aplicado la versión transitoria de V22 requiere una reparación controlada de checksum.
- El despliegue en una cuenta cloud, el uso de dominio/certificados y cualquier cambio de datos productivos requerirán autorización explícita antes de ejecutarse.

### Decisiones confirmadas para el alcance

- Reportes: se implementarán CSV actual, Excel/XLSX, HTML, PDF y envío por correo.
- SaaS: en esta etapa la administración de plan, vigencia y estado será manual; no se conectará una pasarela de pago.
- Bitácora: el objetivo es una llave única bajo control del desarrollador. Custodia, recuperación y el límite de confianza frente a administradores cloud deben quedar diseñados y verificados antes de procesar datos históricos.
- Custodia confirmada: la llave del desarrollador debe llegar al backend como secreto de runtime; nunca almacenarse en Git ni PostgreSQL. La migración debe conservar lectura/recifrado de registros con KMS del esquema anterior antes de retirar ese acceso.
- Siguen pendientes de definición la política de backup (retención, RPO/RTO) y la asignación formal de casos de uso a Web/Móvil.
- Política provisional confirmada: local bajo demanda; RDS retención de 7 días, ventana UTC `03:00-04:00`, objetivos RPO ≤ 5 min y RTO ≤ 4 h sujetos a prueba.

## Progreso de implementación

### Fases 0 y 1 — Completadas en entorno local

- Se actualizaron estados, evidencia reciente y escenarios de aceptación en la matriz.
- Se añadieron pruebas HTTP para dos organizaciones con acceso permitido (200) y acceso cruzado a un evento (404), más una prueba JPA/H2 que persiste dos tenants y valida consultas scoped.
- La suite backend actual quedó en 159 pruebas aprobadas.
- Las pruebas integrales revelaron y se corrigieron un mapeo JPA de privilegio-grupo que impedía inicializar Hibernate y dos JPQL de revocación de tokens con tipos de fecha incompatibles.
- Se verificaron los mismos escenarios contra PostgreSQL 17 limpio: dos tenants listan un evento y la consulta cruzada devuelve 404. Siguen pendientes el recorrido de administración UI y la misma cuenta/tenant a través de Web y Móvil.

### Fase 2 — Herramientas locales implementadas; AWS queda como IaC sin despliegue

- `infra/backup/backup-postgres.ps1` crea bajo demanda backup PostgreSQL custom format en `backups/`, valida que `pg_restore` lo pueda leer y calcula SHA-256.
- `infra/backup/restore-postgres-validation.ps1` valida ruta y checksum y restaura solo a una base nueva `restore_validation_*`; nunca sobrescribe la base de aplicación y conserva la copia para inspección.
- Backup y restore se ejecutaron sobre Compose local. La copia antigua, inicialmente con 22 tablas/22 migraciones, migró hasta V30 (27 tablas/30 migraciones); la original no se modificó.
- CloudFormation establece retención automática RDS de 7 días y una ventana de backup UTC. Ningún recurso AWS fue creado ni modificado.
- Los objetivos RPO/RTO quedan como metas provisionales hasta una prueba AWS autorizada que mida recuperación en el servicio real.

### Fase 3 — Reporte configurable de participantes implementado

- Web permite seleccionar columnas, ordenar, previsualizar, descargar y enviar por correo el reporte.
- Backend limita columnas/formato a enums permitidos, reutiliza filtros y repositorio scoped por tenant, escapa HTML, neutraliza fórmulas CSV y limita reportes a 5000 filas. Las exportaciones grandes requieren acotar filtros.
- Implementados formatos CSV, XLSX, HTML y PDF; el correo adjunta el formato elegido.
- Verificación: suite backend completa (159 pruebas), build web y suite frontend (49 pruebas) pasan. Se probaron CSV, XLSX, HTML y PDF contra PostgreSQL; un correo con adjunto CSV fue capturado por Mailpit.
- El reporte inicial cubre participantes; incorporar más entidades exige añadir fuentes de datos y sus permisos sin aceptar consultas SQL dinámicas.
- El test de repositorio con H2 verifica agregados y aislamiento entre dos tenants; el mapeo devuelve métricas en una proyección agregada y evita consultas por participante.

### Fases 4 y 5 — Ayuda integrada y asignación de plataformas iniciadas

- Web tiene página `Ayuda y guías` con instrucciones para tenant, eventos, participantes/reportes, grupos/privilegios, staff y bitácora.
- Flutter incorpora destino Ayuda con guías de eventos, inscripciones, staff, perfil y permisos.
- `matriz-casos-uso-web-movil.md` distingue tareas administrativas web (usuarios, grupos, reportes, auditoría) de operaciones móviles; no se encontró flujo navegable de inscripción Web.
- La implementación cubre ayuda contextual mínima y matriz inicial. Falta evaluación breve con usuarios y verificación funcional con una misma cuenta en los dos clientes.
- Flutter: Ayuda permanece accesible como único destino y conserva la opción de cerrar sesión; tests y análisis estático pasan.

### Fase 6 — Llave del desarrollador y transición de la bitácora implementadas; ensayo local completado

- Decisión confirmada: las nuevas escrituras usarán una llave AES-256 bajo control del desarrollador, inyectada como secreto de runtime `AUDIT_DEVELOPER_KEY_BASE64`.
- El formato mantiene envelopes KMS v1 descifrables durante transición y escribe v2 con AES-GCM directo. El backfill puede re-cifrar texto plano y envelopes v1 a v2; sigue siendo opt-in.
- KMS permanece para descifrar envelopes heredados y producir índices HMAC exactos con una llave separada; esos índices no permiten recuperar payloads. La nueva lectura de contenido v2 depende de la llave del desarrollador.
- CloudFormation recibe la llave runtime y su CMK desde Secrets Manager externo controlado por el desarrollador. El permiso de descifrado KMS v1 es opt-in con `EnableLegacyAuditPayloadDecrypt` (deshabilitado por defecto, temporal durante backfill); KMS HMAC continúa para los índices existentes. La rotación de HMAC requiere una ventana de mantenimiento y el modo opt-in `audit.cifrado.backfill.reindex-indexes` para recalcular todas las filas v2 con la llave nueva; conservar la anterior hasta expirar snapshots/backups antiguos.
- Pruebas H2 y un ensayo PostgreSQL sobre la restauración antigua migrada a V30 cubren el backfill de 16 filas históricas locales: todas pasaron a v2 y no quedaron columnas sensibles en claro. El ensayo no fue sobre producción; cualquier backfill productivo requiere autorización específica.

### Fase 7 — Ciclo manual de suscripción implementado; nube no desplegada

- V29 crea suscripción única por organización y asigna plan BASIC/ACTIVA a los tenants existentes.
- Las organizaciones nuevas y el seeder demo reciben plan BASIC vigente; `ContextoOrganizacion` bloquea tenants sin suscripción activa/vigente y `SAAS_ADMINISTRADOR` conserva acceso administrativo para reactivarlas.
- Backend expone listado/actualización manual de planes, estado, inicio y fin, solo para `SAAS_ADMINISTRADOR`; la navegación Web incluye formulario de administración y no hay pagos reales. En PostgreSQL se verificó HTTP: no administrador 403, suscripción suspendida/vencida 403 y reactivada 200 en consulta de eventos; falta el recorrido visual del panel con usuario.
- CloudFormation configura backups RDS, llave externa de bitácora y TLS 1.3/1.2 con HTTP→HTTPS; dominio y certificado se proveen como parámetros.
- Permanecen pendientes DNS/ACM, despliegue de prueba, health-check en AWS y demo cloud con dos tenants. No se mutó AWS.

## Principios de ejecución

1. Mantener la autorización en el backend; ocultar controles de interfaz no sustituye permisos de API.
2. Toda consulta, exportación, reporte y operación administrativa debe respetar el tenant activo.
3. No declarar una característica completa sin pruebas reproducibles y evidencia de demostración.
4. Mantener documentación y artefactos de curso fuera de los commits de producto, según el alcance solicitado.
5. Cada fase termina con revisión de código, pruebas dirigidas y actualización de la matriz de aceptación.

## Fases

### Fase 0 — Línea base y decisiones de alcance

**Propósito:** convertir las ocho características en criterios verificables, coherentes con el producto existente.

**Trabajo**
- Actualizar `matriz-aceptacion-ocho-caracteristicas.md` con las pruebas recientes de backend y frontend.
- Crear una lista de escenarios demostrables y enlazarlos con requisito, endpoint, pantalla y prueba.
- Resolver antes de implementar: formato de reportes prioritarios; política de respaldo/retención; significado de “llave única del desarrollador” para bitácora; nivel de suscripción/billing esperado; alcance por plataforma.

**Salida / gate**
- Matriz con estado inicial y evidencia fechada; decisiones de alcance registradas; sin afirmar capacidades no verificadas.

### Fase 1 — Cerrar la base multi-tenant y el control de acceso

**Características:** 1 y 2.

**Trabajo**
- Completar escenarios integrales con dos organizaciones: acceso permitido dentro del tenant y denegación de lecturas, escrituras, reportes y exportaciones cruzadas.
- Verificar creación de usuarios, membresías, grupos, miembros, roles heredados y privilegios de componentes desde el flujo de administración.
- Confirmar que una denegación UI también se aplica en el endpoint y que los cambios de tenant limpian/cargan correctamente la política en Web y Móvil.
- Mantener el alcance universal acotado a la gestión de organizaciones de eventos.

**Aceptación**
- Demo con dos tenants y usuario con membresías distintas.
- Pruebas HTTP con aislamiento real sobre PostgreSQL o entorno equivalente, incluidos recursos inexistentes en el tenant activo.
- Cobertura de permisos backend y privilegios de interfaz en pantallas principales Web y Móvil.

### Fase 2 — Backup y restore

**Característica:** 6.

**Trabajo**
- Definir estrategia por ambiente: respaldos automáticos y snapshots administrados para AWS/RDS; backup lógico y restauración controlada para desarrollo/local.
- Definir periodicidad, retención, cifrado, responsables, RPO y RTO.
- Preparar comandos/runbook versionados para respaldar, restaurar en un destino aislado y verificar consistencia sin sobrescribir producción.
- Asegurar que los respaldos incluyan base de datos y que artefactos/configuración necesarios para recuperación estén inventariados sin guardar secretos en el repositorio.
- Probar recuperación y documentar limitaciones sobre datos cifrados y disponibilidad de KMS.

**Aceptación**
- Backup generado y verificado; restauración exitosa en entorno aislado.
- Prueba de integridad posterior (migraciones, tenants, usuarios y registros de muestra).
- Política de retención/RPO/RTO y evidencia reproducible; ninguna prueba destructiva sobre producción.

### Fase 3 — Reportes configurables

**Característica:** 5.

**Trabajo**
- Diseñar un modelo acotado de reportes sobre entidades permitidas, con selección de columnas, filtros, orden y paginación.
- Implementar validación de campos/operadores permitidos en backend; no aceptar SQL arbitrario desde el cliente.
- Reutilizar autorización y tenant activo en vista previa, ejecución y exportación.
- Priorizar formatos por decisión de Fase 0. Secuencia sugerida: CSV compatible con Excel, XLSX, PDF, HTML y envío por correo; separar email de la generación del reporte.
- Añadir interfaz de constructor y previsualización en Web; definir capacidades móviles solo si los casos de uso de campo las justifican.

**Aceptación**
- Usuario configura columnas/filtros/orden, previsualiza y exporta al formato acordado.
- Pruebas de permisos, límites de tamaño, filtros inválidos e aislamiento entre tenants.
- CSV existente de participantes conserva compatibilidad durante la transición.

### Fase 4 — Usabilidad y asistencia

**Característica:** 4.

**Trabajo**
- Auditar los flujos de ingreso, eventos, inscripciones, staff, participantes, grupos, bitácora y reportes.
- Estandarizar validación visible, mensajes accionables, carga/vacío/error, confirmación de acciones críticas y navegación accesible.
- Añadir ayuda contextual mínima (guía/FAQ integrada y enlaces desde los flujos); evitar que la asistencia dependa de servicios externos.
- Hacer una evaluación breve con usuarios y registrar observaciones y correcciones.

**Aceptación**
- Checklist completado por flujo en Web y Móvil.
- Tareas de prueba completadas sin bloqueo; errores y recuperación entendibles; controles esenciales utilizables por teclado/lector de pantalla donde corresponda.
- Ayuda disponible desde las pantallas principales.

### Fase 5 — Alcance Web/Móvil y consistencia funcional

**Característica:** 7.

**Trabajo**
- Crear matriz CU × plataforma (Web, Móvil o ambas), indicando operación de administración versus operación de campo.
- Alinear implementación con la matriz; corregir diferencias entre documentación y pantallas existentes.
- Verificar que ambas plataformas usen la misma API, reglas de autorización, tenant activo y estados de negocio.
- No duplicar en móvil herramientas administrativas que sean más adecuadas para Web sin una necesidad de campo demostrada.

**Aceptación**
- Matriz aprobada y coincidente con rutas, pantallas y endpoints reales.
- Flujos asignados a ambas plataformas producen resultados coherentes con la misma cuenta/tenant.
- Pruebas de sesión, cambio de organización y permisos en ambas aplicaciones.

### Fase 6 — Cierre de confidencialidad de bitácora

**Característica:** 3.

**Trabajo**
- Revisar el requisito literal del PDF y acordar el modelo de amenaza y quién controla la llave; documentar específicamente el límite frente a administradores de nube/IAM.
- Ejecutar pruebas de cifrado/descifrado, índices de búsqueda, autorización y ausencia de payload en claro en nuevas filas.
- Planificar backfill histórico en lotes con observabilidad, reanudación, dry-run y verificación; probar primero sobre copia restaurada.
- Definir tratamiento de WAL, réplicas, snapshots y respaldos antiguos; la migración de filas actuales por sí sola no elimina copias históricas.
- Activar la operación solo con claves y permisos correctamente configurados por ambiente.

**Aceptación**
- Administrador de BD puede consultar metadatos mínimos pero no reconstruir el payload sin la capacidad criptográfica autorizada.
- Lectura vía aplicación autorizada y filtros exactos pasan pruebas.
- Backfill demostrado sobre entorno de ensayo y plan de copias históricas aprobado; claves no aparecen en código ni logs.

### Fase 7 — SaaS operativo en nube

**Característica:** 8; consolida también 1.

**Trabajo**
- Definir el ciclo de suscripción: alta de cliente/organización, estado de suscripción, vigencia, límites/entitlements y suspensión/reactivación. Decidir si cobro será manual o con proveedor; no integrar pagos sin alcance aprobado.
- Completar configuración cloud: secretos en gestor seguro, HTTPS/certificado, health checks, logs/alertas, despliegue repetible y rollback.
- Desplegar backend, web, base de datos y servicios auxiliares necesarios en ambiente de prueba; validar tenant y suscripción desde el flujo cliente.
- Verificar respaldos/restauración, rotación/uso de KMS y monitoreo antes de considerar producción.

**Aceptación**
- URL de prueba HTTPS funcional y health checks observables.
- Alta de dos organizaciones con aislamiento comprobado; acceso condicionado al estado de suscripción acordado.
- Runbook de despliegue/rollback, secretos protegidos y backup/restore del ambiente de prueba verificado.
- Producción solo después de autorización expresa y acceso válido a cuenta/dominio.

### Fase 8 — Validación final y demostración

**Características:** las ocho.

**Trabajo**
- Ejecutar suites backend, frontend y móvil disponibles; build de producción; validación de Compose; pruebas de aislamiento y restauración.
- Recorrer demostración end-to-end: crear organización, usuario/grupo, asignar privilegios, operar un evento, consultar bitácora, generar reporte, restaurar respaldo y comprobar estado SaaS.
- Actualizar matriz con estado `covered`, `partial`, `missing` o `unclear`, evidencia, fecha y brechas restantes.
- Preparar evidencia de demostración separada del código de producto.

**Aceptación**
- Cada característica tiene criterio reproducible y evidencia enlazada.
- No se declara `covered` una característica que dependa de un despliegue o decisión aún no realizada.
- Sin fallos críticos de seguridad, aislamiento, recuperación o autorización.

## Orden y dependencias

`Fase 0 → Fase 1 → Fase 2 → Fase 3 → Fase 4 → Fase 5 → Fase 6 → Fase 7 → Fase 8`.

- Fase 1 habilita pruebas confiables para reportes y SaaS multi-tenant.
- Fase 2 debe estar definida antes de almacenar/rotar claves y datos cifrados a largo plazo; su prueba de restore también sirve como base segura para el backfill de Fase 6.
- Fase 3 depende de permisos y aislamiento de Fase 1.
- Fases 4 y 5 pueden avanzar en paralelo una vez establecidos los flujos objetivo en Fase 0.
- Fase 7 depende de decisiones de suscripción, respaldo y secretos; no requiere desplegar producción para validar el entorno de prueba.

## Primer incremento recomendado

Comenzar con **Fase 0** breve y luego **Fase 1**, para actualizar evidencia de lo recién integrado y dejar un escenario automatizado de dos tenants. A continuación implementar **Fase 2 (backup/restore)**; es la brecha completa más clara y su restauración proporciona una copia segura para validar el backfill de bitácora. Después seguir con reportes, usabilidad, alcance Web/Móvil, cierre de bitácora y despliegue SaaS.

## Fuera de alcance hasta aprobación específica

- Despliegue o modificación de recursos AWS productivos.
- Cobros reales, integración de pagos o compra de dominio/certificados.
- Ejecución del backfill histórico en bases de datos productivas.
- Restauraciones destructivas o pruebas que sobrescriban datos existentes.
