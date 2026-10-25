# Requisitos, Casos de Uso y Backlog Actualizado

Este documento consolida los requisitos del producto después del Sprint 1 y antes de iniciar el Sprint 2. Los identificadores existentes se conservan para no romper las referencias del documento académico. Los elementos nuevos comienzan desde `RF30` y `CU32`.

## 1. Requisitos Funcionales

| Código | Requisito funcional |
|---|---|
| RF1 | Gestionar el perfil del usuario autenticado. |
| RF2 | Administrar usuarios del sistema. |
| RF3 | Gestionar autenticación, cierre de sesión y recuperación de acceso. |
| RF4 | Gestionar roles del sistema. |
| RF5 | Gestionar permisos y asignarlos a roles. |
| RF6 | Gestionar eventos y categorías. |
| RF7 | Consultar, buscar y filtrar eventos. |
| RF8 | Gestionar clasificación y estados de eventos. |
| RF9 | Gestionar espacios, salas, fechas, horarios y programación. |
| RF10 | Gestionar participantes e inscripciones. |
| RF11 | Controlar cupos y aforo de eventos. |
| RF12 | Cancelar inscripciones según las reglas del evento. |
| RF13 | Gestionar convocatorias para Staff y voluntarios. |
| RF14 | Permitir la postulación, revisión, selección y rechazo de personal de apoyo. |
| RF15 | Asignar turnos y tareas al Staff o voluntariado. |
| RF16 | Gestionar recursos materiales y tecnológicos. |
| RF17 | Asignar recursos a eventos y salas. |
| RF18 | Generar credenciales digitales con código QR. |
| RF19 | Validar códigos QR y controlar el acceso. |
| RF20 | Registrar y consultar la asistencia. |
| RF21 | Gestionar recordatorios y notificaciones automáticas. |
| RF22 | Recomendar eventos mediante Inteligencia Artificial. |
| RF23 | Generar certificados digitales de asistencia. |
| RF24 | Proporcionar funcionalidades diferenciadas para web y móvil. |
| RF25 | Consultar el historial de participación. |
| RF26 | Permitir el escaneo móvil de códigos QR por personal autorizado. |
| RF27 | Generar reportes estadísticos de eventos, participantes, inscripciones y asistencia. |
| RF28 | Mostrar un dashboard de indicadores generales. |
| RF29 | Registrar y consultar operaciones relevantes mediante bitácora y auditoría. |
| RF30 | Crear y administrar grupos de usuarios. |
| RF31 | Asignar usuarios a grupos y consultar sus integrantes. |
| RF32 | Crear reportes personalizados seleccionando columnas, filtros y ordenamiento. |
| RF33 | Exportar reportes en formatos autorizados como CSV, Excel, PDF o HTML. |
| RF34 | Generar respaldos de la información del sistema. |
| RF35 | Restaurar respaldos mediante una operación autorizada y verificable. |
| RF36 | Administrar organizaciones o cuentas cliente de la plataforma. |
| RF37 | Aislar la información, usuarios, eventos y recursos entre organizaciones cliente. |
| RF38 | Consultar el estado y resultado de operaciones de respaldo y restauración. |
| RF39 | Administrar preferencias de usuario relacionadas con notificaciones y recomendaciones. |

## 2. Requisitos No Funcionales

