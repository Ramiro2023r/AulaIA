# Plan de Ejecución por Sprints y Prompts — AulaIA

## 1. Información del documento

- **Proyecto:** AulaIA — Sistema Inteligente de Asistencia Escolar
- **Documento:** Plan Maestro de Ejecución por Sprints y Prompts para Agentes de IA
- **Versión:** 1.0
- **Fecha:** 16 de agosto de 2026
- **Objetivo:** Guiar a una IA de desarrollo desde un repositorio vacío hasta una versión funcional, probada y preparada para producción, mediante prompts pequeños, secuenciales y verificables.
- **Stack:** Angular + Spring Boot + PostgreSQL + FastAPI/Python
- **Método de trabajo:** Un prompt por tarea pequeña. No avanzar al siguiente si el actual no está implementado y validado.

---

# 2. Regla principal de trabajo con IA

La IA NO debe recibir un único prompt para construir todo el sistema.

Debe trabajar:

```text
Sprint
  ↓
Tarea
  ↓
Prompt pequeño
  ↓
Implementación
  ↓
Pruebas
  ↓
Validación
  ↓
Siguiente prompt
```

Cada prompt de este documento está diseñado para ser enviado de forma independiente.

La IA debe conservar el contexto del proyecto y respetar los documentos:

```text
PRD
TRD
ARQUITECTURA.md
BASE_DE_DATOS_AulaIA.md
UI_UX_AulaIA.md
FLUJOS_AulaIA.md
PLAN_EJECUCION_AulaIA.md
```

---

# 3. Instrucción base para TODOS los prompts

Antes de cada prompt técnico puede añadirse este encabezado:

```text
Estás trabajando en el proyecto AulaIA.

Antes de modificar código:

1. Revisa la estructura actual del repositorio.
2. Lee los documentos del proyecto en /docs.
3. Respeta PRD, TRD, Arquitectura, Base de Datos, UI/UX y Flujos.
4. No reescribas módulos que ya funcionan sin necesidad.
5. No cambies contratos existentes sin justificarlo.
6. No hagas commit, push ni deploy.
7. Implementa únicamente lo solicitado en este prompt.
8. Añade o actualiza pruebas.
9. Ejecuta las pruebas relacionadas.
10. Al terminar, informa:
   - archivos creados,
   - archivos modificados,
   - decisiones tomadas,
   - pruebas ejecutadas,
   - resultado,
   - pendientes reales.
11. Si encuentras un problema previo que bloquea la tarea, corrígelo solo si es estrictamente necesario y documenta el motivo.
12. No avances a tareas futuras.
```

---

# 4. Estrategia general de sprints

```text
SPRINT 0  — Preparación del proyecto
SPRINT 1  — Backend base y PostgreSQL
SPRINT 2  — Seguridad y autenticación
SPRINT 3  — Estructura académica
SPRINT 4  — Estudiantes y QR
SPRINT 5  — Docentes y horarios
SPRINT 6  — Sesiones de clase
SPRINT 7  — Asistencia
SPRINT 8  — Frontend Angular base
SPRINT 9  — Modo Aula y cámara QR
SPRINT 10 — Voz y experiencia estudiante
SPRINT 11 — Dashboard docente
SPRINT 12 — Administración
SPRINT 13 — Justificaciones y auditoría
SPRINT 14 — Reportes
SPRINT 15 — FastAPI e IA
SPRINT 16 — Integración completa
SPRINT 17 — Testing integral
SPRINT 18 — Docker y CI/CD
SPRINT 19 — Preparación para producción
SPRINT 20 — Piloto y cierre técnico
```

---

# SPRINT 0 — PREPARACIÓN DEL PROYECTO

## Objetivo

Crear una base limpia y profesional antes de programar funcionalidad.

---

## Prompt 0.1 — Crear estructura raíz

```text
Crea la estructura raíz del proyecto AulaIA.

Debe quedar preparada para:

- frontend Angular
- backend Spring Boot
- data-science FastAPI
- documentación
- Docker
- GitHub Actions

Estructura objetivo:

aulaia/
├── frontend/
├── backend/
├── data-science/
├── docs/
├── docker/
├── .github/workflows/
├── .gitignore
├── README.md
└── docker-compose.yml

No implementes todavía lógica de negocio.

Crea un README inicial con:
- objetivo del proyecto,
- stack,
- estructura,
- requisitos mínimos,
- instrucciones todavía marcadas como pendientes.

Valida que no existan archivos temporales o secretos.
```

### Validación

```text
estructura creada
README existe
.gitignore existe
sin secretos
```

---

## Prompt 0.2 — Preparar documentación del repositorio

```text
Crea dentro de /docs una estructura ordenada para los documentos del proyecto.

Usa nombres como:

01-PRD
02-TRD
03-ARQUITECTURA
04-BASE-DE-DATOS
05-UI-UX
06-FLUJOS
07-PLAN-EJECUCION

No cambies contenido funcional de documentos existentes.

Actualiza README indicando que /docs es la fuente oficial de decisiones del proyecto.
```

---

## Prompt 0.3 — Definir estrategia Git

```text
Documenta en README o CONTRIBUTING.md la estrategia Git del proyecto.

Ramas principales:

main
development

Tipos de ramas:

feature/
fix/
refactor/
docs/
test/
chore/

Define ejemplos:

feature/auth
feature/asistencia
feature/qr
feature/ia

Define formato de commits tipo Conventional Commits.

No crees ramas ni hagas commits.
Solo documenta la estrategia.
```

---

# SPRINT 1 — BACKEND BASE Y POSTGRESQL

## Prompt 1.1 — Crear Spring Boot

```text
Inicializa el backend AulaIA con Java 21 y Spring Boot 3.x.

Dependencias:

- Spring Web
- Spring Data JPA
- Spring Security
- Validation
- PostgreSQL Driver
- Flyway
- Lombok
- MapStruct
- Springdoc OpenAPI
- Spring Boot Test

Package base:

com.aulaia

Configura estructura:

config
controller
service
repository
entity
dto
mapper
exception
security
client
audit

No implementes entidades todavía.

Ejecuta:
- build
- tests iniciales

El proyecto debe arrancar sin errores.
```

