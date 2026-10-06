# Dataset local de empresas con seis meses de operación

## Activación

El dataset se carga únicamente con `DEMO_SEED_ENABLED=true`. En Compose, usar el override local:

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml up --build
```

El override habilita el seeder solo para el backend local. No ejecutar con `DEMO_SEED_ENABLED=true` contra una base productiva.

## Organizaciones incluidas

Se agregan cinco organizaciones con datos de muestra plausibles:

| Organización | Slug | Cuenta organizadora |
|---|---|---|
| Fundación Suma Qamaña | `fundacion-suma-qamana` | `lucia.arce@sumaqamana.local` |
| Nexo Andino Tecnología | `nexo-andino-tecnologia` | `matias.cespedes@nexoandino.local` |
| Casa Cultural La Estación | `casa-cultural-la-estacion` | `mariela.vargas@laestacion.local` |
| Cámara Creativa del Valle | `camara-creativa-del-valle` | `sofia.rocha@camaravalle.local` |
| Club Deportivo Illimani | `club-deportivo-illimani` | `javier.cardenas@clubillimani.local` |

Las cuentas nuevas de este dataset usan la contraseña local compartida `Demo123!`. Es solo para desarrollo/demo; el seeder está deshabilitado por defecto y conserva la contraseña de las cuentas que ya existían. También se conservan los dos tenants históricos `demo-universidad-norte` y `demo-centro-cultural-sur` para no borrar ni romper datos existentes. Por tanto, una base nueva tendrá siete organizaciones de muestra: las cinco de este dataset y las dos fixtures antiguas.

El roster consultado desde la base Docker está exportado en `documentacion/dataset-empresas-historicas-usuarios.csv`: incluye nombre, correo y roles de las 145 membresías de estas cinco organizaciones. No contiene hashes ni valores de contraseña.

## Volumen y realismo

- 5 organizaciones principales, cada una con 2 categorías, 2 sedes, 2 organizadores, 3 integrantes de staff y 24 participantes.
- 14 eventos por organización: dos actividades por mes, desde el primer día del mes de hace seis meses hasta el mes actual. Los nombres incluyen serie, mes y año; los estados reflejan eventos pasados, próximos y cancelados.
- 15 participantes inscritos por evento: 1.050 inscripciones para las 70 actividades, con estados confirmados, en espera y cancelados, y fechas de inscripción anteriores al evento.
- 35 convocatorias de staff/voluntariado, postulaciones con estados variados y asignaciones relacionadas.
- Las organizaciones, usuarios, eventos, inscripciones y demás entidades usan IDs autogenerados por PostgreSQL. Los códigos QR son UUID determinísticos por evento y asistente, no secuencias que expongan el volumen.

Las fechas se calculan respecto a la fecha de ejecución y usan el huso de Bolivia. El seeder es idempotente: usa slugs, nombres de actividad, correos y relaciones tenant-aware para reutilizar registros existentes; no elimina datos.

## Verificación

`DatasetEmpresasHistoricasTest` usa PostgreSQL-compatible H2 para validar cinco organizaciones, 70 eventos, categorías/sedes, más de 800 inscripciones, historia que alcanza el mes de hace seis meses e idempotencia tras ejecutar el seeder dos veces. La contraseña seed se hashea una vez por inicialización.
