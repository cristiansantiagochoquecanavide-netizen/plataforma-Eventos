# Plataforma de Gestión de Eventos

Sistema integral para la planificación, administración, control de acceso y asignación de recursos para eventos corporativos e institucionales. Construido con una arquitectura modular desacoplada por dominios de negocio (**Package-by-Feature**) y un patrón **MVC multicapa estricto** tanto en Backend como en Frontend.

---

## Stack Tecnológico

| Capa / Servicio | Tecnología | Puerto Dev | Descripción |
| :--- | :--- | :--- | :--- |
| **Backend API** | Java 21 + Spring Boot 3.5 (Maven) | `:8080` | API REST con Spring Security (JWT), JPA/Hibernate, arquitectura modular de 5 capas por dominio y OpenAPI 3 |
| **Frontend Web** | Angular 20 (TypeScript) | `:4200` | Panel administrativo con componentes Standalone, Signals y servicios/modelos fragmentados por feature |
| **App Móvil** | Flutter / Dart | Emulador / Móvil | Cliente móvil para asistentes y staff |
| **Servicio IA** | Python / FastAPI | `:8000` | Microservicio satélite de asistencia inteligente |
| **Base de Datos** | PostgreSQL 17 (Alpine) | `:5432` | Persistencia relacional principal |
| **Email Transaccional** | Brevo API / SMTP | Externa | Servicio de envío de correos transaccionales (activación SaaS, recuperación y reportes) |
| **Contenedores** | Docker + Docker Compose | — | Orquestación completa de servicios en entornos de desarrollo y producción |

---

## Enlaces de Acceso Rápido y Credenciales

Al iniciar el sistema por primera vez, el componente `AdministradorInicializador` siembra automáticamente los roles del sistema y usuarios de prueba listos para iniciar sesión:

| Servicio | URL / Acceso | Credenciales Sembradas (Seed) |
| :--- | :--- | :--- |
| **Frontend Web** | [http://localhost:4200](http://localhost:4200) | **Administrador:** `AdminEventos@gmail.com` / `Admin123`<br>**Organizador:** `OrganizadorEventos@gmail.com` / `Organizador123`<br>**Staff:** `StaffEventos@gmail.com` / `Staff123`<br>**Participante:** `ParticipanteEvento@gmail.com` / `Participante123` |
| **App Móvil (Flutter)** | Emulador Android (`10.0.2.2:8080/api`)<br>Dispositivo Físico / APK (`--dart-define=API_URL=...`) | **Participante:** `ParticipanteEvento@gmail.com` / `Participante123`<br>*(Permite también registro libre desde "Crear cuenta")* |
| **Documentación Swagger / OpenAPI** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | — |
| **Health Check (Actuator)** | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) | — |

---

## Arquitectura del Sistema

El proyecto implementa principios de **Arquitectura Limpia y Modular**, erradicando archivos monolíticos ("God Objects") y garantizando alta cohesión y bajo acoplamiento:

### 1. Backend: Package-by-Feature + MVC Multicapa
El backend organiza el código en torno a **Dominios de Negocio** (`com.plataformaeventos.<dominio>`). Cada dominio implementa estrictamente 5 subcapas aisladas:

* **`controlador/` (Presentación / REST API):** Expone los endpoints HTTP, mapea rutas, valida DTOs de entrada con `@Valid` y retorna códigos de estado HTTP semánticos.
* **`servicio/` (Lógica de Negocio):** Implementa las reglas de negocio, validaciones complejas, coordinación entre entidades y transaccionalidad con `@Transactional`.
* **`modelo/` (Entidades de Dominio):** Entidades JPA mapeadas a la base de datos relacional con anotaciones de integridad referencial.
* **`repositorio/` (Capa de Persistencia):** Interfaces de Spring Data JPA que abstraen el acceso y las consultas personalizadas a la base de datos.
* **`dto/` (Transferencia de Datos):** DTOs inmutables (Java Records o clases especializadas) para Requests y Responses, previniendo sobreexposición de entidades JPA.

### 2. Frontend: Modularidad en Features + Standalone Components
El frontend en Angular 20 elimina los "God Models" y "God Services" centralizados:

* **Desacoplamiento de Servicios:** Cada dominio funcional en `src/app/features/<dominio>/servicios/` expone únicamente sus propios métodos HTTP y lógica de consumo de API.
* **Modelos Tipados por Dominio:** Cada feature define sus interfaces y tipos TypeScript en `src/app/features/<dominio>/modelos/`.
* **Capa Core Aislada:** `src/app/core/` aloja únicamente utilidades transversales (interceptores JWT, guards de rutas, gestión de sesión global con Signals y notificaciones Toast).

---

## Estructura del Proyecto

### Árbol General de Directorios
```text
plataforma-gestion-eventos/
├── backend/            # API REST Spring Boot 3 (Java 21, JPA, Security JWT)
├── frontend-web/       # Panel Administrativo Angular 20 (Standalone + Signals)
├── aplicacion-movil/   # Cliente Móvil Flutter (Asistentes y Staff)
├── servicio-ia/        # Microservicio FastAPI (Python)
├── documentacion/      # Arquitectura, guías de despliegue AWS y Postman
├── .agents/            # Configuración y estándares para agentes de IA
├── docker-compose.yml  # Orquestación de contenedores locales
└── .env.example        # Plantilla oficial de variables de entorno
```

### Estructura Detallada del Backend (`backend/src/main/java/com/plataformaeventos/`)
```text
com.plataformaeventos/
├── PlataformaGestionEventosAplicacion.java   # Clase principal de arranque
│
├── autenticacion/                            # Dominio de Inicio de Sesión y JWT
│   ├── controlador/                          # AutenticacionControlador
│   ├── servicio/                             # AutenticacionServicio
│   └── dto/                                  # AutenticacionSolicitud, TokenRespuesta...
│
├── usuarios/                                 # Dominio de Usuarios del Sistema
│   ├── controlador/                          # UsuarioControlador
│   ├── servicio/                             # UsuarioServicio
│   ├── modelo/                               # Usuario, EstadoUsuario
│   ├── repositorio/                          # UsuarioRepository
│   └── dto/                                  # CrearUsuarioDto, UsuarioRespuesta...
│
├── roles/                                    # Dominio de Roles y Jerarquías
│   ├── controlador/                          # RolControlador
│   ├── servicio/                             # RolServicio
│   ├── modelo/                               # Rol
│   ├── repositorio/                          # RolRepository
│   └── dto/                                  # RolDto, AsignarRolDto...
│
├── permisos/                                 # Dominio de Permisos Granulares
│   ├── controlador/                          # PermisoControlador
│   ├── servicio/                             # PermisoServicio
│   ├── modelo/                               # Permiso
│   ├── repositorio/                          # PermisoRepository
│   └── dto/                                  # PermisoDto...
│
├── eventos/                                  # Dominio Principal de Eventos
│   ├── controlador/                          # EventoControlador
│   ├── servicio/                             # EventoServicio
│   ├── modelo/                               # Evento, EstadoEvento, Modalidad...
│   ├── repositorio/                          # EventoRepository
│   └── dto/                                  # CrearEventoDto, EventoDetalleDto...
│
├── categorias/                               # Dominio de Categorías de Eventos
│   ├── controlador/                          # CategoriaControlador
│   ├── servicio/                             # CategoriaServicio
│   ├── modelo/                               # Categoria
│   ├── repositorio/                          # CategoriaRepository
│   └── dto/                                  # CategoriaDto, CrearCategoriaDto...
│
├── espacios/                                 # Dominio de Espacios Físicos y Locaciones
│   ├── controlador/                          # EspacioControlador
│   ├── servicio/                             # EspacioServicio
│   ├── modelo/                               # Espacio
│   ├── repositorio/                          # EspacioRepository
│   └── dto/                                  # EspacioDto, CrearEspacioDto...
│
├── recursos/                                 # Dominio de Recursos e Inventario
│   ├── controlador/                          # RecursoControlador
│   ├── servicio/                             # RecursoServicio
│   ├── modelo/                               # Recurso, EstadoRecurso...
│   ├── repositorio/                          # RecursoRepository
│   └── dto/                                  # RecursoDto, AsignarRecursoDto...
│
├── convocatorias/                            # Dominio de Convocatorias y Postulaciones
│   ├── controlador/                          # ConvocatoriaControlador
│   ├── servicio/                             # ConvocatoriaServicio
│   ├── modelo/                               # Convocatoria, Postulacion...
│   ├── repositorio/                          # ConvocatoriaRepository
│   └── dto/                                  # ConvocatoriaDto...
│
├── perfil/                                   # Dominio de Perfil de Usuario Autenticado
│   ├── controlador/                          # PerfilControlador
│   ├── servicio/                             # PerfilServicio
│   └── dto/                                  # ActualizarPerfilDto, PerfilRespuesta...
│
├── configuracion/                            # Infraestructura: CORS, Seed, OpenAPI, App
│   ├── AdministradorInicializador.java       # Siembra de cuentas y roles por defecto
│   ├── ConfiguracionCors.java
│   └── ConfiguracionOpenApi.java
├── seguridad/                                # Infraestructura: Spring Security & JWT
│   ├── FiltroAutenticacionJwt.java
│   ├── ProveedorTokenJwt.java
│   └── ConfiguracionSeguridad.java
└── excepciones/                              # Manejador global de excepciones REST
    ├── ExcepcionRecursoNoEncontrado.java
    └── ManejadorGlobalExcepciones.java
```

