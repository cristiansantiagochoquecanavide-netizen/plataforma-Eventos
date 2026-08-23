# Frontend Web - Fase 13

## Alcance

La aplicacion Angular en `frontend-web` implementa el panel web de Sprint 0:

- Inicio y cierre de sesion.
- Almacenamiento local de access y refresh tokens.
- Interceptor Bearer con renovacion automatica ante `401`.
- Guard de autenticacion y restriccion de rutas administrativas para `ADMINISTRADOR`.
- Perfil propio y cambio de clave.
- Administracion de usuarios.
- Administracion de roles y asignacion de permisos.

La API sigue siendo la fuente de verdad: los guards mejoran la navegacion, mientras que Spring Security aplica los permisos en cada solicitud.

## Ejecucion local

1. Iniciar el backend en `http://localhost:8080` con PostgreSQL y las variables requeridas.
2. Desde `frontend-web`, ejecutar `npm install`.
3. Ejecutar `npm start`.
4. Abrir `http://localhost:4200`.

La URL de la API esta en `frontend-web/src/environments/environment.ts`. El origen por defecto coincide con `CORS_ORIGENES=http://localhost:4200` del backend.

## Verificacion

Ejecutar `npm run build` para generar el bundle de produccion en `frontend-web/dist/frontend-web`.
