# CU23 en Flutter y Angular

## Contrato

Los clientes registran asistencia con `POST /api/asistencias/check-in` y el cuerpo mínimo `{"idInscripcion": 123}`. El backend valida permiso, inscripción, estado, evento, asignación del staff, duplicidad y fecha oficial. La fecha presentada es `fechaHora` de la respuesta.

## Punto de integración pendiente con CU22

La rama actual no contiene una pantalla ni un servicio CU22 de validación QR en Flutter o Angular. CU23 no simula el escaneo ni interpreta códigos QR. En Flutter, `AttendancePage` recibe por ahora el ID mediante el campo `ID de inscripción`; en Angular, `AsistenciasComponent` ofrece las inscripciones devueltas por `GET /api/inscripciones/participantes`. Cuando CU22 esté listo, el punto exacto de acoplamiento es reemplazar esa selección/entrada por el callback o resultado validado de CU22 que entregue `idInscripcion`, y llamar el método CU23 existente (`SessionController.registrarAsistencia(id)` / `AsistenciasService.registrar(id)`). No se debe enviar el QR al endpoint de CU23.

La consulta Angular reutiliza el endpoint de participantes existente, protegido por `PARTICIPANTE_VER`; CU23 exige además `ASISTENCIA_REGISTRAR`. Si falta el permiso de consulta, la pantalla informa HTTP 403, y el backend sigue siendo la autoridad para ambos permisos.
