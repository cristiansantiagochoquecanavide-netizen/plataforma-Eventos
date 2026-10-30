# Operacion de Produccion

## Precondiciones

- Usar una sesión AWS CLI/SSO con permisos para ECR, CloudFormation, ECS, EC2/VPC, RDS, IAM, Secrets Manager, KMS y CloudWatch.
- Configurar un nombre DuckDNS y dejar IPv6 vacío. La plantilla reserva una Elastic IP; después del despliegue, actualizar el registro A DuckDNS a esa IP. Caddy esperará esa propagación y obtendrá/renovará HTTPS con Let's Encrypt.
- Crear el secreto JSON de aplicación en Secrets Manager con las claves `jwt-secret`, `admin-password`, `smtp-user` y `smtp-password`; guardar su ARN para `ApplicationSecretsArn`. No poner valores secretos en parámetros, archivos de despliegue ni argumentos de comandos.
- La plantilla crea una contraseña maestra administrada por RDS en Secrets Manager y la inyecta al backend. No hace falta crear `POSTGRES_PASSWORD` manualmente.
- Crear el secreto de auditoría propiedad del desarrollador y su KMS key, y proporcionar sus ARN. El rol de ejecución ECS necesita permiso para leer esos secretos y descifrarlos.
- Usar valores distintos a los de desarrollo y rotarlos antes de una entrega productiva. Rotar también cualquier credencial que haya estado en archivos locales.
- El proxy Caddy expone HTTPS al público y se conecta por HTTP al ALB, cuyo grupo de seguridad acepta tráfico solo desde el proxy.
- Confirmar que `CORS_ORIGENES` y `URL_FRONTEND_WEB` usan el dominio publico real.
- Confirmar que SES tiene remitente verificado y permisos de envio.

## Migraciones Flyway

1. Hacer backup/snapshot de PostgreSQL.
2. Desplegar la imagen backend con todas las migraciones Flyway requeridas por la versión que se libera.
3. Esperar a que `/actuator/health` responda `200`.
4. Consultar `flyway_schema_history` y confirmar que se aplicaron todas las migraciones incluidas en la imagen.
5. Validar conteos de `organizacion`, `usuario_organizacion`, `categoria`, `postulacion_staff`, `asignacion_staff` y `bitacora`.
6. Ejecutar smoke tests de login, `GET /api/organizaciones/mias` y una consulta con `X-Organization-Id`.

Las migraciones son hacia adelante. No se debe editar una migracion ya aplicada ni ejecutar `clean` en produccion.

## Despliegue ECS

La preparación ejecutable está en `infra/aws/deploy.ps1`. El script crea los repositorios ECR que falten con escaneo al publicar y tags inmutables, construye/publica las tres imágenes, valida la plantilla y despliega el stack. No incluye valores de contraseñas.

```powershell
./infra/aws/deploy.ps1 `
  -Region us-east-1 `
  -PublicDomainName app.tu-dominio.com `
  -AdminEmail admin@tu-dominio.com `
  -SesFrom no-reply@tu-dominio.com `
  -ApplicationSecretsArn arn:aws:secretsmanager:us-east-1:123456789012:secret:plataforma/app-ABC `
  -ImageTag release-2026-10-06 `
  -AwsProfile plataforma-eventos-deploy
```

Reemplazar los valores de ejemplo. Elegir un `ImageTag` único por release y conservarlo para rollback. El script ejecuta `cloudformation deploy` después de validar la plantilla; ejecutarlo crea/actualiza recursos AWS y puede generar cargos. Revisar el template y los parámetros antes de invocarlo.

Después del despliegue:

1. Copiar `ProxyElasticIp` de los outputs y actualizar el registro A en DuckDNS; no ingresar una IPv6.
2. Esperar que Caddy obtenga el certificado y que ECS esté estable con targets saludables en el ALB.
3. Validar HTTPS, `/actuator/health`, `/api/salud`, autenticación, selección de organización, eventos, inscripciones, postulaciones y bitácora.
4. Confirmar migraciones, logs en CloudWatch y backups RDS; probar restauración antes de operar con datos importantes.

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