| Código | Requisito no funcional |
|---|---|
| RNF1 | El sistema debe proteger las operaciones mediante autenticación y autorización en el backend. |
| RNF2 | Las comunicaciones productivas deben utilizar HTTPS y certificados válidos. |
| RNF3 | El sistema debe mantener disponibilidad mediante health checks y recuperación controlada. |
| RNF4 | Las consultas grandes deben utilizar paginación, filtros e índices adecuados. |
| RNF5 | La arquitectura debe permitir aumentar la capacidad de procesamiento y usuarios. |
| RNF6 | La interfaz debe ser clara, responsive y usable sin capacitación especializada. |
| RNF7 | Las funciones deben distribuirse de forma explícita entre plataforma web y aplicación móvil. |
| RNF8 | La base de datos debe preservar integridad referencial, consistencia y control de concurrencia. |
| RNF9 | Angular y Flutter deben consumir la misma API REST sin acceder directamente a PostgreSQL. |
| RNF10 | El sistema debe mantener arquitectura modular, documentación técnica y pruebas automatizadas. |
| RNF11 | Debe existir una política de backup, retención, restauración, RPO y RTO. |
| RNF12 | El sistema debe ser desplegable mediante contenedores en infraestructura cloud. |
| RNF13 | La bitácora debe protegerse contra modificación o eliminación por usuarios no autorizados. |
| RNF14 | Las exportaciones y reportes deben respetar los permisos del usuario y la organización. |
| RNF15 | Los secretos productivos no deben almacenarse en el repositorio ni en archivos públicos. |
| RNF16 | La interfaz debe ofrecer validaciones, estados de carga, mensajes claros y confirmaciones para operaciones críticas. |
| RNF17 | La solución SaaS debe impedir el acceso de una organización a datos de otra organización. |

## 3. Actores

| Actor | Responsabilidad |
|---|---|
| Administrador | Administra usuarios, seguridad, grupos, configuración, auditoría, reportes y operación del sistema. |
| Organizador | Administra eventos, recursos, convocatorias, postulaciones, asignaciones y reportes de sus eventos. |
| Staff | Consulta convocatorias y asignaciones y ejecuta las funciones operativas autorizadas. |
| Voluntario | Se postula y consulta las asignaciones autorizadas. |
| Participante | Consulta eventos, se inscribe, cancela inscripciones y consulta su participación. |
| Administrador técnico | Ejecuta o supervisa backups, restauraciones y operaciones de recuperación. |
| Administrador de organización | Administra usuarios, eventos y recursos dentro de una organización asignada. |
| Sistema | Controla cupos, registra auditoría, envía notificaciones y aplica reglas automáticas. |

## 4. Casos de Uso por Sprint

### Sprint 0: Identidad y Seguridad

| ID | Caso de uso | Actor principal | Plataforma |
|---|---|---|---|
| CU1 | Gestionar perfil de usuario | Usuario autenticado | Web/Móvil |
| CU2 | Administrar usuarios del sistema | Administrador | Web |
| CU3 | Iniciar y cerrar sesión | Usuario | Web/Móvil |
| CU4 | Recuperar acceso al sistema | Usuario | Web/Móvil |
| CU5 | Gestionar roles y permisos | Administrador | Web |

### Sprint 1: Eventos y Organización

| ID | Caso de uso | Actor principal | Plataforma |
|---|---|---|---|
| CU6 | Gestionar eventos y categorías | Administrador/Organizador | Web |
| CU7 | Consultar y buscar eventos | Participante/Usuario | Web/Móvil |
| CU8 | Gestionar espacios, salas y programación | Administrador/Organizador | Web |
| CU9 | Asignar responsables a eventos | Administrador/Organizador | Web |
| CU15 | Gestionar convocatorias de Staff y voluntarios | Administrador/Organizador | Web |
| CU19 | Registrar y consultar recursos | Administrador/Organizador | Web |
| CU20 | Asignar recursos a eventos y salas | Administrador/Organizador | Web |

### Sprint 2: SaaS, Multi-tenancy, Participantes, Inscripciones y Staff

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
| CU38 | Administrar organizaciones cliente | Administrador SaaS | Web |
| CU40 | Gestionar pertenencia de usuarios a una organización | Administrador SaaS | Web |
| CU31 | Consultar bitácora y auditoría | Administrador/Auditor | Web |

### Sprint 3: QR, Asistencia, Notificaciones e Historial

