# CU22: escanear y validar código QR de acreditación

**Actor:** staff autorizado  
**Plataforma:** aplicación móvil Flutter

## Flujo principal

1. El staff abre **Control de acceso**. La opción aparece a cuentas con el permiso `ASISTENCIA_REGISTRAR`.
2. La app abre la cámara y lee el código QR de la acreditación.
3. La app envía el valor leído a `POST /api/asistencias/validar-qr` con la sesión y la organización activa.
4. El backend busca el código dentro de esa organización y comprueba la asignación de staff al evento, la inscripción confirmada, el estado publicado o en curso del evento y que aún no exista una asistencia.
5. La app muestra participante y evento para que el staff revise a la persona frente a él.
6. El staff confirma el check-in. CU23 recibe `idInscripcion` y registra la asistencia con `POST /api/asistencias/check-in`.

Solicitud:

```json
{ "codigoQr": "INS-9-21-AB12CD34" }
```

La validación responde `200 OK` sin registrar asistencia:

```json
{
  "idInscripcion": 123,
  "participante": "Nombre Apellido",
  "idEvento": 9,
  "nombreEvento": "Evento"
}
```

CU22 no registra ni marca la entrada. Al confirmar, CU23 recibe únicamente `idInscripcion` y vuelve a cargar y validar los datos persistidos antes de guardar la asistencia. Esto permite que CU23 rechace un QR revocado, una asignación retirada o un check-in concurrente.

## Respuestas de error

- `400`: código ausente o inscripción/evento en un estado que no permite el acceso.
- `401`: sesión ausente o vencida.
- `403`: falta el permiso de asistencia o el staff no está asignado al evento.
- `404`: código QR no encontrado en la organización activa.
- `409`: asistencia ya registrada; el staff debe consultar el registro existente en vez de repetir el check-in.

La cámara solicita permiso del sistema operativo. Android declara `CAMERA`; iOS explica que el permiso se usa para escanear acreditaciones. Tras un error, staff puede reintentar sin salir de la pantalla.