---

## Prompt 1.2 — Configurar perfiles

```text
Configura perfiles Spring Boot para:

application.properties
application-dev.properties
application-test.properties

Usa variables de entorno para producción.

No hardcodees credenciales.

Variables esperadas:

DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
JWT_SECRET
IA_SERVICE_URL

Para test usa una configuración aislada apropiada.

Valida que la app pueda arrancar en modo test.
```

---

## Prompt 1.3 — Configurar PostgreSQL y Flyway

```text
Configura Spring Boot para PostgreSQL y Flyway.

No uses ddl-auto=create/update en producción.

Usa:

spring.jpa.hibernate.ddl-auto=validate

Crea la carpeta:

src/main/resources/db/migration

Añade una primera migración mínima de infraestructura si es necesaria.

Valida conexión y arranque con una base PostgreSQL local.
```

---

## Prompt 1.4 — Manejo global de errores

```text
Implementa un manejo global de errores REST.

Crea:

ApiErrorResponse
GlobalExceptionHandler

Formato:

{
  "timestamp": "...",
  "status": 400,
  "code": "...",
  "message": "..."
}

Añade excepciones base:

ResourceNotFoundException
BusinessException
ConflictException

No implementes todavía errores específicos de asistencia.

Añade pruebas.
```

---

## Prompt 1.5 — OpenAPI / Swagger

```text
Configura OpenAPI/Swagger.

Debe estar disponible en desarrollo.

Documenta:
- nombre AulaIA API
- versión
- descripción

No expongas secretos.

Ejecuta backend y valida que Swagger cargue correctamente.
```

---

# SPRINT 2 — SEGURIDAD Y AUTENTICACIÓN

## Prompt 2.1 — Modelo Usuario

```text
Implementa la entidad Usuario según BASE_DE_DATOS_AulaIA.md.

Campos principales:

id
username
passwordHash
rol
activo
ultimoLoginAt
createdAt
updatedAt

Roles iniciales:

ADMIN
DOCENTE

Crea:
- Entity
- Repository
- Enum
- Migración Flyway
- pruebas de repositorio

No implementes login todavía.
```

---

## Prompt 2.2 — Password encoder y seguridad base

```text
Configura Spring Security.

Implementa PasswordEncoder seguro usando BCrypt.

Configura las rutas públicas mínimas:

/api/v1/auth/**
/swagger-ui/**
/v3/api-docs/**

El resto debe quedar protegido.

No implementes JWT todavía.

Añade pruebas de seguridad básicas.
```

---

## Prompt 2.3 — JWT

```text
Implementa autenticación JWT para AulaIA.

Debe incluir:

JwtService
JwtAuthenticationFilter
SecurityConfig

El token debe contener:
- userId
- username
- rol

Configura expiración mediante variable.

No incluyas información sensible.

Añade pruebas unitarias para generación y validación de token.
```

---

## Prompt 2.4 — Login

```text
Implementa:

POST /api/v1/auth/login

Request:
username
password

Response:
accessToken
tokenType
expiresIn
user:
- id
- username
- rol

Validaciones:

usuario existe
usuario activo
password correcto

Actualizar ultimo_login_at al autenticarse correctamente.

Añade pruebas:
- login correcto
- password incorrecta
- usuario inexistente
- usuario inactivo
```

---

## Prompt 2.5 — Crear administrador inicial

```text
Implementa un mecanismo seguro de creación del primer ADMIN para desarrollo.

No dejes contraseñas reales hardcodeadas.

Puede ser:
- seed condicionado a perfil dev,
o
- comando/documentación explícita.

Documenta cómo crear el administrador inicial.

Añade prueba si aplica.
```

---

# SPRINT 3 — ESTRUCTURA ACADÉMICA

## Prompt 3.1 — Grados

```text
Implementa el módulo Grados.

Incluye:

Entity
Repository
Service
Controller
DTOs
Mapper
Migración Flyway
Validaciones
Pruebas

Endpoints:

GET /api/v1/grados
GET /api/v1/grados/{id}
POST /api/v1/grados
PUT /api/v1/grados/{id}

Solo ADMIN puede crear/modificar.

No implementes secciones todavía.
```

---

## Prompt 3.2 — Secciones

```text
Implementa el módulo Secciones.

Relación:
Sección pertenece a Grado.

Campos:
id
grado_id
nombre
periodo_academico
activo

Restricción única:
grado + nombre + periodo

Endpoints CRUD necesarios.

Solo ADMIN puede crear/modificar.

Añade pruebas de:
- creación
- duplicado
- grado inexistente
```

---

## Prompt 3.3 — Cursos

```text
Implementa el módulo Cursos.

Campos:
id
nombre
descripcion
activo

Endpoints CRUD.

Solo ADMIN puede crear/modificar.

Añade DTOs, mapper, validaciones y pruebas.
```

---

# SPRINT 4 — ESTUDIANTES Y QR

## Prompt 4.1 — Entidad Estudiante

```text
Implementa la entidad Estudiante.

Campos:

id
codigo
qrToken
nombres
apellidos
seccion
activo
createdAt
updatedAt

Restricciones:

codigo UNIQUE
qr_token UNIQUE
seccion obligatoria

Crea migración, repository y tests.

Todavía no implementes generación del QR visual.
```

---

## Prompt 4.2 — Servicio de estudiantes

```text
Implementa EstudianteService.

Funciones:

crear
actualizar
buscarPorId
listar
desactivar
buscarPorCodigo
buscarPorQrToken

Al crear:
- generar qrToken aleatorio seguro
- nunca usar nombres o datos personales en el token

Añade pruebas.
```

---

## Prompt 4.3 — API estudiantes

```text
Implementa endpoints:

GET /api/v1/estudiantes
GET /api/v1/estudiantes/{id}
POST /api/v1/estudiantes
PUT /api/v1/estudiantes/{id}
PATCH /api/v1/estudiantes/{id}/desactivar

Filtros:
codigo
nombre
seccion
activo

Solo ADMIN puede crear/modificar/desactivar.

DOCENTE podrá consultar únicamente cuando posteriormente se apliquen permisos de contexto.

Añade pruebas.
```

