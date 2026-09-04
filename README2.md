# Plataforma de Gestión de Eventos

Sistema integral para la planificación, administración, control de acceso y asignación de recursos para eventos corporativos e institucionales. Desarrollado con arquitectura desacoplada y modular.

---

## Stack Tecnológico

| Capa / Servicio | Tecnología | Puerto Dev | Descripción |
| :--- | :--- | :--- | :--- |
| **Backend API** | Java 21 + Spring Boot 3.5 (Maven) | `:8080` | API REST con Spring Security (JWT), JPA y OpenAPI |
| **Frontend Web** | Angular 20 (TypeScript) | `:4200` | Panel administrativo con componentes Standalone y Signals |
| **App Móvil** | Flutter / Dart | Emulador / Móvil | Cliente móvil para asistentes y staff |
| **Servicio IA** | Python / FastAPI | `:8000` | Microservicio satélite de asistencia inteligente |
| **Base de Datos** | PostgreSQL 17 (Alpine) | `:5432` | Persistencia relacional |
| **Email (Dev)** | Mailpit | `:8025` (Web) / `:1025` (SMTP) | Servidor SMTP local para pruebas de correos |
| **Contenedores** | Docker + Docker Compose | — | Orquestación completa de servicios |

---

## Requisitos Previos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado y en ejecución.
- [Git](https://git-scm.com/).
- *(Opcional para desarrollo sin Docker)*: Java 21 JDK, Node.js 20+, Flutter SDK.

---

## Setup Rápido

```bash
# 1. Clonar el repositorio y entrar al proyecto
git clone <url-del-repositorio>
cd plataforma-gestion-eventos

# 2. Copiar y configurar variables de entorno locales
cp .env.example .env
# (Edita .env con tus contraseñas y puertos locales)

# 3. Construir y levantar los contenedores
docker compose up -d --build

# 4. Verificar el estado de los contenedores
docker compose ps

# 5. (Opcional) Inspeccionar las tablas creadas en PostgreSQL
docker compose exec base-datos psql -U eventos_app -d eventos -c "\dt"
```

---

## Enlaces de Acceso Rápido

| Servicio | URL | Credenciales por Defecto |
| :--- | :--- | :--- |
| **Frontend Web** | [http://localhost:4200](http://localhost:4200) | **Administrador:** `admin@eventos.local` / `AdminLocal2026`<br>**Organizador:** `organizador@eventos.local` / `Organizador123`<br>**Staff:** `staff@eventos.local` / `Staff123`<br>**Participante:** `participante@eventos.local` / `Participante123` |
| **Documentación Swagger / OpenAPI** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | — |
| **Bandeja de Correos Local (Mailpit)** | [http://localhost:8025](http://localhost:8025) | — |
| **Health Check (Actuator)** | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) | — |

---

## Estructura del Proyecto

```text
plataforma-gestion-eventos/
├── backend/            # Spring Boot 3 (Java 21, JPA, DTOs, Seguridad)
├── frontend-web/       # Angular 20 (Panel web administrativo por Features)
├── aplicacion-movil/   # Flutter (App móvil multiplataforma)
├── servicio-ia/        # FastAPI (Servicio satélite de IA)
├── documentacion/      # Arquitectura, guías AWS y colecciones Postman
├── .agents/            # Configuración de agentes de IA y estándares técnicos
├── docker-compose.yml  # Definición de contenedores locales
└── .env.example        # Plantilla oficial de configuración
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

# Reiniciar un servicio
docker compose restart backend
docker compose restart frontend-web

# Entrar a la consola interactiva de PostgreSQL
docker compose exec base-datos psql -U eventos_app -d eventos

# Ejecutar tests de Backend dentro del contenedor
docker compose exec backend ./mvnw test

# Detener todos los contenedores
docker compose down

# Resetear base de datos limpia (borra el volumen persistente)
docker compose down -v
```

---

## Desarrollo Local Híbrido (Sin Docker en Backend/Frontend)

Si prefieres programar con recarga rápida levantando solo la Base de Datos y Mailpit en Docker:

```bash
# 1. Levantar solo PostgreSQL y Mailpit
docker compose up -d base-datos mailpit

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
* **Correos de prueba:** En desarrollo local, la aplicación usa **Mailpit**. Abre [http://localhost:8025](http://localhost:8025) para ver todos los correos entrantes de forma inmediata.
* **Reseteo de Base de Datos:** Si realizas cambios en entidades y deseas reiniciar el volumen limpio:
  ```bash
  docker compose down -v
  docker compose up -d --build
  ```

---

## Licencia
Proyecto académico — Sistemas de Información II.