| ID | Caso de uso | Actor principal | Plataforma |
|---|---|---|---|
| CU21 | Generar y visualizar credencial digital con QR | Participante | Móvil/Web |
| CU22 | Escanear y validar código QR | Staff autorizado | Móvil |
| CU23 | Registrar asistencia | Staff autorizado/Sistema | Móvil/Web |
| CU24 | Consultar asistencia en tiempo real | Administrador/Organizador | Web |
| CU25 | Gestionar recordatorios y notificaciones | Administrador/Sistema | Web/Móvil |
| CU27 | Consultar historial de participación | Participante | Móvil/Web |

### Sprint 4: IA, Certificados, Reportes Avanzados y Continuidad Operativa

| ID | Caso de uso | Actor principal | Plataforma |
|---|---|---|---|
| CU26 | Consultar recomendaciones personalizadas de eventos | Participante | Móvil |
| CU28 | Generar certificado digital de asistencia | Administrador/Sistema | Web |
| CU29 | Consultar reportes estadísticos | Administrador/Organizador | Web |
| CU30 | Consultar dashboard de indicadores | Administrador/Organizador | Web |
| CU32 | Gestionar grupos de usuarios | Administrador | Web |
| CU33 | Asignar usuarios a grupos | Administrador | Web |
| CU34 | Crear y configurar reportes personalizados | Administrador/Organizador | Web |
| CU35 | Exportar reportes autorizados | Administrador/Organizador | Web |
| CU36 | Generar respaldo del sistema | Administrador técnico | Web/Operación |
| CU37 | Restaurar respaldo y verificar resultado | Administrador técnico | Web/Operación |
| CU39 | Configurar preferencias de usuario | Participante | Web/Móvil |

El registro automático de auditoría no se ejecuta como una acción manual independiente. Es una regla transversal que debe aplicarse a usuarios, roles, eventos, inscripciones, postulaciones, asignaciones, reportes, backups y restauraciones.

## 5. Product Backlog Completo

### Sprint 0

| ID | Tarea | Tipo | Prioridad | Estado |
|---|---|---|---|---|
| SP0-1 | Entrevistar al Product Owner e identificar necesidades | Análisis | Alta | Terminado |
| SP0-2 | Elaborar el perfil y finalidad del proyecto | Análisis | Alta | Terminado |
| SP0-3 | Definir el problema y las necesidades | Análisis | Alta | Terminado |
| SP0-4 | Definir y asignar roles Scrum | Planificación | Media | Terminado |
| SP0-5 | Elaborar el Product Backlog inicial | Planificación | Alta | Terminado |
| SP0-6 | Identificar actores principales | Análisis | Alta | Terminado |
| SP0-7 | Identificar casos de uso principales | Análisis | Alta | Terminado |
| SP0-8 | Elaborar prototipos iniciales web y móvil | Diseño | Media | Terminado |
| SP0-9 | Definir arquitectura inicial | Diseño | Alta | Terminado |
| SP0-10 | Configurar repositorios Git y GitHub | Configuración | Alta | Terminado |
| SP0-11 | Preparar entornos Spring Boot, Angular, Flutter, PostgreSQL y FastAPI | Configuración | Alta | Terminado |
| SP0-12 | Realizar modelo inicial de datos | Diseño de datos | Alta | Terminado |
| SP0-13 | Crear y configurar PostgreSQL | Base de datos | Alta | Terminado |
| SP0-14 | Diseñar interfaz de inicio de sesión | Diseño | Alta | Terminado |
| SP0-15 | Implementar autenticación e inicio de sesión | Desarrollo | Alta | Terminado |
| SP0-16 | Diseñar interfaces de usuarios y perfil | Diseño | Alta | Terminado |
| SP0-17 | Implementar registro y administración de usuarios | Desarrollo | Alta | Terminado |
| SP0-18 | Implementar roles y permisos | Desarrollo | Alta | Terminado |
| SP0-19 | Probar autenticación, usuarios, roles y permisos | Pruebas | Alta | Terminado |

### Sprint 1