---

## Prompt 4.4 — Regenerar token QR

```text
Implementa:

POST /api/v1/estudiantes/{id}/regenerar-qr

Debe:
- validar ADMIN
- generar nuevo token
- invalidar el anterior
- registrar auditoría posteriormente cuando ese módulo exista

Por ahora crea un punto de extensión para auditoría.

Añade pruebas.
```

---

## Prompt 4.5 — Generar imagen QR

```text
Implementa generación de QR para un estudiante.

Puedes utilizar ZXing en backend o una solución equivalente documentada.

Endpoint:

GET /api/v1/estudiantes/{id}/qr

Debe devolver una imagen QR o representación segura.

El QR debe codificar únicamente:

AULAIA:STUDENT:<qrToken>

No incluir nombres ni código escolar visible dentro del contenido QR.

Añade pruebas del contenido generado cuando sea posible.
```

---

# SPRINT 5 — DOCENTES Y HORARIOS

## Prompt 5.1 — Docentes

```text
Implementa entidad Docente asociada 1 a 1 con Usuario.

Campos:
id
usuario_id
nombres
apellidos
activo

Crea:
- migración
- repository
- service
- controller
- DTOs
- mapper
- pruebas

Al crear docente:
- crear o asociar usuario con rol DOCENTE
- password siempre hasheada

Solo ADMIN puede administrar docentes.
```

---

## Prompt 5.2 — Horario entity

```text
Implementa entidad Horario según diseño de base de datos.

Relaciones:

Curso
Sección
Docente

Campos:
diaSemana
horaInicio
horaFin
toleranciaMinutos
minutosAntesApertura
activo

Validaciones:

dia 1-7
horaFin > horaInicio
tolerancia >= 0

Crea migración y tests.
```

---

## Prompt 5.3 — Detectar conflictos de horario

```text
Implementa validación de conflictos antes de guardar un horario.

Detectar al menos:

- mismo docente en horarios solapados
- misma sección en horarios solapados

No bloquees por conflictos que no estén definidos.

Añade pruebas con:
- horario sin conflicto
- conflicto docente
- conflicto sección
- horarios consecutivos válidos
```

---

## Prompt 5.4 — API horarios

```text
Implementa endpoints CRUD para horarios.

Incluye filtros por:

docente
seccion
curso
dia

Solo ADMIN crea/modifica.

DOCENTE puede consultar sus propios horarios.

Añade autorización y pruebas.
```

---

# SPRINT 6 — SESIONES DE CLASE

## Prompt 6.1 — Entidad SesionClase

```text
Implementa SesionClase.

Campos:
id
horario
fecha
horaApertura
horaCierre
estado

Estados:

PROGRAMADA
ABIERTA
CERRADA
CANCELADA

Restricción:
UNIQUE(horario_id, fecha)

Crea migración y tests.
```

---

## Prompt 6.2 — Crear sesión a partir de horario

```text
Implementa lógica para obtener o crear la sesión real correspondiente a un horario y fecha.

No crear duplicados.

Método sugerido:

obtenerOCrearSesion(horarioId, fecha)

Añade pruebas.
```

---

## Prompt 6.3 — Abrir sesión

```text
Implementa:

POST /api/v1/sesiones/{id}/abrir

Validar:
- sesión existe
- estado permitido
- usuario tiene permiso
- docente corresponde a la clase o es ADMIN

Al abrir:
estado = ABIERTA
horaApertura = hora servidor

Añade pruebas.
```

---

## Prompt 6.4 — Listar sesiones

```text
Implementa:

GET /api/v1/sesiones
GET /api/v1/sesiones/activas
GET /api/v1/sesiones/{id}

Filtros:
fecha
docente
seccion
curso
estado

Aplicar permisos por rol.

Añade pruebas.
```

---

# SPRINT 7 — NÚCLEO DE ASISTENCIA

## Prompt 7.1 — Entidad Asistencia

```text
Implementa Asistencia.

Campos:

id
sesionClase
estudiante
fechaHora
estado
metodo
observacion
createdAt
updatedAt

Estados:
PRESENTE
TARDANZA
AUSENTE
JUSTIFICADO

Métodos:
QR
CODIGO
MANUAL_DOCENTE
SISTEMA

Restricción crítica:

UNIQUE(sesion_clase_id, estudiante_id)

Crea migración, repository y tests.
```

---

## Prompt 7.2 — Resolver estudiante por QR o código

```text
Implementa un componente de resolución de estudiante para registro de asistencia.

Si metodo = QR:
resolver por qrToken contenido en AULAIA:STUDENT:<TOKEN>

Si metodo = CODIGO:
resolver por codigo escolar.

Ambos caminos deben converger en el mismo flujo de registro.

Añade pruebas:
- QR válido
- formato QR inválido
- código válido
- código inexistente
```

---

## Prompt 7.3 — Registrar asistencia

```text
Implementa AsistenciaService.registrar(...).

Flujo obligatorio:

1. validar sesión ABIERTA
2. resolver estudiante
3. validar estudiante activo
4. validar que pertenece a la sección
5. verificar duplicado
6. obtener hora del servidor
7. calcular PRESENTE o TARDANZA
8. guardar
9. devolver respuesta

Nunca confiar en la hora enviada por frontend.

La operación debe ser transaccional.

Añade pruebas completas.
```

---

## Prompt 7.4 — Endpoint registrar

```text
Implementa:

POST /api/v1/asistencias/registrar

Request:
codigo
metodo
sesionId

Response:
success
nombre
hora
estado
mensaje

No devolver apellidos ni información innecesaria al Modo Aula.

Errores específicos:

STUDENT_NOT_FOUND
SESSION_NOT_ACTIVE
STUDENT_NOT_IN_SECTION
ATTENDANCE_ALREADY_REGISTERED
INVALID_QR

Añade pruebas de controller.
```

---

## Prompt 7.5 — Concurrencia y duplicados

