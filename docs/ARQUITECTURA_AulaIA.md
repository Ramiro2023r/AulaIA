# Arquitectura Técnica — AulaIA

## 1. Información del documento

- **Proyecto:** AulaIA — Sistema Inteligente de Asistencia Escolar
- **Documento:** Arquitectura Técnica
- **Versión:** 1.0
- **Fecha:** 16 de agosto de 2026
- **Estado:** Base inicial para desarrollo
- **Objetivo:** Definir la arquitectura técnica del sistema para orientar el desarrollo humano y asistido por IA.

---

## 2. Visión general

AulaIA es un sistema web para registrar y analizar la asistencia de estudiantes de primaria en clases de computación.

El sistema permitirá que cada estudiante registre su asistencia utilizando:

- Código QR individual.
- Cámara de la laptop o computadora.
- Código manual como método de respaldo.

Después de registrar la asistencia, el sistema mostrará una confirmación visual y reproducirá un mensaje por voz.

El profesor contará con un dashboard para consultar:

- Estudiantes presentes.
- Tardanzas.
- Ausencias.
- Porcentaje de asistencia.
- Historiales.
- Reportes.
- Análisis generados por IA.

La arquitectura se diseñará separando claramente:

1. Interfaz web.
2. Backend principal.
3. Base de datos.
4. Servicio de Inteligencia Artificial.
5. Integraciones técnicas.

---

# 3. Arquitectura de alto nivel

```text
                           ┌─────────────────────────┐
                           │        USUARIOS         │
                           │                         │
                           │ Estudiante / Profesor   │
                           │      Administrador      │
                           └────────────┬────────────┘
                                        │
                                        │ HTTPS
                                        ▼
                           ┌─────────────────────────┐
                           │        ANGULAR          │
                           │       Frontend Web      │
                           │                         │
                           │ - Modo Aula             │
                           │ - Dashboard             │
                           │ - Administración        │
                           │ - Reportes              │
                           │ - Asistente IA          │
                           └────────────┬────────────┘
                                        │
                                     REST API
                                        │
                                        ▼
                           ┌─────────────────────────┐
                           │      SPRING BOOT        │
                           │      Backend Core       │
                           │                         │
                           │ - Autenticación         │
                           │ - Estudiantes           │
                           │ - Horarios              │
                           │ - Asistencias           │
                           │ - Reportes              │
                           │ - Seguridad             │
                           │ - Auditoría             │
                           └─────────┬───────┬───────┘
                                     │       │
                           SQL / JPA │       │ HTTP REST
                                     │       │
                                     ▼       ▼
                    ┌───────────────────┐  ┌───────────────────┐
                    │    POSTGRESQL     │  │      FASTAPI      │
                    │                   │  │   Servicio IA     │
                    │ - Usuarios        │  │                   │
                    │ - Estudiantes     │  │ - Pandas          │
                    │ - Cursos          │  │ - scikit-learn    │
                    │ - Horarios        │  │ - Análisis        │
                    │ - Asistencias     │  │ - Predicciones    │
                    │ - Auditoría       │  │ - Resúmenes       │
                    └───────────────────┘  └───────────────────┘
```

---

# 4. Principios arquitectónicos

La arquitectura de AulaIA deberá seguir los siguientes principios:

## 4.1 Separación de responsabilidades

Cada componente tendrá responsabilidades claramente definidas.

- Angular se encargará de la experiencia de usuario.
- Spring Boot contendrá la lógica de negocio.
- PostgreSQL almacenará la información persistente.
- FastAPI ejecutará los procesos de análisis e Inteligencia Artificial.

La lógica crítica de asistencia nunca deberá depender del frontend.

---

## 4.2 Backend como fuente de verdad

Spring Boot será la autoridad para validar:

- Identidad del estudiante.
- Curso.
- Sección.
- Horario.
- Estado de la sesión.
- Duplicados.
- Tardanzas.
- Ausencias.
- Permisos.
- Auditoría.

Angular no decidirá si una asistencia es válida.

---

## 4.3 Seguridad por diseño

El sistema trabaja con información de menores de edad.

Por lo tanto:

- No se utilizará reconocimiento facial.
- No se almacenarán huellas.
- Los QR no deben contener información sensible.
- La autorización deberá implementarse por roles.
- Las modificaciones de asistencia deben quedar auditadas.
- La IA no deberá emitir diagnósticos o sanciones.

---

## 4.4 Modularidad

