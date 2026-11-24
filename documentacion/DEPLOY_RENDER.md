# Despliegue gratuito en Render

El Blueprint de la raíz (`render.yaml`) usa solo servicios gratuitos de Render:

- `plataforma-eventos-web`: frontend Angular como sitio estático.
- `plataforma-eventos-api`: API Spring Boot en un servicio web gratis.
- `plataforma-eventos-ia`: servicio FastAPI en un servicio web gratis.
- `eventos-db`: PostgreSQL gratis, con acceso limitado a la red privada.

La web estática compila la URL pública de la API. El navegador llama directamente a la API y su CORS acepta el origen del frontend. Como Render no permite que un servicio web gratis reciba llamadas por la red privada, la API llama a la URL pública del servicio de IA. El servicio de IA exige la clave compartida generada por Render; el endpoint de salud queda disponible para el health check.

## Publicar y desplegar

1. Confirma que `render.yaml` y los cambios de este despliegue estén en la rama `main` del repositorio `cristiansantiagochoquecanavide-netizen/plataforma-Eventos`.
2. Abre [crear Blueprint en Render](https://dashboard.render.com/blueprint/new?repo=https://github.com/cristiansantiagochoquecanavide-netizen/plataforma-Eventos) y conecta GitHub si Render lo solicita.
3. En la primera aplicación del Blueprint, introduce `ADMIN_CORREO`. Render generará `JWT_SECRET`, `ADMIN_CLAVE`, la clave de cifrado y la clave de acceso interna de IA. Guarda la contraseña inicial de administrador desde las variables de entorno de la API.
4. Aplica el Blueprint y espera a que terminen los tres servicios y la base de datos.
5. Abre la URL pública de `plataforma-eventos-web`.

Las claves `GEMINI_API_KEY`, `REPORTES_OPENAI_API_KEY`, `BREVO_API_KEY` y `BREVO_FROM_EMAIL` son opcionales; añádelas al servicio que corresponda en Render para habilitar esas integraciones. Stripe queda desactivado inicialmente.

## Límites del nivel gratuito

No hay planes pagos en este Blueprint. Los servicios web gratis pueden entrar en reposo tras 15 minutos sin tráfico y tardar cerca de un minuto en despertar; al estar la API y la IA en servicios separados, el primer acceso puede tardar más. Render limita las horas gratuitas compartidas por workspace y puede suspender servicios al agotarlas.

PostgreSQL gratis en Render se elimina a los 30 días. Por eso, esta configuración sirve para pruebas o demostraciones temporales, pero no mantiene los datos de forma indefinida. Exporta la base antes de su vencimiento si necesitas conservar la información. El almacenamiento local de los servicios web gratis también es efímero.

La API y la IA tienen URL públicas porque Render no permite que servicios web gratis reciban tráfico privado. Las rutas de la API mantienen la autenticación de la aplicación; las rutas de IA requieren una clave interna aleatoria que solo comparten API y servicio de IA. No compartas esa clave.