```text
Revisa el flujo de asistencia ante dos solicitudes simultáneas del mismo estudiante.

Asegura que la restricción UNIQUE sea la última barrera.

Captura correctamente la violación de constraint y conviértela en:

ATTENDANCE_ALREADY_REGISTERED

Añade una prueba de concurrencia o integración razonable.
```

---

## Prompt 7.6 — Consulta de asistencias

```text
Implementa:

GET /api/v1/asistencias
GET /api/v1/asistencias/sesion/{id}
GET /api/v1/asistencias/estudiante/{id}

Filtros:
fecha
estado
seccion
curso
estudiante

Respeta permisos.

Añade paginación.

Añade pruebas.
```

---

## Prompt 7.7 — Cerrar sesión y generar ausentes

```text
Implementa:

POST /api/v1/sesiones/{id}/cerrar

Flujo:

1. validar sesión ABIERTA
2. obtener estudiantes activos de la sección
3. identificar quiénes no tienen asistencia
4. crear AUSENTE con metodo SISTEMA
5. cambiar estado a CERRADA
6. registrar horaCierre

Todo debe ocurrir en una transacción.

No generar ausentes en sesión CANCELADA.

Añade pruebas.
```

---

## Prompt 7.8 — Cancelar sesión

```text
Implementa cancelación de sesión.

Endpoint:

POST /api/v1/sesiones/{id}/cancelar

Debe requerir motivo.

Al cancelar:
estado = CANCELADA

No generar ausentes.

Deja integración preparada para auditoría.

Añade pruebas.
```

---

# SPRINT 8 — FRONTEND ANGULAR BASE

## Prompt 8.1 — Crear Angular

```text
Inicializa frontend Angular moderno con TypeScript.

Configura:

routing
HttpClient
environments
estructura core/shared/features/layouts

No implementes pantallas completas todavía.

Ejecuta:
build
tests
```

---

## Prompt 8.2 — Design tokens

```text
Implementa los design tokens definidos en UI_UX_AulaIA.md.

Incluye:

colores
tipografía Inter
spacing
radius
sombras
estados

No diseñes todavía todos los componentes.

Crea variables CSS/SCSS reutilizables.
```

---

## Prompt 8.3 — Componentes base

```text
Crea componentes reutilizables:

Button
Input
Select
Textarea
Card
Badge
Modal/Dialog
Toast
Skeleton
PageHeader
EmptyState

Respeta accesibilidad.

No implementes todavía componentes específicos de asistencia.

Añade tests básicos.
```

---

## Prompt 8.4 — Layout

```text
Implementa layout administrativo:

Sidebar
Header
Área de contenido

Debe ser responsive.

Sidebar:
- expandido desktop
- colapsable laptop
- drawer tablet/móvil

No conectes aún todas las opciones.

Añade pruebas visuales/lógicas razonables.
```

---

# SPRINT 9 — LOGIN Y SEGURIDAD FRONTEND

## Prompt 9.1 — Pantalla login

```text
Implementa pantalla Login según UI/UX.

Campos:
usuario
contraseña

Acciones:
iniciar sesión

Estados:
loading
error

Conecta con:

POST /api/v1/auth/login

Guarda token de forma segura según arquitectura definida.

No implementes refresh complejo si backend aún no lo soporta.
```

---

## Prompt 9.2 — AuthService y interceptor

```text
Implementa:

AuthService
AuthInterceptor
AuthGuard
RoleGuard

Enviar Bearer token.

Redirecciones:
ADMIN → admin dashboard
DOCENTE → docente dashboard

Añade tests.
```

---

# SPRINT 10 — MODO AULA Y QR

## Prompt 10.1 — Pantalla Modo Aula base

```text
Implementa la pantalla fullscreen Modo Aula.

Mostrar:

AulaIA
curso
sección
instrucción
área cámara
botón ingresar código manual

No conectes cámara todavía.

Debe adaptarse a 1366x768 y 1920x1080.
```

---

## Prompt 10.2 — Integrar cámara

```text
Integra acceso a cámara del navegador en Modo Aula.

Manejar estados:

permission pending
permission denied
camera unavailable
camera ready

No registres asistencia todavía.

Añade botón de reintento y alternativa manual.
```

---

## Prompt 10.3 — Integrar ZXing

```text
Integra ZXing o librería compatible para leer QR con la cámara.

Al detectar:
- obtener contenido
- validar formato básico
- bloquear lecturas repetidas temporalmente

No inventes estado de asistencia.

Solo prepara emisión del código leído.

Añade tests donde sea posible.
```

---

## Prompt 10.4 — Conectar QR con backend

```text
Conecta el QR detectado con:

POST /api/v1/asistencias/registrar

Enviar:

codigo
metodo = QR
sesionId

Manejar:
success
duplicado
código inválido
otra sección
sesión cerrada
error de red

Nunca mostrar éxito antes de respuesta backend.
```

---

## Prompt 10.5 — Registro manual

```text
Implementa modal de registro manual.

Input:
código estudiante

Enviar al mismo endpoint:

metodo = CODIGO

Debe compartir el mismo servicio frontend que QR.

No dupliques lógica.

Permitir Enter.

Añade validaciones y tests.
```

---

# SPRINT 11 — VOZ Y EXPERIENCIA DEL ESTUDIANTE

## Prompt 11.1 — Servicio Text-to-Speech

```text
Implementa VoiceService con Web Speech API.

Funciones:

speak(text)
cancel()
isSupported()

Configurar:
lang = es-PE

Permitir:
enabled
rate
volume

No uses una API de pago.

Añade fallback silencioso si el navegador no soporta voz.
```

---

## Prompt 11.2 — Confirmación PRESENTE

```text
Implementa pantalla/componente de confirmación de asistencia PRESENTE.

Mostrar:

check
primer nombre
hora
PRESENTE
mensaje positivo

Reproducir voz:

"¡Hola {nombre}! Tu asistencia fue registrada correctamente. ¡Que tengas una excelente clase de computación!"

Después de 3-5 segundos:
- limpiar
- volver al escáner
- reactivar lectura

Evitar que voz y cámara se superpongan incorrectamente.
```

