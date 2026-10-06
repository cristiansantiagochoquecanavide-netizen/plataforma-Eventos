# Alta SaaS y suscripciones con Stripe

## Alcance implementado

- Catálogo público mensual en BOB: Básico (gratis), Profesional (Bs 399) y Empresarial (Bs 1.299); las cuotas y los formatos de reporte proceden del backend.
- Formulario público de alta empresarial con verificación de correo antes de crear Checkout o el tenant.
- Checkout alojado por Stripe para los dos planes de pago. El navegador no activa la cuenta ni confirma el pago; solo el webhook firmado puede provisionar el tenant tras validar moneda e importe.
- El propietario nuevo define su clave mediante enlace de activación. Por seguridad, una dirección que ya pertenece a una cuenta (incluso si se creó después de empezar el checkout) no se añade como propietaria automáticamente; esa ruta requiere completar un flujo autorizado de asociación.
- Los eventos de webhook se guardan con ID único para deduplicación. Pagos fallidos suspenden la suscripción; cancelación definitiva la vence. Se guarda el tiempo del último evento Stripe aplicado para ignorar eventos de estado antiguos.
- Si el Checkout expira o se cancela, el enlace de verificación se puede reutilizar durante siete días para intentar de nuevo. El pago completado no queda condicionado a la página de éxito del navegador.

## Rutas

Frontend público:

- `GET /planes` — catálogo y solicitud de alta.
- `GET /verificar-alta-saas?token=...` — verificación; redirige a Checkout cuando corresponde.
- `GET /activar-cuenta-saas?token=...` — definir clave y activar al propietario.
- `GET /pago/completado` — confirmación informativa; el estado final depende del webhook.

API pública:

- `GET /api/saas/planes`
- `POST /api/saas/altas`
- `POST /api/saas/altas/verificar-email`
- `POST /api/saas/altas/activar`
- `POST /api/saas/stripe/webhook` (requiere `Stripe-Signature` válida)

## Configuración local

La aplicación se mantiene en modo de cobro deshabilitado por defecto. Para pruebas se configura el backend únicamente con variables de entorno:

```text
STRIPE_ENABLED=true
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
URL_FRONTEND_WEB=http://localhost:4200
```

El correo debe estar conectado a Mailpit o a otro servidor SMTP de prueba para recibir verificación y activación. `STRIPE_SUCCESS_URL` y `STRIPE_CANCEL_URL` son opcionales; sus valores predeterminados apuntan a las rutas del frontend local.

`docker-compose.yml` reenvía estas variables al backend. Desde la raíz de esta worktree, el plan Básico puede probarse sin claves Stripe:

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml up --build
```

Abre `http://localhost:4200/planes`, solicita el plan Básico y consulta el correo de verificación en `http://localhost:8025`. La primera subida del backend aplica V31 por Flyway. Completa ambos enlaces para activar la cuenta y luego entra por `http://localhost:4200/iniciar-sesion`.

Para Checkout de Profesional/Empresarial, primero deja ejecutándose `stripe listen --forward-to localhost:8080/api/saas/stripe/webhook` y toma el secreto `whsec_...` que Stripe CLI muestra en esa terminal. En otra PowerShell, desde la raíz del proyecto, configura claves de prueba y levanta/reinicia Compose:

```powershell
$env:STRIPE_ENABLED = 'true'
$env:STRIPE_SECRET_KEY = 'sk_test_...'
$env:STRIPE_WEBHOOK_SECRET = 'whsec_...'
docker compose -f docker-compose.yml -f docker-compose.local.yml up --build
```

Usa solo una tarjeta de prueba de Stripe en Checkout. El navegador de éxito es informativo; confirma la provisión en Mailpit/webhook. La aceptación de BOB aún depende de la configuración de la cuenta Stripe de prueba.

No almacenar claves Stripe en el repositorio, frontend, capturas o mensajes. Para probar webhooks, usar el CLI de Stripe y su secreto temporal de firma.

## Verificación observada

- Backend: `cd backend; .\mvnw.cmd test` — 164 pruebas aprobadas, sin fallos ni errores.
- Frontend: `cd frontend-web; npm run build` — build de producción aprobado.
- Frontend: `cd frontend-web; npx ng test --watch=false --browsers=ChromeHeadless --progress=false` — 49 pruebas aprobadas.
- La prueba del servicio webhook valida rechazo de firma incorrecta y deduplicación de evento; una prueba de dominio cubre el reintento de Checkout tras cancelación.

## Pendientes antes de cobrar en producción

- Confirmar elegibilidad comercial de Stripe para una entidad en Bolivia y compatibilidad de BOB tanto para el Checkout como para liquidación. No se ha probado cuenta/moneda con credenciales reales.
- Ejecutar migración V31 y flujo end-to-end en PostgreSQL de prueba con SMTP/Mailpit, Stripe CLI y claves test.
- Acordar el tratamiento fiscal y legal del cobro en Bolivia antes de publicar la oferta comercial.
- Configurar secretos en runtime (por ejemplo Secrets Manager) y webhook HTTPS para el entorno autorizado. No se ha desplegado en AWS.
