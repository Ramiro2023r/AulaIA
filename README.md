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
├── docker-compose.yml  # Placeholder documentado (servicios en sprint posterior)
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

- [x] Sprint 0 — Estructura raíz del repositorio creada (esta tarea)
- [ ] Frontend Angular (pendiente)
- [ ] Backend Spring Boot (pendiente)
- [ ] Servicio FastAPI (pendiente)
- [ ] PostgreSQL y migraciones (pendiente)
- [ ] Docker Compose completo (pendiente)
- [ ] GitHub Actions (pendiente)

## Requisitos mínimos

| Herramienta | Versión sugerida |
|---|---|
| Node.js | 22.x |
| Java | 21 |
| Python | 3.12+ |
| PostgreSQL | 17 |
| Docker | Desktop reciente + Docker Compose |
| Git | Reciente |

## Instrucciones de ejecución

**Pendiente.** El sistema aún está en preparación. Las instrucciones de instalación y ejecución se agregarán al README conforme se implementen los sprints.

## Consideraciones de privacidad y demo

- Este sistema se desarrolla como **prototipo funcional para demostración académica**.
- Todos los datos de desarrollo serán **ficticios** (nunca datos reales de menores).
- **No se utiliza reconocimiento facial ni biometría de estudiantes.**
- El QR contiene únicamente un identificador seguro, sin datos personales.

---
Documentación oficial: `docs/`