---

## Prompt 11.3 — Confirmación TARDANZA

```text
Implementa estado visual y voz para TARDANZA.

Mostrar:
icono reloj
nombre
hora
TARDANZA

Voz breve y respetuosa.

No usar lenguaje que avergüence al estudiante.
```

---

## Prompt 11.4 — Duplicado y errores amigables

```text
Implementa estados visuales y por voz para:

duplicado
código inválido
sesión cerrada
cámara no disponible
error de red

No leer datos sensibles por voz.

Después del mensaje:
volver al flujo principal.
```

---

# SPRINT 12 — DASHBOARD DOCENTE

## Prompt 12.1 — API resumen docente

```text
Implementa endpoint backend para dashboard docente.

Debe devolver solo datos necesarios:

clase actual
clases del día
presentes
tardanzas
ausentes
total estudiantes
porcentaje

No incluir IA todavía.

Añade pruebas.
```

---

## Prompt 12.2 — Dashboard frontend

```text
Implementa dashboard docente según UI_UX.

Cards:

Presentes
Tardanzas
Ausentes
Asistencia %

Card:
Clase actual

Lista:
Clases del día

Botón:
Abrir Modo Aula

Conectar con backend.
```

---

## Prompt 12.3 — Actualización automática

```text
Implementa actualización periódica del dashboard docente.

Usa inicialmente polling cada 5-10 segundos.

Evita solicitudes duplicadas.

Detén polling al salir de pantalla.

Documenta que WebSocket/SSE queda como mejora futura.

Añade tests.
```

---

# SPRINT 13 — ADMINISTRACIÓN FRONTEND

## Prompt 13.1 — Estudiantes

```text
Implementa módulo frontend de estudiantes.

Pantallas:

listado
crear
editar
detalle

Funciones:

buscar
filtrar por grado/sección/estado
desactivar

Conectar con API.

Aplicar roles.
```

---

## Prompt 13.2 — QR estudiante

```text
Implementa pestaña/card QR en detalle de estudiante.

Funciones:

ver QR
descargar
imprimir
regenerar

Regenerar debe requerir confirmación.

Después de regenerar actualizar la imagen.
```

---

## Prompt 13.3 — Grados, secciones y cursos

```text
Implementa interfaces administrativas para:

grados
secciones
cursos

Mantén diseño consistente.

Añade formularios simples, validaciones y estados.
```

---

## Prompt 13.4 — Docentes

```text
Implementa administración de docentes.

Pantallas:

listado
crear
editar
desactivar

No mostrar password existente.

Permitir establecer credencial inicial de forma segura.
```

---

## Prompt 13.5 — Horarios

```text
Implementa gestión visual de horarios.

Incluye:

vista semanal
vista lista
crear
editar

Mostrar conflictos devueltos por backend.

No intentes resolver conflictos solo en frontend.
```

---

# SPRINT 14 — JUSTIFICACIONES Y AUDITORÍA

## Prompt 14.1 — Auditoría backend

```text
Implementa módulo Auditoría.

Entidad:
Auditoria

Campos según BASE_DE_DATOS_AulaIA.md.

Crear AuditService reutilizable.

Registrar inicialmente:

regenerar QR
modificar asistencia
cancelar sesión
cerrar sesión
modificar horario
desactivar estudiante

Añade pruebas.
```

---

## Prompt 14.2 — Corrección manual de asistencia

```text
Implementa actualización autorizada de asistencia.

Endpoint apropiado.

Requerir:

nuevo estado
motivo obligatorio

Guardar:
valor anterior
valor nuevo
usuario
fecha
motivo

No permitir al estudiante modificar.

Añade pruebas de permisos.
```

---

## Prompt 14.3 — Justificaciones backend

```text
Implementa entidad y flujo de justificaciones.

Estados:

PENDIENTE
APROBADA
RECHAZADA

Funciones:

crear
consultar
aprobar
rechazar

Al aprobar:
asistencia → JUSTIFICADO

Registrar auditoría.

Añade pruebas.
```

---

## Prompt 14.4 — Justificaciones frontend

```text
Implementa pantalla de justificaciones.

Mostrar:

estudiante
fecha
estado asistencia
motivo
estado justificación

Acciones autorizadas:

aprobar
rechazar

Solicitar confirmación.

Mostrar historial.
```

---

## Prompt 14.5 — Auditoría frontend

```text
Implementa pantalla de auditoría solo para ADMIN.

Filtros:

usuario
entidad
acción
fecha

Tabla:

fecha
usuario
acción
entidad
detalle

No permitir edición.
```

---

# SPRINT 15 — REPORTES

## Prompt 15.1 — Consultas de reporte backend

```text
Implementa servicio de reportes.

Filtros:

fechaInicio
fechaFin
curso
seccion
estudiante
estado

Debe generar un DTO normalizado para:

pantalla
Excel
PDF

Añade pruebas de filtros.
```

---

## Prompt 15.2 — Reporte Excel

```text
Implementa exportación XLSX con Apache POI.

Columnas mínimas:

fecha
estudiante
código
curso
sección
hora
estado
método

Aplicar filtros solicitados.

Formato profesional.

Añade prueba de generación de archivo.
```

---

## Prompt 15.3 — Reporte PDF

```text
Implementa exportación PDF con una librería compatible con licencia adecuada.

Debe incluir:

AulaIA
periodo
curso/sección
resumen
tabla de asistencia

No implementar contenido visual excesivo.

Añade prueba de generación.
```

---

## Prompt 15.4 — Frontend reportes

```text
Implementa pantalla Reportes.

Filtros:

fecha inicio
fecha fin
curso
sección
estudiante
estado

Mostrar resumen y tabla.

Botones:

Excel
PDF

Manejar loading/error.
```

---

# SPRINT 16 — FASTAPI E IA

## Prompt 16.1 — Inicializar FastAPI