### Estructura Detallada del Frontend Web (`frontend-web/src/app/`)
```text
src/app/
├── app.config.ts                             # Configuración global y proveedores HTTP
├── app.routes.ts                             # Enrutador principal con Lazy Loading y Guards
├── app.ts / app.html / app.css               # Shell raíz de la aplicación
│
├── core/                                     # Servicios transversales de infraestructura
│   ├── autenticacion.interceptor.ts          # Interceptor HTTP para adjuntar JWT
│   ├── guards.ts                             # Guards de autenticación y autorización por rol
│   ├── sesion.service.ts                     # Estado reactivo global de la sesión (Signals)
│   ├── toast.service.ts                      # Sistema de notificaciones toast al usuario
│   ├── validacion-clave.ts                   # Validadores de contraseñas robustas
│   ├── mensajes-error.ts                     # Mapeo unificado de errores del API
│   └── modelos.ts                            # Re-export de modelos y tipos base
│
├── features/                                 # Módulos organizados por Dominio de Negocio
│   ├── auth/                                 # Inicio de sesión y recuperación
│   │   ├── modelos/auth.modelo.ts
│   │   ├── servicios/auth.service.ts
│   │   └── login.component.*
│   ├── usuarios/                             # Gestión CRUD de usuarios
│   │   ├── modelos/usuario.modelo.ts
│   │   ├── servicios/usuarios.service.ts
│   │   └── usuarios-lista.component.*
│   ├── roles/                                # Gestión de roles y permisos
│   │   ├── modelos/rol.modelo.ts
│   │   └── servicios/roles.service.ts
│   ├── eventos/                              # Gestión de eventos, calendario y estados
│   │   ├── modelos/evento.modelo.ts
│   │   ├── servicios/eventos.service.ts
│   │   └── eventos-lista.component.*
│   ├── categorias/                           # Categorías de eventos
│   │   ├── modelos/categoria.modelo.ts
│   │   └── servicios/categorias.service.ts
│   ├── espacios/                             # Gestión de espacios y capacidad
│   │   ├── modelos/espacio.modelo.ts
│   │   └── servicios/espacios.service.ts
│   ├── recursos/                             # Asignación y catálogo de recursos
│   │   ├── modelos/recurso.modelo.ts
│   │   └── servicios/recursos.service.ts
│   ├── convocatorias/                        # Publicación y evaluación de convocatorias
│   │   ├── modelos/convocatoria.modelo.ts
│   │   └── servicios/convocatorias.service.ts
│   ├── perfil/                               # Mi cuenta y cambio de clave
│   │   ├── modelos/perfil.modelo.ts
│   │   └── servicios/perfil.service.ts
│   └── panel/                                # Dashboard y navegación principal
│       └── panel-layout.component.*
│
└── shared/                                   # Componentes UI reutilizables
    ├── ui/                                   # Botones, modales, tablas, alertas
    └── componentes/                          # Navbar, sidebar, pie de página
```

---