Cada dominio deberá desarrollarse como módulo independiente.

Ejemplo:

```text
auth
usuarios
estudiantes
docentes
grados
secciones
cursos
horarios
sesiones
asistencias
reportes
auditoria
ia
```

---

# 5. Stack tecnológico

## Frontend

```text
Angular
TypeScript
HTML5
CSS / SCSS
RxJS
Angular Router
Angular HttpClient
```

Funciones adicionales:

```text
ZXing
Web Speech API
Camera API
```

---

## Backend

```text
Java 21
Spring Boot 3.x
Spring Web
Spring Data JPA
Spring Security
Bean Validation
JWT
Flyway
OpenAPI / Swagger
MapStruct
Lombok
```

---

## Base de datos

```text
PostgreSQL
```

Acceso:

```text
Spring Data JPA
Hibernate
Flyway
```

---

## Inteligencia Artificial

```text
Python 3.12+
FastAPI
Pandas
NumPy
scikit-learn
Pydantic
Uvicorn
```

Opcionalmente se podrá integrar posteriormente un modelo generativo mediante API.

---

# 6. Arquitectura del frontend

La aplicación Angular se dividirá por dominios.

```text
frontend/
└── src/
    └── app/
        ├── core/
        │   ├── auth/
        │   ├── guards/
        │   ├── interceptors/
        │   ├── services/
        │   └── models/
        │
        ├── shared/
        │   ├── components/
        │   ├── directives/
        │   ├── pipes/
        │   └── utils/
        │
        ├── features/
        │   ├── dashboard/
        │   ├── estudiantes/
        │   ├── docentes/
        │   ├── cursos/
        │   ├── secciones/
        │   ├── horarios/
        │   ├── asistencias/
        │   ├── modo-aula/
        │   ├── reportes/
        │   └── inteligencia-artificial/
        │
        └── layouts/
```

---

# 7. Modo Aula

El Modo Aula será una interfaz especializada para registrar estudiantes rápidamente.

## Pantalla principal

```text
┌─────────────────────────────────────┐
│               AulaIA                │
│                                     │
│        COMPUTACIÓN — 6.º A          │
│                                     │
│     Escanea tu código QR            │
│                                     │
│             [ CÁMARA ]              │
│                                     │
│        Ingresar código manual       │
└─────────────────────────────────────┘
```

La cámara permanecerá lista para detectar códigos QR.

---

# 8. Arquitectura del QR

El QR no almacenará directamente:

- Nombre.
- DNI.
- Dirección.
- Datos familiares.
- Información académica sensible.

Debe almacenar únicamente un identificador seguro.

Ejemplo:

```text
AULAIA:STUDENT:7QX9K2
```

Flujo:

```text
QR
 ↓
Cámara
 ↓
Angular
 ↓
ZXing
 ↓
Token / código
 ↓
POST /api/v1/asistencias/registrar
 ↓
Spring Boot
 ↓
Validaciones
 ↓
PostgreSQL
```

---

# 9. Registro manual

Cuando la cámara falle, se utilizará un código manual.

Ejemplo:

```text
EST-000145
```

El frontend enviará exactamente el mismo proceso de registro al backend.

Nunca debe existir una lógica de asistencia diferente para QR y código manual.

Ambos métodos convergen en el mismo servicio:

```text
AsistenciaService.registrar(...)
```

---

# 10. Flujo de registro de asistencia

```text
Alumno
  │
  ▼
Escanea QR
  │
  ▼
Angular obtiene identificador
  │
  ▼
POST /api/v1/asistencias/registrar
  │
  ▼
Spring Boot
  │
  ├─ Buscar sesión activa
  ├─ Buscar estudiante
  ├─ Validar sección
  ├─ Validar curso
  ├─ Validar horario
  ├─ Validar duplicado
  ├─ Obtener hora servidor
  ├─ Calcular estado
  │
  ▼
Guardar asistencia
  │
  ▼
Responder
```

Ejemplo de respuesta:

```json
{
  "success": true,
  "estudiante": {
    "nombre": "Juan"
  },
  "hora": "09:03",
  "estado": "PRESENTE",
  "mensaje": "Asistencia registrada correctamente"
}
```

---

# 11. Estados de asistencia

Estados iniciales:

```text
PRESENTE
TARDANZA
AUSENTE
JUSTIFICADO
```

Ejemplo:

```text
Clase: 09:00

08:55 → PRESENTE
09:03 → PRESENTE
09:12 → TARDANZA
```