```text
Inicializa /data-science con Python y FastAPI.

Estructura:

app/
  main.py
  api/
  services/
  models/
  schemas/
  tests/

Dependencias:

fastapi
uvicorn
pandas
numpy
scikit-learn
pydantic
pytest

Crear:

GET /health

Ejecutar tests.
```

---

## Prompt 16.2 — Contrato de análisis

```text
Implementa endpoint:

POST /api/v1/analisis/asistencia

Request con datos agregados de asistencia.

Response:

porcentajeAsistencia
tendencia
nivelAtencion
resumen

No consultar PostgreSQL directamente.

Añade validaciones y tests.
```

---

## Prompt 16.3 — Análisis estadístico inicial

```text
Implementa análisis estadístico determinista.

Calcular:

porcentaje asistencia
frecuencia tardanzas
frecuencia ausencias
tendencia simple

No uses todavía machine learning si no existe dataset suficiente.

Añade tests con casos conocidos.
```

---

## Prompt 16.4 — Detección de patrones

```text
Implementa detección de patrones simples usando Pandas.

Casos:

3 ausencias en últimas 4 sesiones
tardanzas recurrentes
descenso de asistencia semanal

Devolver insights informativos.

No diagnosticar ni sancionar estudiantes.

Añade tests.
```

---

## Prompt 16.5 — Cliente Spring Boot → FastAPI

```text
Implementa cliente HTTP en Spring Boot para FastAPI.

Configurar:

IA_SERVICE_URL

Funciones:

health
analizarAsistencia

Agregar timeouts.

Si IA falla:
- registrar log
- devolver estado de IA no disponible
- no bloquear asistencia

Añade pruebas con mock del servicio.
```

---

## Prompt 16.6 — API IA del backend

```text
Implementa endpoints del backend para consultas IA autorizadas.

Spring Boot debe:

1. validar usuario
2. validar permisos
3. consultar PostgreSQL
4. preparar datos mínimos
5. llamar FastAPI
6. devolver resultado

Nunca pasar acceso directo de FastAPI a la base de datos.
```

---

## Prompt 16.7 — UI AulaIA IA

```text
Implementa pantalla AulaIA IA.

Elementos:

input de consulta
chips rápidos
cards de insights

Consultas iniciales:

¿Quiénes faltaron hoy?
Resumen de esta semana
Estudiantes con tardanzas
Tendencia del mes

No implementar chat libre si backend aún no lo soporta.

Mostrar claramente cuando IA no está disponible.
```

---

# SPRINT 17 — CONSULTAS INTELIGENTES

## Prompt 17.1 — Consultas predefinidas seguras

```text
Implementa consultas inteligentes basadas en intents predefinidos.

Ejemplos:

FALTARON_HOY
RESUMEN_SEMANA
TARDANZAS_RECURRENTES
TENDENCIA_MES

No permitas SQL generado por IA.

Mapea intent → consulta backend segura.

Añade pruebas.
```

---

## Prompt 17.2 — Respuestas naturales

```text
Construye respuestas legibles para profesor a partir de resultados estructurados.

Ejemplo:

"Durante esta semana, 6.º A tuvo una asistencia promedio de 93%. Se registraron 2 tardanzas y 1 ausencia."

La respuesta debe estar respaldada por datos.

No inventar causas.

Añade tests.
```

---

# SPRINT 18 — TESTING INTEGRAL

## Prompt 18.1 — Suite backend

```text
Audita la cobertura de pruebas backend.

Asegura pruebas para:

auth
estudiantes
horarios
sesiones
asistencias
duplicados
tardanzas
ausentes
justificaciones
reportes
auditoría
IA fallback

No cambies funcionalidad salvo errores detectados.
```

---

## Prompt 18.2 — Testcontainers

```text
Integra Testcontainers con PostgreSQL para pruebas de integración críticas.

Migraciones Flyway deben ejecutarse en tests.

Pruebas mínimas:

registro asistencia
constraint duplicado
cierre de sesión
ausentes
justificación

Documenta ejecución.
```

---

## Prompt 18.3 — Tests frontend

```text
Añade o completa pruebas frontend para:

login
guards
Modo Aula
QR success
QR error
registro manual
VoiceService
dashboard
formularios admin

No busques cobertura artificial.
Prioriza flujos críticos.
```

---

## Prompt 18.4 — E2E

```text
Configura Playwright.

Crear E2E principales:

1. login docente
2. abrir clase
3. registrar código manual
4. ver confirmación
5. dashboard actualizado
6. cerrar sesión
7. comprobar ausente

Para QR real, mockear cámara si es necesario.

Documentar ejecución.
```

---

## Prompt 18.5 — Tests FastAPI

```text
Completa pytest para:

health
análisis
validaciones
patrones
datos vacíos
errores

Asegura resultados deterministas.
```

---

# SPRINT 19 — DOCKER Y ENTORNO LOCAL

## Prompt 19.1 — Docker backend

```text
Crea Dockerfile del backend.

Usa build multi-stage.

Java 21.

No incluir secretos.

Validar imagen y arranque.
```

---

## Prompt 19.2 — Docker FastAPI

```text
Crea Dockerfile para data-science.

Instalar dependencias de forma reproducible.

Exponer puerto adecuado.

Healthcheck si corresponde.

Validar arranque.
```

---

## Prompt 19.3 — Docker frontend

```text
Crea Dockerfile para Angular.

Build de producción.

Servir estáticos con servidor apropiado.

Configurar rutas SPA.

Validar.
```

---

## Prompt 19.4 — Docker Compose

```text
Completa docker-compose.yml para desarrollo.

Servicios:

postgres
backend
fastapi
frontend

Configurar:
red
variables
volúmenes

No hardcodear secretos reales.

Debe ser posible levantar el sistema con un comando documentado.
```

---

# SPRINT 20 — CI/CD

## Prompt 20.1 — Workflow backend

```text
Crea GitHub Actions para backend.

En pull request/push:

- setup Java
- build
- tests

No deploy todavía.

Añade cache de dependencias.
```

---

## Prompt 20.2 — Workflow frontend

```text
Crea workflow Angular:

install
lint si existe
test
build

No deploy todavía.
```

---

