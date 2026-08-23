# Cierre de Sprint 0

## Alcance entregado

Sprint 0 implementa exclusivamente identidad y control de acceso:

| Dominio | Entregables |
|---|---|
| Seguridad | BCrypt, JWT de acceso, refresh tokens hasheados, rotacion, revocacion y CORS configurable |
| Autenticacion | Registro de participante, login, logout, refresh y usuario actual |
| Recuperacion | Token de un solo uso con expiracion, correo SMTP y restablecimiento de clave |
| Perfil | Consulta, actualizacion y cambio de clave propios |
| Usuarios | Listado paginado, creacion, edicion, roles y estados |
| Roles | Consulta, creacion y edicion de roles personalizados |
| Permisos | Catalogo y asignacion idempotente por rol |
| Clientes | Angular administrativo, Flutter para cuenta propia y FastAPI de estado |
| Operacion | Docker Compose para PostgreSQL, Spring Boot, Angular y FastAPI |

No se incluyeron tablas ni funcionalidades de eventos, inscripciones, QR, asistencia, notificaciones, reportes, certificados o inteligencia artificial.

## Evidencias verificables

| Comprobacion | Comando o recurso | Resultado esperado |
|---|---|---|
| Backend | `backend/mvnw.cmd test` | Pruebas Spring Boot exitosas |
| Panel web | `npm run build` desde `frontend-web` | Bundle Angular en `dist/frontend-web` |
| Movil | `flutter analyze` y `flutter test` desde `aplicacion-movil` | Sin incidencias y pruebas exitosas |
| FastAPI | `GET /api/salud` | `200` y `servicio-ia` |
| Integracion | `docker compose up --build` | Cuatro servicios saludables |
| API | Coleccion en `documentacion/postman` | Flujos de autenticacion y administracion ejecutables |

## Coleccion Postman

Importar los dos archivos de `documentacion/postman` en Postman y seleccionar el entorno `Plataforma Eventos - Local`.

1. Iniciar los servicios mediante Docker o iniciar el backend localmente.
2. Configurar `adminClave` con el valor de `ADMIN_CLAVE` usado en el backend.
3. Ejecutar `Iniciar sesion administrador`; la coleccion guarda los tokens.
4. Ejecutar las solicitudes de usuarios y roles en orden para que se guarden `usuarioId` y `rolId`.
5. La recuperacion de clave requiere un SMTP funcional; reemplazar `TOKEN_RECIBIDO_POR_CORREO` por el token recibido.

## Seguridad revisada

- Ningun secreto se versiona: `.env` permanece ignorado y `.env.example` contiene marcadores.
- El backend resuelve las autoridades desde base de datos en cada solicitud autenticada.
- Las cuentas inactivas no pueden usar access ni refresh tokens.
- Los cambios de clave, correo e inactivacion revocan refresh tokens correspondientes.
- El navegador solo se acepta desde los origenes de `CORS_ORIGENES`.
