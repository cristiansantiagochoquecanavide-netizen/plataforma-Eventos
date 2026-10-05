# Runbook local de backup y restore

## Alcance y objetivos provisionales

- Ambiente local: backup PostgreSQL bajo demanda, custom-format, checksum SHA-256 y restore a una base nueva e aislada.
- Objetivos acordados para AWS/RDS: retención de backups automáticos de 7 días, ventana UTC `03:00-04:00`, RPO objetivo ≤ 5 minutos y RTO objetivo ≤ 4 horas. Son objetivos a medir en un ensayo, no garantías de AWS ni niveles de servicio ya demostrados.
- CloudFormation queda preparado; estos cambios no despliegan ni modifican AWS.
- Los archivos de backup pueden contener datos personales o payload de bitácora. La carpeta local `backups/` está excluida de Git; mantenerla en almacenamiento protegido y eliminar copias según la política aprobada.

## Crear backup local

Requisito: servicios Compose activos y `base-datos` saludable.

```powershell
.\infra\backup\backup-postgres.ps1
```

El comando crea `backups/eventos-<UTC>.dump` y su `.sha256`; verifica que el archivo sea legible por `pg_restore`. No vuelca el contenido del backup a la consola.

## Probar restore sin sobrescribir la aplicación

```powershell
.\infra\backup\restore-postgres-validation.ps1 .\backups\eventos-<UTC>.dump
```

La herramienta valida el checksum, crea una base con prefijo `restore_validation_`, restaura allí y confirma que haya tablas. Nunca borra una base existente ni apunta a la base configurada de la aplicación. La base de ensayo se conserva para inspección.

Después de verificar los datos, eliminar manualmente solo la base de validación creada, sustituyendo el nombre real:

```powershell
docker compose exec base-datos dropdb --if-exists -U $env:POSTGRES_USER restore_validation_<identificador>
```

## Validación posterior al restore

1. Confirmar tablas y `flyway_schema_history` con migraciones exitosas.
2. Confirmar existencia de tenants, usuarios y registros esperados, sin imprimir ni adjuntar contenido personal.
3. Consultar el health check solo después de iniciar una instancia apuntando explícitamente a la base restaurada.
4. Si se restaura bitácora cifrada, comprobar que el entorno tenga acceso a la llave correcta; sin ella, la aplicación debe fallar de forma cerrada al descifrar.

## AWS/RDS

- RDS configura backups automáticos con retención de 7 días, almacenamiento cifrado y una ventana UTC definida en `infra/aws/cloudformation.yml`.
- Para una prueba futura, restaurar snapshot/PITR a una instancia temporal nueva, verificar health y aislamiento, medir RPO/RTO y eliminar el recurso temporal solo después de validar su identificador.
- No ejecutar restauración, despliegue ni cambios en AWS productivo sin autorización específica.

## Llave de bitácora controlada por el desarrollador

- Las nuevas escrituras usan AES-256-GCM con `AUDIT_DEVELOPER_KEY_BASE64`, una llave Base64 de exactamente 32 bytes suministrada como secreto de runtime. La llave no se guarda en el repositorio, en PostgreSQL ni en logs.
- Los índices exactos continúan usando una llave KMS HMAC separada. Sus tokens no permiten descifrar payloads, aunque pueden revelar coincidencias/patrones de búsqueda. No confundir la llave HMAC con la llave desarrollador del contenido.
- Compose no suministra una llave por defecto: sin ella, nuevas escrituras y lecturas v2 fallan cerradas. Para desarrollo local, genera una llave aleatoria de 32 bytes en PowerShell y mantenla en el entorno actual o en `.env` (ignorado por Git); el ejemplo no la imprime:

```powershell
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 32
$rng.GetBytes($bytes)
$env:AUDIT_DEVELOPER_KEY_BASE64 = [Convert]::ToBase64String($bytes)
$rng.Dispose()
[Array]::Clear($bytes, 0, $bytes.Length)
```

- No reutilices una llave conocida/demo. Para despliegue, la llave persiste en el secreto externo y el stack solo recibe su ARN.
- Para AWS, crear el secreto fuera del stack de la plataforma bajo control del desarrollador y pasar su ARN como `DeveloperAuditKeySecretArn`; pasar también la ARN de su CMK mediante `DeveloperAuditSecretKmsKeyArn`. El secreto contiene solo el valor Base64. La política de recurso de Secrets Manager/CMK debe conceder lectura y descifrado del secreto al execution role de la tarea backend.
- Envelopes históricos `crypto_version=1` KMS siguen siendo legibles durante la transición. El backfill opt-in convierte texto plano y envelopes KMS heredados a `crypto_version=2` con la llave del desarrollador; conserva los índices HMAC existentes. Debe ensayarse solo sobre la base restaurada antes de planificar producción.
- CloudFormation deja deshabilitado por defecto el permiso del backend para descifrar payloads KMS v1. Activa `EnableLegacyAuditPayloadDecrypt=true` solo durante la migración controlada; tras verificar que no quedan filas v1, vuelve a `false` para retirar el permiso. La llave HMAC separada permanece activa para los índices actuales.
- La rotación de la llave HMAC no es transparente: los tokens existentes no se pueden buscar con una llave nueva. Se agregó un modo explícito para recalcular los índices de todas las filas v2: `--audit.cifrado.backfill.enabled=true --audit.cifrado.backfill.reindex-indexes=true`. Debe apuntar a la llave nueva, ejecutarse como proceso no-web con el backend detenido (ventana de mantenimiento) y validarse con búsquedas exactas representativas antes de reabrir tráfico. El modo normal sigue omitiendo filas v2. Conserva la llave anterior y la capacidad IAM de restaurarla mientras existan snapshots/backups con índices antiguos o se necesite rollback; solo entonces evalúa retirarla.
- La llave actual no admite rotación en caliente: antes de sustituirla deben re-cifrarse todos los payloads v2 en una operación de rotación versionada y verificable. Sin la llave, el sistema falla cerrado al descifrar; conserva copias de recuperación bajo custodia autorizada.