| ID | Tarea | Tipo | Prioridad | Estado |
|---|---|---|---|---|
| SP1-1 | Diseñar interfaces de eventos y categorías | Diseño | Alta | Terminado |
| SP1-2 | Implementar eventos y categorías | Desarrollo | Alta | Terminado |
| SP1-3 | Probar eventos y categorías | Pruebas | Alta | Terminado |
| SP1-4 | Diseñar consulta, búsqueda y detalle de eventos | Diseño | Alta | Terminado |
| SP1-5 | Implementar consulta y búsqueda de eventos | Desarrollo | Alta | Terminado |
| SP1-6 | Probar consulta y búsqueda de eventos | Pruebas | Alta | Terminado |
| SP1-7 | Diseñar espacios, salas y programación | Diseño | Alta | Terminado |
| SP1-8 | Implementar espacios, fechas y horarios | Desarrollo | Alta | Terminado |
| SP1-9 | Validar disponibilidad y conflictos de programación | Desarrollo | Alta | Terminado |
| SP1-10 | Probar espacios y programación | Pruebas | Alta | Terminado |
| SP1-11 | Diseñar asignación de responsables | Diseño | Media | Terminado |
| SP1-12 | Implementar asignación y consulta de responsables | Desarrollo | Alta | Terminado |
| SP1-13 | Probar asignación de responsables | Pruebas | Alta | Terminado |
| SP1-14 | Diseñar convocatorias de Staff y voluntarios | Diseño | Media | Terminado |
| SP1-15 | Implementar gestión de convocatorias | Desarrollo | Alta | Terminado |
| SP1-16 | Probar gestión de convocatorias | Pruebas | Alta | Terminado |
| SP1-17 | Diseñar registro y consulta de recursos | Diseño | Alta | Terminado |
| SP1-18 | Implementar registro, actualización y consulta de recursos | Desarrollo | Alta | Terminado |
| SP1-19 | Implementar consulta de disponibilidad de recursos | Desarrollo | Alta | Terminado |
| SP1-20 | Diseñar asignación de recursos | Diseño | Alta | Terminado |
| SP1-21 | Implementar asignación de recursos | Desarrollo | Alta | Terminado |
| SP1-22 | Probar gestión y asignación de recursos | Pruebas | Alta | Terminado |

### Sprint 2

| ID | Tarea | Tipo | Prioridad | Estado |
|---|---|---|---|---|
| SP2-1 | Diseñar interfaz de gestión de participantes | Diseño | Alta | Pendiente |
| SP2-2 | Implementar registro y administración de participantes | Desarrollo | Alta | Pendiente |
| SP2-3 | Probar gestión de participantes | Pruebas | Alta | Pendiente |
| SP2-4 | Diseñar interfaz web y móvil de inscripciones | Diseño | Alta | Completado |
| SP2-5 | Implementar inscripción a eventos | Desarrollo | Alta | Completado |
| SP2-6 | Consultar estado de inscripciones | Desarrollo | Alta | Completado |
| SP2-7 | Cancelar inscripciones | Desarrollo | Alta | Completado |
| SP2-8 | Controlar cupos, aforo y duplicados | Desarrollo | Alta | Completado |
| SP2-9 | Probar inscripción, cancelación y cupos | Pruebas | Alta | Completado |
| SP2-10 | Diseñar interfaz móvil de postulación | Diseño | Alta | Completado |
| SP2-11 | Implementar postulación a Staff y voluntariado | Desarrollo | Alta | Completado |
| SP2-12 | Probar postulaciones | Pruebas | Alta | Completado |
| SP2-13 | Diseñar consulta y selección de postulantes | Diseño | Alta | Completado |
| SP2-14 | Implementar selección y rechazo de postulantes | Desarrollo | Alta | Completado |
| SP2-15 | Probar selección y rechazo | Pruebas | Alta | Completado |
| SP2-16 | Diseñar asignación de turnos y tareas | Diseño | Alta | Completado |
| SP2-17 | Implementar asignación de Staff, turnos y tareas | Desarrollo | Alta | Completado |
| SP2-18 | Probar asignaciones | Pruebas | Alta | Completado |
| SP2-19 | Actualizar RF, RNF, CU, reglas de negocio y trazabilidad | Análisis | Alta | Completado |
| SP2-20 | Validar el estado real del incremento de Sprint 1 | Análisis | Alta | Completado |
| SP2-21 | Diseñar el modelo de organizaciones, membresías y tenant activo | Diseño de datos | Alta | Completado |
| SP2-22 | Implementar organizaciones cliente y estado de organización | Desarrollo | Alta | Completado |
| SP2-23 | Implementar pertenencia de usuarios a organizaciones | Desarrollo | Alta | Completado |
| SP2-24 | Resolver la organización desde sesión o token autenticado | Seguridad | Alta | Completado |
| SP2-25 | Aplicar aislamiento por organización en consultas y escrituras | Seguridad | Alta | Completado |
| SP2-26 | Migrar eventos, recursos, convocatorias e inscripciones al contexto de organización | Migración | Alta | Completado |
| SP2-27 | Definir permisos de inscripción, postulación, selección, asignación y tenant | Seguridad | Alta | Completado |
| SP2-28 | Diseñar e implementar bitácora transversal incluyendo organización | Desarrollo | Alta | Completado |
| SP2-29 | Implementar consulta y filtros de bitácora por organización | Desarrollo | Alta | Completado |
| SP2-31 | Probar aislamiento entre organizaciones | Pruebas | Alta | Completado |
| SP2-32 | Probar permisos, bitácora, inscripciones y asignaciones | Pruebas | Alta | Completado |
| SP2-33 | Integrar funciones web, móvil, backend y base de datos | Integración | Alta | Completado |
| SP2-34 | Actualizar OpenAPI, Postman y documentación | Documentación | Alta | Completado |
| SP2-35 | Preparar evidencia del Sprint Review | Gestión | Alta | Pendiente |

