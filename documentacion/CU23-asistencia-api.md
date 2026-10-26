# CU23: registro de asistencia

## Check-in

`POST /api/asistencias/check-in`

Requiere autenticación, membresía activa en la organización seleccionada mediante `X-Organization-Id` (o la organización principal) y el permiso `ASISTENCIA_REGISTRAR`. El usuario autenticado también debe tener una asignación de staff activa para el evento de la inscripción.

```json
{
  "idInscripcion": 123
}
```

El servidor obtiene participante, evento, organización, usuario que registra y fecha/hora. El cliente no puede enviar ni reemplazar esos datos. La inscripción debe estar confirmada y el evento publicado o en curso.

Respuesta `201 Created`:

```json
{
  "idAsistencia": 45,
  "idInscripcion": 123,
  "participante": "Nombre Apellido",
  "correoParticipante": "persona@ejemplo.test",
  "idEvento": 9,
  "nombreEvento": "Evento",
  "fechaHora": "2026-10-25T18:30:00Z",
  "idRegistradoPor": 7,
  "nombreRegistradoPor": "Nombre Staff"
}
```

Errores: `400` para solicitud, estado de inscripción o estado de evento no válidos; `401` para sesión ausente; `403` si falta el permiso o el usuario no está asignado al evento; `404` si no existe la inscripción dentro de la organización; `409` si ya hay asistencia.

La tabla guarda una única asistencia por inscripción con restricción `UNIQUE`; las solicitudes concurrentes quedan serializadas bloqueando la inscripción en la transacción. La fecha se genera en PostgreSQL.

## Integración con CU22

CU22 valida el QR con `POST /api/asistencias/validar-qr`, obtiene el `idInscripcion` asociado y muestra participante/evento al staff. CU22 no registra la entrada. Después de la confirmación del staff, el cliente llama a este endpoint enviando únicamente ese identificador. CU23 no recibe ni interpreta códigos QR ni datos de participante desde el cliente; vuelve a cargar y validar la inscripción persistida.