La tolerancia debe ser configurable.

Ejemplo:

```text
Inicio: 09:00
Tolerancia: 10 minutos
```

Entonces:

```text
09:00 - 09:10 → PRESENTE
09:11 en adelante → TARDANZA
```

---

# 12. Control de duplicados

La base de datos debe impedir que un estudiante registre dos asistencias para la misma sesión.

Restricción recomendada:

```sql
UNIQUE (sesion_clase_id, estudiante_id)
```

Además, el backend deberá validar previamente.

---

# 13. Sistema de voz

Angular utilizará:

```text
SpeechSynthesisUtterance
```

Ejemplo:

```javascript
const mensaje = new SpeechSynthesisUtterance(
  "¡Hola Juan! Tu asistencia fue registrada correctamente."
);

mensaje.lang = "es-PE";
speechSynthesis.speak(mensaje);
```

---

# 14. Mensajes de voz

## Presente

```text
¡Hola Juan!
Tu asistencia fue registrada correctamente.
¡Que tengas una excelente clase de computación!
```

## Tardanza

```text
¡Hola Juan!
Tu asistencia fue registrada.
Has registrado una tardanza.
Puedes ingresar a tu clase.
```

## Duplicado

```text
Juan, tu asistencia ya fue registrada.
No necesitas volver a registrarte.
```

## Código inválido

```text
No pude encontrar ese código.
Inténtalo nuevamente o solicita ayuda a tu profesor.
```

---

# 15. Arquitectura backend

Estructura recomendada:

```text
backend/
└── src/main/java/com/aulaia/
    ├── config/
    ├── security/
    ├── controller/
    ├── service/
    ├── repository/
    ├── entity/
    ├── dto/
    ├── mapper/
    ├── exception/
    ├── validation/
    ├── client/
    └── audit/
```

---

# 16. Capas backend

## Controller

Responsable de:

- Recibir HTTP.
- Validar DTO.
- Invocar servicios.
- Retornar respuestas.

No deberá contener lógica de negocio compleja.

---

## Service

Contendrá:

- Reglas de negocio.
- Validaciones.
- Transacciones.
- Flujo de asistencia.

---

## Repository

Responsable únicamente de acceso a datos.

---

## DTO

Nunca se expondrán entidades JPA directamente.

Se utilizarán:

```text
Request DTO
Response DTO
```

---

# 17. Módulos backend

```text
AuthModule
UsuarioModule
EstudianteModule
DocenteModule
CursoModule
GradoModule
SeccionModule
HorarioModule
SesionClaseModule
AsistenciaModule
ReporteModule
AuditoriaModule
IAModule
```

---

# 18. Modelo de datos

## usuarios

```text
id
username
password_hash
rol
activo
created_at
updated_at
```

---

## estudiantes

```text
id
codigo
qr_token
nombres
apellidos
seccion_id
activo
created_at
updated_at
```

---

## docentes

```text
id
usuario_id
nombres
apellidos
activo
```

---

## cursos

```text
id
nombre
descripcion
activo
```

---

## grados

```text
id
nombre
nivel
```

Ejemplo:

```text
6.º Primaria
```

---

## secciones

```text
id
grado_id
nombre
periodo_academico
```

Ejemplo:

```text
6.º A
```

---

## horarios

```text
id
curso_id
seccion_id
docente_id
dia_semana
hora_inicio
hora_fin
tolerancia_minutos
activo
```

---

## sesiones_clase

```text
id
horario_id
fecha
hora_apertura
hora_cierre
estado
```

Estados:

```text
PROGRAMADA
ABIERTA
CERRADA
CANCELADA
```

---

## asistencias

```text
id
sesion_clase_id
estudiante_id
fecha_hora
estado
metodo
created_at
```

Método:

```text
QR
CODIGO
MANUAL_DOCENTE
```

---

## auditoria

```text
id
usuario_id
entidad
entidad_id
accion
valor_anterior
valor_nuevo
fecha_hora
```

---

# 19. Relaciones principales

```text
Grado
  │
  └── Secciones
        │
        └── Estudiantes


Docente
   │
   └── Horarios
         │
         └── Sesiones de clase
                 │
                 └── Asistencias
                         │
                         └── Estudiante
```

---

# 20. API REST

Base:

```text
/api/v1
```

---

# 21. Autenticación

```text
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
```

---

# 22. Estudiantes

