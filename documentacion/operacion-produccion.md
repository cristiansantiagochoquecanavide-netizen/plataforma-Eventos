# Operacion de Produccion

## Precondiciones

- Crear secretos fuera del repositorio para `POSTGRES_PASSWORD`, `JWT_SECRET`, `ADMIN_CLAVE`, `SMTP_USER` y `SMTP_PASSWORD`.
- Usar valores distintos a los de desarrollo y rotarlos antes de una entrega productiva.
- Configurar un certificado ACM y un dominio HTTPS delante del Application Load Balancer. La plantilla base conserva HTTP como transporte inicial y no debe exponerse sin TLS en produccion.
- Confirmar que `CORS_ORIGENES` y `URL_FRONTEND_WEB` usan el dominio publico real.
- Confirmar que SES tiene remitente verificado y permisos de envio.

## Migraciones Flyway

1. Hacer backup/snapshot de PostgreSQL.
2. Desplegar la imagen backend con las migraciones V14 a V21 incluidas.
3. Esperar a que `/actuator/health` responda `200`.
4. Consultar `flyway_schema_history` y confirmar que la ultima version aplicada sea `21`.
5. Validar conteos de `organizacion`, `usuario_organizacion`, `categoria`, `postulacion_staff`, `asignacion_staff` y `bitacora`.
6. Ejecutar smoke tests de login, `GET /api/organizaciones/mias` y una consulta con `X-Organization-Id`.

Las migraciones son hacia adelante. No se debe editar una migracion ya aplicada ni ejecutar `clean` en produccion.

## Despliegue ECS

1. Construir y publicar las imagenes backend, frontend e IA con tags inmutables.
2. Validar el template CloudFormation antes de actualizar el stack.
3. Actualizar la task definition con los tags nuevos y secretos del gestor elegido.
4. Esperar estado estable del servicio ECS y targets saludables en el ALB.
5. Validar salud, autenticacion, selección de organización, eventos, inscripciones, postulaciones y bitácora.

## Rollback

- Si falla la imagen: volver a la task definition anterior y esperar targets saludables.
- Si falla una migración: detener el despliegue, conservar el snapshot y revisar el error; no borrar `flyway_schema_history`.
- Si el cambio de esquema ya fue aplicado, corregir con una nueva migración compatible antes de volver a desplegar.
- Si el problema es de datos, restaurar el snapshot en una instancia separada y comparar antes de reemplazar producción.

## Validacion automatizada

```text
backend: .\\mvnw.cmd test
frontend-web: npm run build
aplicacion-movil: flutter test
aplicacion-movil: flutter analyze
```

La colección Postman de Sprint 2 está en `documentacion/postman` y la referencia de endpoints en `documentacion/api-sprint-2.md`.
