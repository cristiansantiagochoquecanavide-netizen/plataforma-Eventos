# Revisión del CU16

## Resultado

Revisión del backend y de la aplicación Flutter completada. Se corrigieron defectos del flujo existente; no se añadieron funcionalidades ni se modificaron migraciones.

## Problemas corregidos

1. **Postulación y cierre concurrentes.** La lectura que valida la convocatoria ahora adquiere un bloqueo JPA `PESSIMISTIC_READ` dentro de la transacción de postulación. Un cierre no puede confirmar su actualización entre esa validación y el guardado. La restricción UNIQUE sigue protegiendo las postulaciones simultáneas del mismo usuario.
2. **JWT de usuario inexistente.** El filtro captura `UsernameNotFoundException` y deja que Spring Security responda 401, en lugar de propagar un error interno.
3. **Renovación JWT duplicada.** Se unificó el tratamiento de llamadas autenticadas en `SessionController`. Las peticiones concurrentes comparten una renovación, necesaria porque el backend rota el token de refresco. Un error de negocio después de renovar no cierra la sesión. Un fallo temporal de red tampoco elimina las credenciales; un refresco inválido sí lo hace. Una respuesta tardía de renovación no restaura una sesión cerrada.
4. **Clientes HTTP sin cerrar.** El envío de solicitudes utiliza un ayudante compartido que cierra el cliente en `finally`, también ante errores o timeout.
5. **Pérdida de cambios del perfil.** La navegación conserva las pantallas con `IndexedStack`; convocatorias se carga al visitar la pestaña por primera vez.
6. **Detalle oculto ante falta de permiso de postulación.** La información de la convocatoria se conserva aunque falle la consulta de la postulación. Las acciones quedan deshabilitadas cuando no se pudo confirmar el estado, incluido el retiro.

## Elementos verificados

- `PostulacionStaff`: dos relaciones `ManyToOne` obligatorias y lazy, sin cascadas de borrado, claves de identidad y enum textual coherentes con SQL. Fechas generadas con callbacks JPA.
- V9: nombres de tabla y columnas coincidentes con la entidad; referencias a `convocatoria_personal` y `usuario`; UNIQUE por convocatoria y usuario; estados `PENDIENTE` y `RETIRADA`; índice para consultas del propietario.
- Flyway: las nueve migraciones se aplican sobre PostgreSQL vacío. No se modificaron V1–V9 ni sus contratos.
- Permisos: `CONVOCATORIA_POSTULARSE` se asigna a STAFF y PARTICIPANTE; V7 ya proporciona `CONVOCATORIA_VER`. Los endpoints comprueban permisos en el servidor.
- Identidad: procede de `Authentication.getName()`; un identificador de usuario enviado por el cliente no decide el propietario.
- Validaciones: usuario activo, convocatoria existente y abierta, fecha límite inclusiva, duplicados incluso después del retiro y retiro limitado al propietario.
- DTOs y modelos Dart: nombres y tipos coincidentes; las listas, objetos y el 204 sin postulación tienen tratamientos diferentes en el cliente.
- Pantallas: lista de convocatorias abiertas, detalle, confirmación, consulta de estado y retiro. Se bloquea el envío repetido y se actualiza el estado después de un error de duplicado.
- Arquitectura: se conservan las convenciones JPA/servicios transaccionales/DTO record y StatefulWidget/SessionController/ApiClient. No se realizó una reorganización global de `main.dart`.

## Archivos modificados en esta revisión

Backend:

- `backend/src/main/java/com/plataformaeventos/convocatorias/ConvocatoriaRepository.java`
- `backend/src/main/java/com/plataformaeventos/convocatorias/postulaciones/PostulacionStaffService.java`
- `backend/src/main/java/com/plataformaeventos/seguridad/JwtFiltro.java`
- `backend/src/test/java/com/plataformaeventos/convocatorias/postulaciones/PostulacionStaffServiceTest.java`
- `backend/src/test/java/com/plataformaeventos/convocatorias/postulaciones/PostulacionStaffControllerTest.java`
- `backend/src/test/java/com/plataformaeventos/convocatorias/postulaciones/PostulacionStaffIntegracionTest.java` (nuevo)

Móvil:

- `aplicacion-movil/lib/main.dart`
- `aplicacion-movil/lib/convocatorias/convocatorias_home.dart`
- `aplicacion-movil/lib/convocatorias/pages/detalle_convocatoria_page.dart`
- `aplicacion-movil/test/convocatorias_test.dart`

Documentación: `CU16_REVISION.md` (este informe).

## Funcionalidades existentes conservadas

- Consultar convocatorias abiertas y su detalle.
- Postularse con el usuario del JWT.
- Consultar la postulación propia y listar las propias mediante API.
- Mostrar confirmación y estado de la postulación.
- Retirar una postulación pendiente conservando su historial, sin permitir una nueva postulación a la misma convocatoria.

## Pruebas realizadas

- Maven `-o -B package`: **48 pruebas, 0 fallos, 0 errores, 0 omitidas**, compilación y empaquetado correctos.
- Dos pruebas de integración con `CU16_INTEGRACION=true` y una instancia temporal de PostgreSQL: arranque completo de Spring, Flyway V1–V9, validación Hibernate, permisos persistidos, alta/consulta/listado/retiro reales, rechazo UNIQUE y bloqueo frente a un cierre concurrente. La prueba de bloqueo comprueba además que el cierre puede ejecutarse después de terminar la transacción de lectura.
- Flutter `test --no-pub`: **17 pruebas aprobadas**, incluidas las existentes de login y perfil. Regresiones nuevas para fallo de red al renovar, renovación concurrente, detalle con 403 y conservación del formulario entre pestañas.
- Dart `analyze lib test`: **sin observaciones**.

Las pruebas de integración requieren `CU16_INTEGRACION=true` y una base PostgreSQL desechable configurada con las variables habituales de la aplicación. Sin esa variable se omiten para no usar accidentalmente la base habitual. No se ejecutó una prueba manual en un teléfono físico.