```text
GET    /api/v1/estudiantes
GET    /api/v1/estudiantes/{id}
POST   /api/v1/estudiantes
PUT    /api/v1/estudiantes/{id}
DELETE /api/v1/estudiantes/{id}

GET /api/v1/estudiantes/{id}/qr
```

---

# 23. Cursos

```text
GET    /api/v1/cursos
POST   /api/v1/cursos
PUT    /api/v1/cursos/{id}
DELETE /api/v1/cursos/{id}
```

---

# 24. Horarios

```text
GET  /api/v1/horarios
POST /api/v1/horarios
PUT  /api/v1/horarios/{id}
```

---

# 25. Sesiones

```text
POST /api/v1/sesiones/{id}/abrir
POST /api/v1/sesiones/{id}/cerrar

GET /api/v1/sesiones/activas
```

---

# 26. Asistencias

Registro:

```text
POST /api/v1/asistencias/registrar
```

Request:

```json
{
  "codigo": "AULAIA:STUDENT:7QX9K2",
  "metodo": "QR"
}
```

Respuesta:

```json
{
  "success": true,
  "nombre": "Juan",
  "estado": "PRESENTE",
  "hora": "09:03",
  "mensaje": "Asistencia registrada correctamente"
}
```

Consulta:

```text
GET /api/v1/asistencias
GET /api/v1/asistencias/hoy
GET /api/v1/asistencias/sesion/{id}
GET /api/v1/asistencias/estudiante/{id}
```

---

# 27. Reportes

```text
GET /api/v1/reportes/asistencia
GET /api/v1/reportes/asistencia/excel
GET /api/v1/reportes/asistencia/pdf
```

Filtros:

```text
fecha_inicio
fecha_fin
curso_id
seccion_id
estudiante_id
estado
```

---

# 28. Arquitectura de Inteligencia Artificial

El sistema utilizará un servicio independiente:

```text
FastAPI
```

Spring Boot se comunicará mediante HTTP REST.

```text
Spring Boot
     │
     ▼
FastAPI
     │
     ├── Pandas
     ├── NumPy
     └── scikit-learn
```

---

# 29. Responsabilidades del servicio IA

El servicio IA podrá:

- Analizar históricos.
- Calcular tendencias.
- Detectar patrones.
- Identificar recurrencia de tardanzas.
- Identificar cambios importantes de asistencia.
- Generar resúmenes estadísticos.
- Ejecutar modelos predictivos cuando exista suficiente información.

No debe:

- Registrar asistencias.
- Modificar estudiantes.
- Cambiar horarios.
- Alterar datos directamente.

---

# 30. API de IA

Ejemplo:

```text
POST /api/v1/analisis/asistencia
```

Request:

```json
{
  "estudianteId": 150,
  "presentes": 18,
  "tardanzas": 3,
  "faltas": 2
}
```

Response:

```json
{
  "porcentajeAsistencia": 78.26,
  "tendencia": "DESCENDENTE",
  "nivelAtencion": "MEDIO"
}
```

---

# 31. Asistente inteligente

En una fase posterior:

```text
Profesor:
¿Cómo estuvo la asistencia de sexto A esta semana?
```

Flujo:

```text
Angular
 ↓
Spring Boot
 ↓
Validación de permisos
 ↓
Consulta de datos
 ↓
IA
 ↓
Respuesta
```

La IA nunca tendrá acceso libre a toda la base de datos.

Spring Boot enviará únicamente los datos necesarios.

---

# 32. Seguridad

Autenticación:

```text
Spring Security
JWT
```

Roles iniciales:

```text
ADMIN
DOCENTE
```

El estudiante no necesita iniciar sesión para el Modo Aula.

Su QR únicamente permite iniciar el proceso de registro.

---

# 33. Permisos

## ADMIN

Puede:

- Gestionar usuarios.
- Gestionar estudiantes.
- Gestionar docentes.
- Gestionar cursos.
- Gestionar horarios.
- Consultar reportes globales.
- Consultar auditoría.

---

## DOCENTE

Puede:

- Consultar sus clases.
- Abrir/cerrar sesiones.
- Consultar asistencias.
- Corregir registros autorizados.
- Descargar reportes de sus clases.
- Utilizar funciones IA autorizadas.

---

# 34. Auditoría

Deben auditarse:

```text
CREAR_ASISTENCIA_MANUAL
MODIFICAR_ASISTENCIA
JUSTIFICAR_ASISTENCIA
ELIMINAR_REGISTRO
MODIFICAR_HORARIO
MODIFICAR_ESTUDIANTE
```

