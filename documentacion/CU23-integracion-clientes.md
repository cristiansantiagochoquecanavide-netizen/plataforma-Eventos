# CU23 en Flutter y Angular

## Contrato

Los clientes registran asistencia con `POST /api/asistencias/check-in` y el cuerpo mínimo `{"idInscripcion": 123}`. El backend valida permiso, inscripción, estado, evento, asignación del staff, duplicidad y fecha oficial. La fecha presentada es `fechaHora` de la respuesta.

## Flujo de CU22 a CU23

En Flutter, CU22 lee el QR y llama `POST /api/asistencias/validar-qr`. El backend busca la inscripción en la organización activa y valida elegibilidad para el staff autenticado. La pantalla muestra participante y evento; el staff compara esos datos con la acreditación y confirma. Solo entonces Flutter llama `SessionController.registrarAsistencia(idInscripcion)`, que consume `POST /api/asistencias/check-in`.

En Web, CU23 mantiene el control operativo con su listado de inscripciones elegibles. El operador selecciona una inscripción, revisa participante y evento, y confirma el registro. Web llama `AsistenciasService.registrar(idInscripcion)`; no lee ni envía códigos QR.

Los dos clientes terminan en el mismo check-in de CU23. El backend vuelve a cargar la inscripción y comprueba permiso, asignación activa, estado confirmado, evento publicado/en curso y ausencia de asistencia previa. La respuesta incluye la hora oficial del servidor.
