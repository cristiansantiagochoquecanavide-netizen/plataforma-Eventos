# Plataforma de Gestion de Eventos

Proyecto semestral de Sistemas de Informacion II.

## Servicios

- `backend`: API Spring Boot y PostgreSQL.
- `frontend-web`: panel administrativo Angular.
- `aplicacion-movil`: cliente Flutter para participante y staff.
- `servicio-ia`: servicio FastAPI reservado para Sprint futuros.

## Ejecucion con Docker

1. Crear `.env` a partir de `.env.example` y reemplazar todas las claves de ejemplo.
2. Ejecutar `docker compose up --build`.
3. Abrir el panel en `http://localhost:4200`.

Servicios expuestos: PostgreSQL `5432`, backend `8080`, FastAPI `8000` y Angular `4200`.

El cliente Flutter se ejecuta localmente; consultar `aplicacion-movil/README.md`.

## Demo local Sprint 2 con Docker

El archivo base `docker-compose.yml` mantiene `DEMO_SEED_ENABLED=false` por defecto. Para levantar la demo multi-tenant local, usar el override `docker-compose.local.yml`, que habilita el seeder solo en el backend.

Iniciar o reconstruir el stack local:

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml up --build
```

Iniciar en segundo plano:

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml up --build -d
```

Inspeccionar estado:

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml ps
```

Ver logs de todos los servicios:

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml logs -f
```

Ver logs del backend:

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml logs -f backend
```

Detener el stack sin borrar datos:

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml down
```

Reset destructivo de la base demo local:

> Destructivo: este comando borra el volumen `datos_postgres` y elimina todos los datos locales de PostgreSQL. Usarlo solo para reiniciar la demo.

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml down -v
```

URLs locales:

- Angular: `http://localhost:4200`
- API Spring Boot: `http://localhost:8080/api/`
- Mailpit: `http://localhost:8025`
- FastAPI: `http://localhost:8000`

Las credenciales demo creadas por el seeder son solo para uso local. No reutilizarlas fuera del entorno de desarrollo.

## Correo en produccion

En local, CU4 puede usar Mailpit. En AWS debe usarse un SMTP transaccional real, por ejemplo Amazon SES, configurando `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_FROM`, `SMTP_AUTH`, `SMTP_STARTTLS_ENABLE`, `SMTP_STARTTLS_REQUIRED` y `URL_FRONTEND_WEB`.

Ejemplo de variables en `.env.production.example` y guia en `documentacion/despliegue-aws-correo.md`.

## Validacion de Sprint 0

La coleccion Postman y el entorno local estan en `documentacion/postman`. La matriz de cierre, evidencias y comandos de validacion estan en `documentacion/cierre-sprint-0.md`.