---

# 35. Privacidad de menores

Reglas obligatorias:

1. No implementar reconocimiento facial.
2. No implementar huellas.
3. No guardar fotografías de estudiantes para asistencia.
4. No incluir información personal en QR.
5. Mostrar datos personales solo el tiempo necesario.
6. Limitar acceso según rol.
7. Utilizar HTTPS.
8. No enviar datos innecesarios al servicio IA.
9. Trabajar con datos ficticios durante desarrollo y exposición cuando sea posible.

---

# 36. Manejo de errores

Formato estándar:

```json
{
  "timestamp": "2026-08-16T09:03:00",
  "status": 409,
  "code": "ATTENDANCE_ALREADY_REGISTERED",
  "message": "La asistencia ya fue registrada"
}
```

Códigos recomendados:

```text
STUDENT_NOT_FOUND
SESSION_NOT_ACTIVE
STUDENT_NOT_IN_SECTION
ATTENDANCE_ALREADY_REGISTERED
OUTSIDE_ATTENDANCE_WINDOW
INVALID_QR
UNAUTHORIZED
FORBIDDEN
INTERNAL_ERROR
```

---

# 37. Testing

## Backend

```text
JUnit 5
Mockito
Spring Boot Test
Testcontainers
```

Pruebas mínimas:

```text
registrarAsistenciaCorrectamente()
rechazarCodigoInexistente()
rechazarDuplicado()
registrarTardanza()
rechazarAlumnoOtraSeccion()
rechazarSesionCerrada()
```

---

## Frontend

```text
Angular TestBed
Jasmine / Jest
Playwright
```

Pruebas:

```text
Login
Escaneo QR
Registro manual
Dashboard
Permisos
Errores
```

---

# 38. Testing del servicio IA

```text
pytest
```

Validar:

- Contrato de API.
- Datos faltantes.
- Resultados estadísticos.
- Manejo de errores.
- Modelos cargados correctamente.

---

# 39. Documentación API

Spring Boot expondrá:

```text
/swagger-ui.html
```

o:

```text
/swagger-ui/index.html
```

La especificación será:

```text
OpenAPI 3
```

---

# 40. Estructura del repositorio

```text
aulaia/
│
├── frontend/
│   └── Angular
│
├── backend/
│   └── Spring Boot
│
├── data-science/
│   └── FastAPI
│
├── database/
│   ├── scripts/
│   └── diagrams/
│
├── docs/
│   ├── PRD.docx
│   ├── TRD.docx
│   └── ARQUITECTURA.md
│
├── docker/
│
├── .github/
│   └── workflows/
│
├── docker-compose.yml
└── README.md
```

---

# 41. Estrategia Git

Ramas:

```text
main
development
```

Features:

```text
feature/auth
feature/estudiantes
feature/horarios
feature/asistencias
feature/qr
feature/voz
feature/dashboard
feature/reportes
feature/ia
```

Correcciones:

```text
fix/asistencia-duplicada
fix/qr-camera
```

---

# 42. Docker

Servicios previstos:

```text
docker-compose
│
├── frontend
├── backend
├── postgres
└── fastapi
```

Ejemplo conceptual:

```yaml
services:

  postgres:
    image: postgres

  backend:
    build: ./backend

  fastapi:
    build: ./data-science

  frontend:
    build: ./frontend
```

---

# 43. Despliegue inicial

Arquitectura propuesta:

```text
Angular
   │
   ▼
Vercel
   │
   ▼
Spring Boot
   │
   ▼
Render
   │
   ├──────────► Neon PostgreSQL
   │
   └──────────► FastAPI / Render
```

---

# 44. Variables de entorno

Backend:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD

JWT_SECRET
JWT_EXPIRATION

IA_SERVICE_URL
```

Frontend:

```text
API_URL
```

FastAPI:

```text
MODEL_PATH
ENVIRONMENT
```

Nunca deben subirse secretos al repositorio.

---

# 45. CI/CD

GitHub Actions podrá ejecutar:

```text
Push
 ↓
Build
 ↓
Tests
 ↓
Static Analysis
 ↓
