# Datos demo para probar CU23

El seeder local crea tres inscripciones confirmadas y pendientes de asistencia en la organización `principal`:

- `CU23 Demo - Evento 1`
- `CU23 Demo - Evento 2`
- `CU23 Demo - Evento 3`

Cada evento tiene una asignación activa para `StaffEventos@gmail.com` y una inscripción confirmada de `ParticipanteEvento@gmail.com`. El seeder es idempotente y no crea duplicados al reiniciar.

## Activar con Docker

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build
```

Después, iniciar sesión en `http://localhost:4200` con:

```text
Usuario: StaffEventos@gmail.com
Clave: Staff123
```

Abrir **Registrar asistencia**, seleccionar una de las tres inscripciones y pulsar **Registrar asistencia**.
