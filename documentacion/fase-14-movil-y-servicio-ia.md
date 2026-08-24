# Fase 14 - Movil y Servicio IA

## Aplicacion movil

`aplicacion-movil` usa Flutter y consume la API Spring Boot de Sprint 0. Los tokens se conservan con `flutter_secure_storage`; el access token se envia como Bearer y se renueva una vez al recibir `401`.

La aplicacion cubre el alcance movil de Sprint 0: registro de participante, inicio y renovacion de sesion, recuperacion de clave, consulta y actualizacion del perfil y cambio de clave. La recuperacion usa enlaces `eventos://recuperar-clave?token=...`, con token precargado, confirmacion de la nueva clave y entrada manual como respaldo. El backend limita las solicitudes a una por cuenta cada 60 segundos. En desarrollo, Mailpit recibe los correos en `http://localhost:8025`. La administracion de usuarios, roles y permisos se mantiene exclusivamente en Angular.

El esquema personalizado es exclusivo de desarrollo; produccion debe migrar a Android App Links HTTPS y un dominio verificado.

## Servicio IA

`servicio-ia` es un proyecto FastAPI aislado. Solo ofrece `GET /api/salud` en Sprint 0. No lee PostgreSQL ni implementa recomendaciones hasta Sprint 4.
