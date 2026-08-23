# Fase 14 - Movil y Servicio IA

## Aplicacion movil

`aplicacion-movil` usa Flutter y consume la API Spring Boot de Sprint 0. Los tokens se conservan con `flutter_secure_storage`; el access token se envia como Bearer y se renueva una vez al recibir `401`.

La aplicacion cubre el alcance movil actual: inicio de sesion, consulta y actualizacion del perfil y cambio de clave. La administracion de usuarios, roles y permisos se mantiene exclusivamente en Angular.

## Servicio IA

`servicio-ia` es un proyecto FastAPI aislado. Solo ofrece `GET /api/salud` en Sprint 0. No lee PostgreSQL ni implementa recomendaciones hasta Sprint 4.
