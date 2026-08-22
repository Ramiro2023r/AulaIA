# AulaIA — Sistema Inteligente de Asistencia Escolar

## ¿Qué es AulaIA?

AulaIA es un sistema web educativo que automatiza el registro y seguimiento de asistencia de estudiantes de primaria en clases de computación, mediante código QR y código manual, con confirmación visual y por voz, y análisis inteligente de apoyo para el docente.

**Estado actual:** prototipo funcional para demostración académica. Los datos de desarrollo son **ficticios**. No se utiliza reconocimiento facial ni biometría de estudiantes.

## Problema que resuelve

El registro manual de asistencia consume tiempo de clase, genera errores y dificulta consultar históricos y tendencias. En una sala de cómputo, AulaIA convierte la asistencia en un proceso digital, rápido e interactivo:

- Reduce el tiempo del docente al pasar lista.
- Evita registros duplicados y mejora la trazabilidad.
- Detecta tardanzas y ausencias según horario.
- Ofrece una experiencia sencilla para estudiantes de ~11 años.
- Brinda reportes, estadísticas y análisis inteligente al docente.

## Objetivo del sistema

Desarrollar una plataforma web segura y fácil de usar que automatice el registro y seguimiento de asistencia en clases de computación, incorporando interacción por voz e Inteligencia Artificial para apoyar la gestión docente.

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Frontend | Angular, TypeScript, RxJS, ZXing (QR), Web Speech API |
| Backend | Java 21, Spring Boot 3.x, Spring Security, JWT, JPA, Flyway, OpenAPI |
| Base de datos | PostgreSQL |
| IA / Análisis | Python 3.12+, FastAPI, Pandas, NumPy, scikit-learn |
| Contenedores | Docker, Docker Compose |
| CI/CD | GitHub Actions |
| Pruebas | JUnit 5, Mockito, Testcontainers, Jasmine/Jest, Playwright, pytest |

## Arquitectura general

```text
Angular (frontend)
      │  REST API (/api/v1)
      ▼
Spring Boot (backend — fuente de verdad, reglas de negocio)
      │                        │
      ▼                        ▼
PostgreSQL                FastAPI (servicio IA)
(asistencias,             (análisis, patrones,
 estructura académica)     resúmenes)
```

Principios clave:

- **Backend como fuente de verdad:** las reglas de asistencia, horarios y permisos se validan en Spring Boot; el frontend nunca decide si una asistencia es válida.
- **Privacidad por diseño:** sin reconocimiento facial, sin biometría, QR sin datos personales.
- **IA como apoyo:** FastAPI analiza datos autorizados; nunca modifica asistencias ni bloquea el sistema principal.
- **Modularidad:** cada dominio se desarrolla como módulo independiente.

## Estructura del repositorio

```text
aulaia/
├── frontend/           # Angular (SPA, Modo Aula, dashboard, administración)
├── backend/            # Spring Boot (API REST, reglas de negocio, seguridad)
├── data-science/       # FastAPI (servicio de análisis e IA)
├── docs/               # Fuente oficial de decisiones del proyecto
├── docker/             # Archivos auxiliares de Docker
├── .github/
│   └── workflows/      # GitHub Actions (CI/CD)
├── .gitignore
├── README.md
├── docker-compose.yml  # Entorno local: PostgreSQL, FastAPI, backend y frontend
└── .env.example        # Variables de entorno de ejemplo
```

> `docs/` es la **fuente oficial** de decisiones del proyecto (PRD, TRD, Arquitectura, Base de Datos, UI/UX, Flujos, Plan de Ejecución, Dockerización).

## Documentación oficial

Los documentos dentro de `/docs` constituyen la fuente de verdad del proyecto.

Los archivos `.md` son la referencia principal para agentes de IA. Los `.docx` se conservan como versiones documentales.

En caso de conflicto:

1. PRD define qué debe hacer el producto.
2. TRD y Arquitectura definen cómo se construye.
3. Base de Datos define la persistencia.
4. UI/UX define la experiencia visual.
5. Flujos define el comportamiento.
6. Plan de Ejecución define el orden de implementación.
7. Dockerización define el entorno de contenedores.

## Módulos principales

- **Modo Aula:** registro rápido de asistencia por QR (cámara) o código manual, con confirmación visual y por voz.
- **Asistencia:** estados PRESENTE, TARDANZA, AUSENTE y JUSTIFICADO; control de duplicados.
- **Dashboard docente:** indicadores de la clase (presentes, tardanzas, ausentes, porcentaje).
- **Estructura académica:** grados, secciones, cursos, docentes, estudiantes y horarios.
- **Reportes:** consultas y exportación a Excel/PDF.
- **Inteligencia Artificial:** resúmenes, patrones y alertas informativas para el docente.
- **Auditoría:** trazabilidad de modificaciones relevantes.

## Estrategia de desarrollo por sprints

Desarrollo incremental, un prompt/tarea por sprint, siguiendo `docs/07-PLAN_EJECUCION_AulaIA.md`:

```text
SPRINT 0  — Preparación del proyecto
SPRINT 1  — Backend base y PostgreSQL
SPRINT 2  — Seguridad y autenticación
SPRINT 3  — Estructura académica
SPRINT 4  — Estudiantes y QR
SPRINT 5  — Docentes y horarios
SPRINT 6  — Sesiones de clase
SPRINT 7  — Núcleo de asistencia
SPRINT 8  — Frontend Angular base
SPRINT 9  — Login y seguridad frontend
SPRINT 10 — Modo Aula y QR
SPRINT 11 — Voz y experiencia del estudiante
SPRINT 12 — Dashboard docente
SPRINT 13 — Administración frontend
SPRINT 14 — Justificaciones y auditoría
SPRINT 15 — Reportes
SPRINT 16 — FastAPI e IA
SPRINT 17 — Consultas inteligentes
SPRINT 18 — Testing integral
SPRINT 19 — Docker y entorno local
SPRINT 20 — CI/CD
SPRINT 21 — Preparación para producción
SPRINT 22 — Despliegue
SPRINT 23 — Piloto y validación final
SPRINT 24 — Mejoras post-MVP
```

## Estado actual del desarrollo

El repositorio contiene los módulos Angular, Spring Boot, FastAPI, PostgreSQL
con migraciones Flyway y el entorno local completo de Docker Compose. La
ejecución reproducible se describe en la sección «Ejecutar con Docker».

## Requisitos mínimos

| Herramienta | Versión sugerida |
|---|---|
| Node.js | 22.x |
| Java | 21 |
| Python | 3.12+ |
| PostgreSQL | 17 |
| Docker | Desktop reciente + Docker Compose |
| Git | Reciente |

## Ejecutar con Docker

La forma recomendada de ejecutar AulaIA en otra PC es Docker Desktop. No se
instalan localmente Java, Node, Python ni PostgreSQL: Docker Compose construye
y conecta los cuatro servicios.

### 1. Requisitos

- Docker Desktop en ejecución, con Docker Compose v2.
- Git para clonar el repositorio.

Comprueba la instalación:

```powershell
docker --version
docker compose version
```

### 2. Preparar las variables

Después de clonar el proyecto, crea un archivo `.env` local que nunca se sube
al repositorio:

```powershell
Copy-Item .env.example .env
```

En macOS/Linux:

```bash
cp .env.example .env
```

Edita únicamente `.env` y reemplaza estos valores obligatorios:

- `POSTGRES_PASSWORD`
- `JWT_SECRET` (valor aleatorio de al menos 32 caracteres)

Las credenciales de Telegram son opcionales. Con `TELEGRAM_ENABLED=false` y
token vacío, Telegram queda deshabilitado sin impedir el arranque. Para
habilitarlo, completa `TELEGRAM_BOT_TOKEN` y `TELEGRAM_BOT_USERNAME` en `.env`;
no es necesario reconstruir las imágenes si solo cambian esas variables.

### 3. Construir e iniciar

```powershell
docker compose up --build
```

Para ejecutarlo en segundo plano:

```powershell
docker compose up --build -d
```

Docker Compose crea una red interna (`aulaia_network`) y un volumen persistente
(`postgres_data`). Flyway ejecuta las migraciones al iniciar el backend. En el
perfil `dev` también se habilitan los usuarios de demostración existentes,
salvo que se defina `APP_DEMO_USERS_ENABLED=false`.

### 4. URLs locales

| Servicio | URL |
|---|---|
| Frontend Angular | http://localhost:4200 |
| Backend Spring Boot | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| FastAPI | http://localhost:8000 |
| Documentación FastAPI | http://localhost:8000/docs |
| Salud FastAPI | http://localhost:8000/health |
| Salud backend | http://localhost:8080/actuator/health |

PostgreSQL no publica un puerto al host: solo se accede desde la red interna de
Docker mediante el backend. Sus datos quedan guardados en `postgres_data`.

El frontend servido por Nginx consume `/api/v1` bajo su mismo origen y Nginx lo
reenvía al servicio `backend`; por ello el navegador no depende de resolver
nombres internos Docker. Para desarrollo sin Docker, Angular conserva su
configuración de desarrollo hacia `http://localhost:8080/api/v1`.

### Operaciones útiles

```powershell
# Ver el estado y los logs de todos los servicios
docker compose ps
docker compose logs -f

# Ver solamente el backend
docker compose logs -f backend

# Detener sin perder la base de datos
docker compose down

# Detener y borrar también los datos PostgreSQL (acción irreversible local)
docker compose down -v

# Reconstruir tras modificar código o Dockerfiles
docker compose up --build
```

## Consideraciones de privacidad y demo

- Este sistema se desarrolla como **prototipo funcional para demostración académica**.
- Todos los datos de desarrollo serán **ficticios** (nunca datos reales de menores).
- **No se utiliza reconocimiento facial ni biometría de estudiantes.**
- El QR contiene únicamente un identificador seguro, sin datos personales.

---
Documentación oficial: `docs/`