## Prompt 20.3 — Workflow FastAPI

```text
Crea workflow Python:

setup
install
pytest

No deploy todavía.
```

---

## Prompt 20.4 — Validación global CI

```text
Revisa workflows.

Asegura que un fallo en tests bloquee pipeline.

Documenta badges opcionales en README.

No habilites despliegue automático todavía.
```

---

# SPRINT 21 — PREPARACIÓN PARA PRODUCCIÓN

## Prompt 21.1 — Auditoría de secretos

```text
Audita el repositorio para detectar:

passwords
tokens
API keys
connection strings
archivos .env

Corrige exposición si existe.

Actualiza .gitignore.

No cambies credenciales externas.
Informa si alguna debería rotarse.
```

---

## Prompt 21.2 — CORS y seguridad HTTP

```text
Configura CORS para producción mediante variables.

No usar * indiscriminadamente.

Revisa headers de seguridad.

Valida que frontend autorizado funcione.
```

---

## Prompt 21.3 — Logs

```text
Revisa logging.

Debe registrar:

errores
login relevante
asistencia
integración IA
acciones administrativas

Nunca registrar:

password
JWT completo
datos sensibles innecesarios

Añade IDs de correlación si es razonable.
```

---

## Prompt 21.4 — Configuración producción

```text
Crea configuración de producción basada en variables.

Backend:
DB
JWT
IA URL
CORS

Frontend:
API URL

FastAPI:
environment

No incluir secretos.

Documentar variables requeridas.
```

---

## Prompt 21.5 — Migraciones finales

```text
Ejecuta todas las migraciones desde una base PostgreSQL vacía.

Valida:

orden
constraints
foreign keys
índices

Ejecuta backend con:

ddl-auto=validate

Corrige únicamente mediante nuevas migraciones.
```

---

# SPRINT 22 — DESPLIEGUE

## Prompt 22.1 — Preparar Neon PostgreSQL

```text
Prepara el proyecto para desplegar PostgreSQL en Neon.

No crees recursos sin autorización.

Documenta:

variables
SSL
cadena JDBC esperada
migraciones

Asegura compatibilidad con PostgreSQL administrado.
```

---

## Prompt 22.2 — Preparar Render backend

```text
Prepara backend para Render.

Documenta:

build command
start command
variables
health endpoint

No hagas deploy.

Verifica que el artefacto compile localmente como producción.
```

---

## Prompt 22.3 — Preparar Render FastAPI

```text
Prepara FastAPI para Render.

Documenta:

build
start
PORT
health

No despliegues.
```

---

## Prompt 22.4 — Preparar Vercel frontend

```text
Prepara Angular para Vercel.

Configura:

build
output
SPA routing
API URL por environment

No despliegues.

Valida build production.
```

---

# SPRINT 23 — PILOTO Y VALIDACIÓN FINAL

## Prompt 23.1 — Datos demo

```text
Crea una estrategia de datos ficticios para demo.

Incluir:

1 admin
1 docente
1 curso Computación
1 grado
1 sección 6.º A
10-20 estudiantes ficticios
1 horario
1 sesión

No usar datos reales de menores.

Preferir seed de desarrollo controlado.
```

---

## Prompt 23.2 — Guion técnico de demo

```text
Documenta el procedimiento exacto para demostrar AulaIA:

1. login
2. abrir clase
3. activar Modo Aula
4. escanear QR
5. voz
6. duplicado
7. tardanza
8. dashboard
9. cierre
10. ausentes
11. reporte
12. IA

No modificar código.
```

---

## Prompt 23.3 — Auditoría funcional final

```text
Realiza auditoría funcional completa de AulaIA contra:

PRD
TRD
Arquitectura
Base de Datos
UI/UX
Flujos

Crea una matriz:

Requisito
Estado
Prueba
Observación

No corrijas todavía.
Solo reporta diferencias reales.
```

---

## Prompt 23.4 — Corrección de bloqueantes

```text
Usa el informe de auditoría final.

Corrige únicamente requisitos:

CRÍTICOS
ALTOS

Trabaja uno por uno.

Después de cada corrección:
- pruebas
- validación
- documentación

No mezclar correcciones no relacionadas.
```

---

## Prompt 23.5 — Regresión completa

```text
Ejecuta regresión final:

backend tests
frontend tests
FastAPI tests
E2E
builds de producción
migraciones

No cambies funcionalidad salvo fallo demostrado.

Entrega resumen final.
```

---

# SPRINT 24 — MEJORAS POST-MVP

Estas tareas NO deben mezclarse con el MVP.

---

## Prompt 24.1 — WebSocket/SSE

```text
Evalúa reemplazar polling del dashboard por SSE o WebSocket.

Primero presenta propuesta técnica.

No implementar hasta aprobación.
```

---

## Prompt 24.2 — Modo offline

```text
Analiza una arquitectura de Modo Aula offline-first.

Objetivo:
registrar temporalmente durante pérdida de Internet y sincronizar después.

Presenta riesgos de duplicados, seguridad y sincronización.

No implementar hasta aprobación.
```

---

## Prompt 24.3 — Matrículas históricas

```text
Diseña migración desde estudiantes.seccion_id hacia tabla matriculas.

Objetivo:
soportar múltiples años académicos sin perder histórico.

Primero presentar plan y migración.
No ejecutar hasta aprobación.
```

---

## Prompt 24.4 — Multiinstitución

```text
Diseña evolución SaaS multiinstitución:

instituciones
sedes
usuarios por institución
aislamiento de datos

No implementar.
Solo arquitectura.
```

---

# 5. Plantilla de prompt para cualquier nueva tarea

Cuando aparezca una nueva funcionalidad, usar:

```text
Analiza primero el proyecto AulaIA.

TAREA:
[describir una sola tarea]

OBJETIVO:
[resultado esperado]

ALCANCE:
[qué sí]

FUERA DE ALCANCE:
[qué no]

REVISA:
[archivos/módulos]

REGLAS:
- respetar arquitectura
- no duplicar lógica
- seguridad backend
- no cambiar otras funcionalidades

PRUEBAS:
[listar pruebas mínimas]

CRITERIO DE ACEPTACIÓN:
[resultado verificable]

AL FINAL:
- archivos modificados
- pruebas
- resultados
- pendientes

No commit.
No push.
No deploy.
No avances a otra tarea.
```

