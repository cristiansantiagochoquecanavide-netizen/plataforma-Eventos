# Agents Registry

Available custom agents under `.agents/agents/`:

- `orchestrator.md` — Enrutador central y coordinador multi-agente.
- `backend.md` — Java 21, Spring Boot 3, Spring Data JPA, PostgreSQL, Spring Security (JWT), Maven.
- `frontend.md` — Angular 20, TypeScript, Standalone Components, Signals/RxJS, Reactive Forms.
- `mobile.md` — Flutter, Dart, BLoC/Provider, Android/iOS.
- `architecture.md` — Diseño modular, límites de dominio y decisiones arquitectónicas.
- `architect-planner.md` — Planificación de arquitectura y estrategia de ejecución por fases.
- `code-review.md` — Puertas de calidad, estándares y análisis de riesgos.
- `qa-testing.md` — Planificación y ejecución de pruebas unitarias/integración (JUnit 5, Mockito, Jasmine/Karma).
- `infra.md` — Docker, Docker Compose, variables de entorno y despliegue.

## Flujo de Trabajo
1. `orchestrator` clasifica el requerimiento.
2. `orchestrator` delega al especialista correspondiente (`backend`, `frontend`, `mobile`, etc.).
3. El especialista ejecuta respetando los estándares de seguridad y modularidad.
4. Se validan las pruebas y se consolida la entrega.