Deploy
```

---

# 46. Observabilidad

El backend debe generar logs para:

- Inicio de sesión.
- Registro de asistencia.
- Errores.
- Integraciones.
- IA.
- Operaciones administrativas.

No deben escribirse passwords ni tokens completos en logs.

---

# 47. Flujo completo del sistema

```text
                  ESTUDIANTE

                      │
                     QR
                      │
                      ▼

               Cámara Laptop
                      │
                      ▼
                   Angular
                      │
                      ▼
                 Spring Boot
                      │
            ┌─────────┴─────────┐
            │                   │
            ▼                   ▼
       PostgreSQL           Servicio IA
            │
            ▼
       Asistencia
            │
            ▼
        Respuesta
            │
            ▼
         Angular
            │
       ┌────┴────┐
       │         │
       ▼         ▼
    Pantalla    Voz
       │
       ▼
  "Presente ✅"

```

---

# 48. Decisiones arquitectónicas

## ADR-001 — No usar reconocimiento facial

**Decisión**

El sistema utilizará QR/código.

**Motivo**

Evitar tratamiento innecesario de información biométrica de menores.

---

## ADR-002 — Spring Boot como autoridad de negocio

**Decisión**

Toda regla de asistencia se ejecutará en backend.

**Motivo**

Evitar manipulación desde frontend y mantener consistencia.

---

## ADR-003 — IA separada del núcleo

**Decisión**

La IA estará detrás de FastAPI.

**Motivo**

Permitir evolucionar modelos sin afectar el backend principal.

---

## ADR-004 — PostgreSQL

**Decisión**

Utilizar base relacional.

**Motivo**

La información de estudiantes, horarios y asistencias posee relaciones claras y requiere integridad.

---

## ADR-005 — Web Speech API

**Decisión**

Utilizar inicialmente Text-to-Speech del navegador.

**Motivo**

Reduce costos y complejidad.

---

# 49. Orden recomendado de implementación

```text
01. Base del repositorio
02. PostgreSQL + migraciones
03. Backend Spring Boot
04. Seguridad
05. Usuarios
06. Estudiantes
07. Grados y secciones
08. Cursos
09. Docentes
10. Horarios
11. Sesiones de clase
12. Asistencia
13. Frontend Angular
14. Login
15. Dashboard
16. Modo Aula
17. QR
18. Registro manual
19. Voz
20. Reportes
21. Auditoría
22. FastAPI
23. Análisis IA
24. Testing integral
25. Docker
26. CI/CD
27. Producción
```

---

# 50. Regla para agentes de IA

Cuando una IA trabaje sobre el proyecto deberá:

1. Analizar el código existente antes de modificarlo.
2. Respetar esta arquitectura.
3. No duplicar lógica de negocio.
4. No modificar contratos de API sin documentarlo.
5. No crear dependencias innecesarias.
6. No guardar secretos en código.
7. Añadir migraciones para cambios de base de datos.
8. Añadir o actualizar pruebas.
9. Ejecutar las pruebas afectadas.
10. Documentar los cambios realizados.
11. No hacer commit, push o deploy sin autorización.
12. Mantener compatibilidad con las funcionalidades existentes.

---

# 51. Definition of Done

Una funcionalidad se considera terminada cuando:

- Está implementada.
- Respeta la arquitectura.
- Tiene validaciones.
- Tiene manejo de errores.
- Tiene seguridad correspondiente.
- Tiene pruebas.
- Las pruebas pasan.
- Está documentada.
- No rompe otras funcionalidades.
- Fue validada manualmente cuando corresponde.

---

# 52. Resultado arquitectónico esperado

La arquitectura final debe permitir que AulaIA sea:

- Fácil de utilizar por estudiantes.
- Fácil de administrar por profesores.
- Seguro para información de menores.
- Modular.
- Escalable.
- Mantenible.
- Testeable.
- Preparado para IA.
- Preparado para despliegue en nube.
- Capaz de crecer posteriormente a otros cursos y aulas.

---

# 53. Resumen

La arquitectura de AulaIA se compone de cuatro elementos centrales:

```text
Angular
+
Spring Boot
+
PostgreSQL
+
FastAPI / Python
```

El registro de asistencia utiliza:

```text
QR
+
Cámara de laptop
+
Código manual
+
Text-to-Speech
```

La Inteligencia Artificial se utilizará para:

```text
Análisis
+
Patrones
+
Alertas informativas
+
Resúmenes
+
Consultas inteligentes
```

La lógica de asistencia permanecerá siempre en Spring Boot, mientras que FastAPI funcionará como servicio especializado de análisis.

Esta separación permitirá desarrollar el sistema por fases sin comprometer seguridad, mantenibilidad ni crecimiento futuro.
