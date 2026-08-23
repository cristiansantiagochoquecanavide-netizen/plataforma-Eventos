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

## Validacion de Sprint 0

La coleccion Postman y el entorno local estan en `documentacion/postman`. La matriz de cierre, evidencias y comandos de validacion estan en `documentacion/cierre-sprint-0.md`.