## Requisitos Previos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado y en ejecución.
- [Git](https://git-scm.com/).
- *(Opcional para desarrollo sin Docker)*: Java 21 JDK, Node.js 20+, Flutter SDK.

---

## Setup Rápido (Entorno Docker)

```bash
# 1. Clonar el repositorio y entrar al proyecto
git clone <url-del-repositorio>
cd plataforma-gestion-eventos

# 2. Copiar y configurar variables de entorno locales
cp .env.example .env
# (Edita .env con tus contraseñas y puertos locales si es necesario)

# 3. Construir y levantar los contenedores
docker compose up -d --build

# 4. Verificar el estado de los contenedores
docker compose ps

# 5. Inspeccionar las tablas creadas en PostgreSQL
docker compose exec base-datos psql -U eventos_app -d eventos -c "\dt"
```

---

## Comandos Esenciales de Consola (Docker)

```bash
# Levantar todos los servicios en segundo plano
docker compose up -d

# Levantar y reconstruir tras cambios en el código
docker compose up -d --build

# Ver logs en tiempo real del backend
docker compose logs -f backend

# Ver logs en tiempo real del frontend
docker compose logs -f frontend-web

# Reiniciar un servicio específico
docker compose restart backend
docker compose restart frontend-web

# Entrar a la consola interactiva de PostgreSQL
docker compose exec base-datos psql -U eventos_app -d eventos

# Ejecutar tests de Backend dentro del contenedor
docker compose exec backend ./mvnw test

# Detener todos los contenedores
docker compose down

# Resetear base de datos limpia (borra el volumen persistente y vuelve a sembrar)
docker compose down -v
docker compose up -d --build
```

---

## Pruebas y Verificación de Calidad

### Backend (Spring Boot 3 + JUnit 5)
Para verificar la compilación y la suite completa de 38 pruebas unitarias y de integración:
```bash
cd backend
./mvnw clean test
```
> **Resultado esperado:** `BUILD SUCCESS` (38/38 tests en verde).

### Frontend (Angular 20)
Para compilar y validar que los tipos e interfaces estén 100% integrados sin errores:
```bash
cd frontend-web
npm run build
```
> **Resultado esperado:** Compilación exitosa en `dist/frontend-web` con 0 errores de tipado.

### App Móvil (Flutter - Casos de Uso CU11, CU12, CU13)
Para ejecutar la suite de pruebas automatizadas de widgets y flujos de inscripción:
```bash
cd aplicacion-movil
flutter test
```
> **Resultado esperado:** `All tests passed!` (7/7 tests en verde).

**Ejecución en Emulador Android:**
```bash
cd aplicacion-movil
flutter run
```
*(Se conecta automáticamente a `http://10.0.2.2:8080/api`).*

**Compilación de APK para celulares físicos o servidores remotos:**
```bash
cd aplicacion-movil
flutter build apk --dart-define=API_URL=https://tu-servidor.com/api
```

---

## Desarrollo Local Híbrido (Sin Docker en Backend/Frontend)

Si prefieres programar con recarga rápida levantando solo la Base de Datos en Docker:

```bash
# 1. Levantar solo PostgreSQL
docker compose up -d base-datos

# 2. Levantar Backend en tu terminal (requiere Java 21)
cd backend
./mvnw spring-boot:run

# 3. Levantar Frontend Web en otra terminal (requiere Node 20)
cd frontend-web
npm install
npm start
```

---

## Solución de Problemas (Troubleshooting)

* **Error de conexión a PostgreSQL (`Connection refused`):** Verifica que el contenedor `base-datos` esté corriendo con `docker compose ps` y que el puerto `5432` no esté ocupado por otra instancia local.
* **Correos transaccionales:** La aplicación utiliza **Brevo** para despachar correos reales (asegúrate de que `BREVO_API_KEY` y `BREVO_FROM_EMAIL` estén definidos en tu archivo `.env`).
* **Reseteo de Base de Datos y Re-siembra:** Si realizas cambios en entidades JPA y deseas reiniciar la base de datos con las cuentas por defecto limpias:
  ```bash
  docker compose down -v
  docker compose up -d --build
  ```

---

## Licencia
Proyecto académico — Sistemas de Información II.