### Sprint 3

| ID | Tarea | Tipo | Prioridad | Estado |
|---|---|---|---|---|
| SP3-1 | Diseñar credencial digital con QR | Diseño | Alta | Pendiente |
| SP3-2 | Generar códigos QR asociados a inscripciones | Desarrollo | Alta | Pendiente |
| SP3-3 | Visualizar credencial QR | Desarrollo | Alta | Pendiente |
| SP3-4 | Probar generación y visualización QR | Pruebas | Alta | Pendiente |
| SP3-5 | Diseñar escaneo y validación QR | Diseño | Alta | Pendiente |
| SP3-6 | Implementar escaneo y validación QR | Desarrollo | Alta | Pendiente |
| SP3-7 | Probar control de acceso QR | Pruebas | Alta | Pendiente |
| SP3-8 | Registrar asistencia | Desarrollo | Alta | Pendiente |
| SP3-9 | Diseñar seguimiento web de asistencia | Diseño | Alta | Pendiente |
| SP3-10 | Consultar asistencia en tiempo real | Desarrollo | Alta | Pendiente |
| SP3-11 | Probar asistencia | Pruebas | Alta | Pendiente |
| SP3-12 | Diseñar recordatorios y notificaciones | Diseño | Alta | Pendiente |
| SP3-13 | Implementar notificaciones de inscripción y confirmación | Desarrollo | Alta | Pendiente |
| SP3-14 | Implementar recordatorios, cambios y cancelaciones | Desarrollo | Alta | Pendiente |
| SP3-15 | Probar notificaciones | Pruebas | Alta | Pendiente |
| SP3-16 | Diseñar historial de participación | Diseño | Media | Pendiente |
| SP3-17 | Implementar historial de inscripciones y asistencias | Desarrollo | Alta | Pendiente |
| SP3-18 | Probar historial | Pruebas | Alta | Pendiente |

### Sprint 4