---

# 6. Regla de tamaño de prompt

Cada prompt debe intentar modificar una sola responsabilidad.

Malo:

```text
Crea estudiantes, horarios, asistencias, QR, dashboard e IA.
```

Correcto:

```text
Implementa únicamente la entidad Estudiante,
su migración y repository.
```

Después:

```text
Implementa EstudianteService.
```

Después:

```text
Implementa endpoints de estudiantes.
```

---

# 7. Regla de validación entre prompts

Antes de continuar:

```text
¿Compila?
¿Pasan tests?
¿Las migraciones funcionan?
¿Respeta documentos?
¿No rompió módulos anteriores?
```

Si la respuesta es NO:

```text
detener
corregir
volver a probar
```

---

# 8. Prompt de revisión después de cada sprint

```text
Haz una revisión del Sprint X de AulaIA.

No agregues funcionalidades nuevas.

Revisa:

1. cumplimiento de prompts del sprint,
2. arquitectura,
3. seguridad,
4. base de datos,
5. pruebas,
6. duplicación,
7. errores,
8. documentación.

Ejecuta todos los tests relacionados.

Entrega:

- COMPLETO
- PARCIAL
- BLOQUEADO

Lista cualquier deuda técnica encontrada.

No hagas commit, push ni deploy.
```

---

# 9. Prompt para corregir un sprint incompleto

```text
Toma el informe de revisión del Sprint X.

Corrige únicamente los puntos marcados como:

BLOQUEANTE
INCORRECTO
INCOMPLETO

No agregues mejoras opcionales.

Después:

- ejecuta pruebas,
- vuelve a validar,
- informa resultado.

No avances al siguiente sprint.
```

---

# 10. Prompt de auditoría de seguridad antes de producción

```text
Audita AulaIA antes de producción.

Revisa únicamente seguridad y privacidad:

- autenticación
- autorización
- JWT
- CORS
- passwords
- secretos
- logs
- endpoints públicos
- acceso por rol
- QR
- privacidad de menores
- datos enviados a IA
- SQL injection
- XSS
- CSRF según arquitectura
- validación de inputs

Clasifica hallazgos:

CRÍTICO
ALTO
MEDIO
BAJO

No corrijas todavía.

Entrega recomendaciones concretas.
```

---

# 11. Prompt final de readiness

```text
Evalúa si AulaIA está listo para un piloto escolar.

Revisa:

- funciones MVP
- seguridad
- privacidad
- estabilidad
- migraciones
- backups
- reportes
- QR
- cámara
- voz
- IA
- pruebas
- despliegue
- documentación

Devuelve:

READY
READY WITH CONDITIONS
NOT READY

Justifica cada bloqueo.

No cambies código.
```

---

# 12. Orden obligatorio de trabajo

No saltar directamente a IA.

El orden recomendado es:

```text
Datos
↓
Backend
↓
Seguridad
↓
Académico
↓
Estudiantes
↓
Horarios
↓
Sesiones
↓
Asistencia
↓
Frontend
↓
QR
↓
Voz
↓
Dashboard
↓
Reportes
↓
IA
↓
Testing
↓
Producción
```

La IA es un complemento.

El núcleo del sistema debe funcionar incluso si FastAPI está caído.

---

# 13. Qué NO debe hacer el agente

No debe:

- crear todo el proyecto de golpe,
- inventar requisitos,
- cambiar tecnologías sin aprobación,
- modificar migraciones aplicadas,
- eliminar datos sin estrategia,
- usar reconocimiento facial,
- almacenar biometría,
- confiar en el frontend para la asistencia,
- permitir duplicados,
- crear SQL generado libremente por LLM,
- conectar FastAPI directamente a producción sin control,
- hacer commit,
- hacer push,
- hacer deploy sin autorización.

---

# 14. Flujo diario recomendado con Codex u otro agente

Ejemplo:

```text
PROMPT 1
↓
IA implementa
↓
Revisamos resultado

PROMPT 2
↓
IA implementa
↓
Revisamos resultado

PROMPT 3
↓
IA implementa
↓
Revisión Sprint
```

No enviar 15 prompts juntos.

---

# 15. Definition of Done global

AulaIA MVP estará terminado cuando:

## Backend

- autenticación funciona,
- roles funcionan,
- estudiantes funcionan,
- horarios funcionan,
- sesiones funcionan,
- asistencia funciona,
- duplicados bloqueados,
- tardanzas calculadas,
- ausentes generados,
- auditoría funciona,
- reportes funcionan.

## Frontend

- login,
- dashboard,
- Modo Aula,
- QR,
- código manual,
- voz,
- administración,
- reportes,
- IA.

## IA

- FastAPI disponible,
- análisis estadístico,
- patrones básicos,
- fallback seguro.

## Calidad

- migraciones,
- tests,
- E2E,
- documentación,
- builds de producción,
- seguridad validada.

## Producción

- variables,
- PostgreSQL,
- backend,
- frontend,
- FastAPI,
- HTTPS,
- backups.

---

# 16. Resultado final esperado

Al seguir este documento prompt por prompt, el agente de IA debería construir AulaIA de manera incremental:

```text
Repositorio vacío
      ↓
Infraestructura
      ↓
Backend base
      ↓
Base de datos
      ↓
Seguridad
      ↓
Modelo académico
      ↓
Asistencia
      ↓
Angular
      ↓
QR
      ↓
Voz
      ↓
Dashboard
      ↓
Reportes
      ↓
IA
      ↓
Testing
      ↓
Docker
      ↓
CI/CD
      ↓
Producción
      ↓
Piloto escolar
```

La prioridad es que cada incremento sea pequeño, comprensible, probado y reversible.

Este documento debe utilizarse como guía operativa para el desarrollo completo de AulaIA con ayuda de agentes de inteligencia artificial.
