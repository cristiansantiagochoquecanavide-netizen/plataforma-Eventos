# Matriz actual de funcionalidades Web / Móvil

Alcance comprobado contra las rutas de `frontend-web/src/app/app.routes.ts`, los destinos de `HomeShell` en Flutter (`aplicacion-movil/lib/main.dart`) y sus pantallas actuales. “Web” indica disponibilidad en el cliente web; “Móvil” indica disponibilidad en la app Flutter. La matriz no afirma paridad de operaciones cuando solo una plataforma tiene el caso de uso.

| Caso de uso / capacidad | Web | Móvil | División recomendada |
|---|:---:|:---:|---|
| Iniciar sesión | ✓ | ✓ | Ambas: acceso inicial. |
| Recuperar/restablecer clave | ✓ | ✓* | Ambas; móvil consume enlace profundo de recuperación. |
| Consultar eventos | ✓ | ✓ | Ambas: búsqueda y consulta de campo. |
| Inscribirse a un evento | — | ✓ | Móvil: inscripción del asistente. |
| Consultar/cancelar inscripciones propias | — | ✓ | Móvil: autoservicio del asistente. |
| Postularse a convocatorias de staff | ✓ | ✓ | Ambas: web administra; móvil facilita operación de campo. |
| Consultar asignaciones de staff | ✓ | ✓ | Ambas, con web para coordinación y móvil para consulta operativa. |
| Crear/editar eventos y categorías | ✓ | — | Web: administración. |
| Administrar espacios, recursos y programación | ✓ | — | Web: operaciones administrativas. |
| Administrar usuarios, roles, miembros y grupos | ✓ | — | Web: administración de la organización/plataforma. |
| Configurar privilegios por componente UI | ✓ | — | Web: pantalla administrativa; la app móvil aplica la política recibida. |
| Gestionar participantes e historial | ✓ | — | Web: operación de padrón y administración. |
| CU27 Consultar historial de participación y eventos asistidos | ✓ | ✓ | Participante: consulta sus inscripciones y asistencias en ambas plataformas. |
| Construir/previsualizar/exportar/enviar reportes de participantes | ✓ | — | Web: reportes administrativos con filtros tenant-scoped. |
| Consultar bitácora | ✓ | — | Web: operación autorizada de auditoría. |
| Ver ayuda contextual | ✓ | ✓ | Ambas: orientación de uso. |

El flujo móvil de restablecimiento se inicia mediante enlace de app; no se encontró un flujo de inscripción Web navegable en las rutas/pantallas actuales.

## Pruebas de paridad requeridas

- Una misma cuenta y organización activa ve los mismos eventos permitidos en Web y Móvil.
- Cambiar de organización refresca datos y privilegios; no queda información del tenant anterior en memoria o pantalla.
- Acciones disponibles en Móvil tienen autorización equivalente en el backend, aunque el panel administrativo solo exista en Web.
- Comprobar inscripción, postulación y asignación con el mismo estado de negocio en ambos clientes.
