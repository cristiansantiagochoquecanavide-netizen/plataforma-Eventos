# Despliegue en Render

El Blueprint de la raíz (`render.yaml`) crea estos recursos en Oregon:

- `plataforma-eventos-web`: frontend Angular servido por Nginx. Nginx reenvía `/api` y el WebSocket de aforo a la API por la red privada de Render.
- `plataforma-eventos-api`: API Spring Boot privada; toma PostgreSQL y la URL interna de IA desde referencias del Blueprint.
- `plataforma-eventos-ia`: servicio FastAPI privado.
- `eventos-db`: PostgreSQL administrado, accesible solo por la red privada.

## Publicar el Blueprint

1. Confirma que `render.yaml` y estos cambios estén en la rama `main` del repositorio `cristiansantiagochoquecanavide-netizen/plataforma-Eventos`.
2. Abre [crear Blueprint en Render](https://dashboard.render.com/blueprint/new?repo=https://github.com/cristiansantiagochoquecanavide-netizen/plataforma-Eventos) y conecta GitHub si Render lo solicita.
3. En la primera aplicación del Blueprint, introduce `ADMIN_CORREO`. `ADMIN_CLAVE` y `JWT_SECRET` se generan en Render; guarda la contraseña inicial del administrador desde el panel de variables del servicio API.
4. Revisa los planes antes de aplicar: la web usa el plan gratis; API e IA son servicios privados y usan el plan pago mínimo `0.5c-512mb`.
5. Espera a que termine el despliegue y abre la URL pública del servicio web.

## Datos y limitaciones

El Blueprint deja PostgreSQL en el plan gratis para pruebas. Render elimina las bases de datos gratis a los 30 días; para conservar los datos, cambia `plan: free` por un plan PostgreSQL pago antes de aplicar el Blueprint. Los servicios web gratis pueden entrar en reposo tras 15 minutos sin tráfico y tardar alrededor de un minuto en despertar. No uses esta configuración gratuita como producción.

Las migraciones Flyway se ejecutan al arrancar la API. Las claves `GEMINI_API_KEY`, `REPORTES_OPENAI_API_KEY`, `BREVO_API_KEY` y `BREVO_FROM_EMAIL` son opcionales para el arranque; añádelas en el servicio correspondiente en Render para habilitar esas integraciones. Stripe queda desactivado inicialmente.

Para desplegar una actualización, sincroniza la rama `main` del Blueprint desde Render o deja activo el despliegue automático de commits.
