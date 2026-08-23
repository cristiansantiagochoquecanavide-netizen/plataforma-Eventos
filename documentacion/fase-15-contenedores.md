# Fase 15 - Contenedores

## Servicios

| Servicio | Contenedor | Puerto local | Healthcheck |
|---|---|---:|---|
| PostgreSQL | `base-datos` | 5432 | `pg_isready` |
| Spring Boot | `backend` | 8080 | `/actuator/health` |
| Angular + Nginx | `frontend-web` | 4200 | `/` |
| FastAPI | `servicio-ia` | 8000 | `/api/salud` |

`docker-compose.yml` usa un volumen administrado llamado `datos_postgres` para los datos de PostgreSQL. Flutter no se contiene: se ejecuta localmente sobre emulador o dispositivo.

## Inicio

1. Copiar `.env.example` como `.env`.
2. Reemplazar `POSTGRES_PASSWORD`, `JWT_SECRET` y `ADMIN_CLAVE` por valores seguros.
3. Ejecutar:

```powershell
docker compose up --build
```

Para ejecutarlo en segundo plano:

```powershell
docker compose up --build --detach
docker compose ps
```

## Verificacion

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health
Invoke-WebRequest http://localhost:8000/api/salud
Invoke-WebRequest http://localhost:4200/
```

## Detencion

`docker compose down` conserva el volumen de PostgreSQL. Usar `docker compose down --volumes` solo cuando se desee eliminar los datos locales.