| ID | Tarea | Tipo | Prioridad | Estado |
|---|---|---|---|---|
| SP4-1 | Diseñar interfaz de recomendaciones | Diseño | Alta | Pendiente |
| SP4-2 | Preparar datos para recomendaciones | Datos | Alta | Pendiente |
| SP4-3 | Implementar lógica de recomendación IA | Desarrollo | Alta | Pendiente |
| SP4-4 | Implementar servicio FastAPI de recomendaciones | Desarrollo | Alta | Pendiente |
| SP4-5 | Integrar IA con Spring Boot y Flutter | Integración | Alta | Pendiente |
| SP4-6 | Probar recomendaciones | Pruebas | Alta | Pendiente |
| SP4-7 | Diseñar certificado digital | Diseño | Media | Pendiente |
| SP4-8 | Generar certificados de asistencia | Desarrollo | Alta | Pendiente |
| SP4-9 | Probar certificados | Pruebas | Alta | Pendiente |
| SP4-10 | Diseñar reportes estadísticos avanzados | Diseño | Alta | Pendiente |
| SP4-11 | Implementar reportes de eventos, participantes, inscritos y asistentes | Desarrollo | Alta | Pendiente |
| SP4-12 | Implementar indicadores de asistencia, categorías, estados y participación | Desarrollo | Alta | Pendiente |
| SP4-13 | Probar reportes e indicadores | Pruebas | Alta | Pendiente |
| SP4-14 | Diseñar dashboard general | Diseño | Alta | Pendiente |
| SP4-15 | Implementar dashboard general | Desarrollo | Alta | Pendiente |
| SP4-16 | Probar dashboard | Pruebas | Alta | Pendiente |
| SP4-17 | Diseñar operación de backups | Infraestructura | Alta | Pendiente |
| SP4-18 | Configurar backups automáticos y retención | Infraestructura | Alta | Pendiente |
| SP4-19 | Implementar generación de respaldo | Desarrollo | Alta | Pendiente |
| SP4-20 | Implementar restauración y verificación | Desarrollo | Alta | Pendiente |
| SP4-21 | Probar backup y restore | Pruebas | Alta | Pendiente |
| SP4-22 | Configurar HTTPS, secretos protegidos y RDS privado | Infraestructura | Alta | Pendiente |
| SP4-23 | Configurar monitoreo, rollback y despliegue seguro | Infraestructura | Alta | Pendiente |
| SP4-24 | Ejecutar pruebas de integración completas | Pruebas | Alta | Pendiente |
| SP4-25 | Corregir incidencias de integración | Corrección | Alta | Pendiente |
| SP4-26 | Validar incremento final con el Product Owner | Gestión | Alta | Pendiente |

## 6. Trazabilidad resumida

| Característica de la docente | Requisitos | Casos de uso | Sprint principal |
|---|---|---|---|
| Solución universal | RF36-RF37, RNF5, RNF17 | CU38, CU40 | Sprint 2 |
| Usuarios, grupos y privilegios | RF2-RF5, RF30-RF31, RNF1 | CU2, CU5, CU32, CU33 | Sprint 0 y Sprint 4 |
| Log y bitácora | RF29, RNF13 | CU31 | Sprint 2 |
| Facilidad de uso | RNF6, RNF16 | Todos | Todos |
| Reportes personalizados | RF27-RF28, RF32-RF33, RNF14 | CU29, CU30, CU34, CU35 | Sprint 4 |
| Backup y restore | RF34-RF35, RF38, RNF11 | CU36, CU37 | Sprint 4 |
| Web y móvil | RF24-RF26, RNF7, RNF9 | CU7, CU11-CU14, CU16, CU21-CU27 | Sprint 2 y Sprint 3 |
| SaaS en la nube | RF36-RF37, RNF2, RNF3, RNF5, RNF12, RNF15, RNF17 | CU38, CU40 | Sprint 2 |

## 7. Criterio de aceptación del Sprint 2

El Sprint 2 se considerará completo cuando exista una organización cliente funcional, los usuarios pertenezcan a una organización, los datos queden aislados entre organizaciones, y las inscripciones, postulaciones y asignaciones funcionen respetando permisos y cupos. Las operaciones relevantes deben registrarse en bitácora y contar con pruebas de aislamiento, integración y documentación actualizada.
