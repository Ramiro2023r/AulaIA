**AulaIA**

**Sistema Inteligente de Asistencia Escolar**

**TECHNICAL REQUIREMENTS DOCUMENT (TRD)**

| **Proyecto**     | AulaIA                                              |
|------------------|-----------------------------------------------------|
| **Documento**    | Technical Requirements Document - TRD               |
| **Versión**      | 1.0                                                 |
| **Fecha**        | 16 de agosto de 2026                                |
| **Arquitectura** | Angular + Spring Boot + PostgreSQL + FastAPI/Python |
| **Estado**       | Base técnica para desarrollo asistido por IA        |

*Este documento define cómo debe construirse técnicamente AulaIA y será
la referencia para agentes de IA, desarrolladores y pruebas del
proyecto.*

# 1. Propósito del documento

Este TRD traduce el PRD de AulaIA a una especificación técnica
implementable. Define arquitectura, componentes, responsabilidades,
contratos API, modelo de datos, seguridad, integración de QR y voz,
módulo de IA, pruebas, observabilidad, despliegue y criterios técnicos
de aceptación. El objetivo es reducir ambigüedad durante el desarrollo,
especialmente cuando se utilicen agentes de IA para generar o modificar
código.

# 2. Principios técnicos

- Arquitectura modular: cada capa tendrá responsabilidades claras y bajo
  acoplamiento.

- Backend como fuente de verdad: reglas de asistencia, horarios,
  permisos y estados se validan en Spring Boot.

- Frontend sin lógica crítica de seguridad: Angular guía la experiencia,
  pero no decide reglas de negocio definitivas.

- Privacidad por diseño: no se utilizará reconocimiento facial ni
  biometría de estudiantes.

- Identificación mediante código/QR y validación server-side.

- IA como apoyo, no como autoridad: análisis y alertas deben ser
  revisables por docentes.

- Trazabilidad: toda corrección de asistencia debe quedar auditada.

- Desarrollo incremental: primero núcleo transaccional, después QR/voz,
  reportes e IA.

# 3. Stack tecnológico oficial

| **Capa** | **Tecnología** | **Uso** | **Observación** |
|----|----|----|----|
| Frontend | Angular 17+ / TypeScript | SPA, Modo Aula, dashboard, administración | Componentes standalone y servicios HTTP. |
| Backend | Java 21 + Spring Boot 3.x | API REST y reglas de negocio | Spring Security, Validation, JPA/Hibernate. |
| Base de datos | PostgreSQL | Persistencia transaccional | Migraciones versionadas con Flyway. |
| IA / análisis | Python 3.12 + FastAPI | Análisis, patrones y modelos | Servicio desacoplado; no accede libremente a datos sensibles. |
| Datos IA | Pandas + scikit-learn | Procesamiento y modelos iniciales | Primero reglas/estadística; ML cuando existan datos suficientes. |
| QR | ZXing / biblioteca compatible Angular | Lectura por cámara y generación QR | QR contiene identificador opaco, no PII visible. |
| Voz | Web Speech API - SpeechSynthesis | Confirmaciones habladas | Sin grabar audio del estudiante. |
| Documentación API | OpenAPI / Swagger | Contrato y pruebas de endpoints | Disponible en entorno dev/test. |
| Testing | JUnit 5, Mockito, Testcontainers, Cypress/Playwright | Pruebas unitarias, integración y E2E | Pipeline obligatorio antes de merge. |
| Versionado | Git + GitHub | Código y colaboración | Ramas feature/fix y PR. |
| Contenedores | Docker / Docker Compose | Entornos reproducibles | Backend, DB y FastAPI. |

# 4. Arquitectura de alto nivel

La arquitectura objetivo separa la experiencia de usuario, las reglas de
negocio y el análisis de IA:

> Angular → Spring Boot REST API → PostgreSQL\
> ↓\
> FastAPI / Python IA

Angular nunca se conecta directamente a PostgreSQL ni al servicio de IA.
Spring Boot actúa como gateway de negocio y autorización. FastAPI recibe
únicamente datos necesarios para análisis o consultas previamente
filtradas.

# 5. Estructura de repositorio recomendada

> aulaia/\
> ├── frontend/ \# Angular\
> ├── backend/ \# Spring Boot\
> ├── data-science/ \# FastAPI + análisis IA\
> ├── database/ \# scripts y documentación SQL\
> ├── docs/ \# PRD, TRD, UX, flujos, testing\
> ├── docker-compose.yml\
> ├── .github/workflows/\
> └── README.md

# 6. Frontend Angular

## 6.1 Módulos/pantallas

- Autenticación

- Dashboard docente

- Modo Aula

- Estudiantes

- Docentes

- Grados y secciones

- Cursos

- Horarios

- Asistencias

- Justificaciones/correcciones

- Reportes

- Análisis IA

- Administración/configuración

## 6.2 Estructura sugerida

> src/app/\
> ├── core/ \# auth, interceptors, guards, config\
> ├── shared/ \# componentes reutilizables\
> ├── features/\
> │ ├── auth/\
> │ ├── aula/\
> │ ├── estudiantes/\
> │ ├── horarios/\
> │ ├── asistencias/\
> │ ├── reportes/\
> │ └── ia/\
> └── app.routes.ts

## 6.3 Reglas de frontend

- Usar Reactive Forms para formularios administrativos.

- Agregar HttpInterceptor para token/autenticación y manejo común de
  errores.

- Proteger rutas mediante guards según rol.

- Modo Aula debe ser usable en pantalla completa y con controles
  grandes.

- Tras un registro, mostrar confirmación 2-4 segundos, reproducir voz y
  volver automáticamente al escáner.

- No persistir datos sensibles en localStorage salvo credenciales/tokens
  si la estrategia seleccionada lo requiere; preferir cookies seguras
  cuando sea viable.

- Cancelar o serializar SpeechSynthesis para evitar voces superpuestas.

# 7. Backend Spring Boot

## 7.1 Capas

- Controller: expone endpoints y valida formato de entrada.

- Service: contiene reglas de negocio y transacciones.

- Repository: acceso JPA a PostgreSQL.

- Entity: modelo persistente.

- DTO: contratos de entrada/salida; no exponer entidades directamente.

- Mapper: conversión Entity ↔ DTO.

- Security: autenticación, autorización y contexto de usuario.

- Exception Handler: errores consistentes mediante @ControllerAdvice.

## 7.2 Paquetes sugeridos

> com.aulaia\
> ├── auth\
> ├── usuario\
> ├── estudiante\
> ├── docente\
> ├── academico\
> ├── horario\
> ├── sesionclase\
> ├── asistencia\
> ├── reporte\
> ├── ia\
> ├── auditoria\
> ├── config\
> └── common

# 8. Modelo de dominio

| **Entidad** | **Responsabilidad** | **Relaciones** | **Campos clave** |
|----|----|----|----|
| Usuario | Acceso al sistema | Rol, Docente opcional | id, username/email, passwordHash, rol, activo |
| Estudiante | Identidad escolar | Sección, asistencias | id, codigo, nombres, apellidos, activo |
| Docente | Datos del profesor | Usuario, horarios | id, usuarioId, nombres, apellidos |
| Grado | Nivel académico | Secciones | id, nombre |
| Seccion | Grupo de alumnos | Grado, estudiantes, horarios | id, gradoId, nombre, periodo |
| Curso | Materia | Horarios | id, nombre, codigo |
| Horario | Programación recurrente | Curso, sección, docente | id, diaSemana, inicio, fin, toleranciaMin |
| SesionClase | Instancia de una clase | Horario, asistencias | id, horarioId, fecha, apertura, cierre, estado |
| Asistencia | Registro por estudiante/sesión | Estudiante, sesión | id, estudianteId, sesionId, fechaHora, estado, metodo |
| Justificacion | Motivo autorizado | Asistencia, usuario | id, asistenciaId, motivo, estado |
| Auditoria | Historial de cambios | Usuario y entidad | id, accion, entidad, entidadId, antes, despues, fechaHora |

# 9. Esquema relacional mínimo

- usuarios(id PK, username UNIQUE, password_hash, rol, activo,
  created_at, updated_at)

- estudiantes(id PK, codigo UNIQUE, nombres, apellidos, seccion_id FK,
  activo, created_at)

- docentes(id PK, usuario_id FK UNIQUE, nombres, apellidos, activo)

- grados(id PK, nombre)

- secciones(id PK, grado_id FK, nombre, periodo, activo)

- cursos(id PK, codigo UNIQUE, nombre, activo)

- horarios(id PK, curso_id FK, seccion_id FK, docente_id FK, dia_semana,
  hora_inicio, hora_fin, tolerancia_min, activo)

- sesiones_clase(id PK, horario_id FK, fecha, apertura_at, cierre_at,
  estado)

- asistencias(id PK, sesion_id FK, estudiante_id FK, registrado_at,
  estado, metodo, observacion, created_by)

- justificaciones(id PK, asistencia_id FK, motivo, estado, creado_por,
  created_at)

- auditoria(id PK, usuario_id FK, entidad, entidad_id, accion,
  payload_anterior JSONB, payload_nuevo JSONB, created_at)

Restricción crítica: UNIQUE(sesion_id, estudiante_id) en asistencias
para impedir duplicados incluso ante concurrencia.

# 10. Estados y enumeraciones

| **Enum** | **Valores** | **Uso** |
|----|----|----|
| Rol | ADMIN, DOCENTE | Autorización. |
| EstadoAsistencia | PRESENTE, TARDANZA, AUSENTE, JUSTIFICADO | Estado académico. |
| MetodoRegistro | QR, CODIGO_MANUAL, DOCENTE | Origen del registro. |
| EstadoSesion | PROGRAMADA, ABIERTA, CERRADA, CANCELADA | Ciclo de sesión. |
| EstadoJustificacion | PENDIENTE, APROBADA, RECHAZADA | Flujo de justificación. |

# 11. Flujo técnico de registro de asistencia

1.  Angular obtiene el código desde el lector QR o input manual.

2.  Frontend envía POST /api/v1/asistencias/registrar con
    codigoEstudiante y sesionId (o contexto de aula).

3.  Spring Security valida la sesión del usuario/dispositivo autorizado.

4.  AsistenciaService busca la sesión activa y valida estado ABIERTA.

5.  Busca estudiante por código y verifica que esté activo.

6.  Valida que el estudiante pertenezca a la sección de la sesión.

7.  Comprueba UNIQUE lógica/preexistencia para evitar duplicado.

8.  Obtiene hora actual del servidor.

9.  Compara hora actual con inicio + tolerancia para clasificar
    PRESENTE/TARDANZA.

10. Persiste asistencia en una transacción.

11. Devuelve DTO con primer nombre, estado, hora y mensaje de voz.

12. Angular actualiza UI y reproduce SpeechSynthesis.

# 12. API REST propuesta

| **Método** | **Endpoint** | **Rol** | **Descripción** | **Respuesta principal** |
|----|----|----|----|----|
| POST | /api/v1/auth/login | Público | Autenticar usuario | token/sesión + perfil |
| GET | /api/v1/estudiantes | ADMIN/DOCENTE | Listar según permisos | paginado |
| POST | /api/v1/estudiantes | ADMIN | Crear estudiante | estudiante + código |
| GET | /api/v1/estudiantes/{id}/qr | ADMIN/DOCENTE | Obtener QR autorizado | imagen/valor QR |
| GET | /api/v1/horarios | ADMIN/DOCENTE | Consultar horarios | lista/paginado |
| POST | /api/v1/sesiones/{id}/abrir | DOCENTE | Abrir sesión | sesión ABIERTA |
| POST | /api/v1/sesiones/{id}/cerrar | DOCENTE | Cerrar y consolidar ausentes | resumen |
| POST | /api/v1/asistencias/registrar | DOCENTE/MODO_AULA | Registrar QR/código | resultado registro |
| GET | /api/v1/sesiones/{id}/asistencias | DOCENTE | Estado de clase | lista + métricas |
| PATCH | /api/v1/asistencias/{id} | ADMIN/DOCENTE | Corregir con motivo | registro actualizado |
| GET | /api/v1/reportes/asistencia | ADMIN/DOCENTE | Reporte filtrado | datos/archivo |
| POST | /api/v1/ia/resumen | DOCENTE | Generar resumen autorizado | texto + métricas |
| POST | /api/v1/ia/consulta | DOCENTE | Pregunta en lenguaje natural | respuesta fundamentada |

# 13. Contrato de registro

## 13.1 Request

> {\
> "sesionId": 125,\
> "codigoEstudiante": "EST-000145",\
> "metodo": "QR"\
> }

## 13.2 Response exitoso

> {\
> "success": true,\
> "estudianteId": 44,\
> "nombreMostrar": "Juan",\
> "estado": "PRESENTE",\
> "hora": "09:03",\
> "mensaje": "Asistencia registrada correctamente",\
> "mensajeVoz": "¡Hola, Juan! Tu asistencia fue registrada
> correctamente. ¡Que tengas una excelente clase de computación!"\
> }

## 13.3 Errores esperados

- 400 INVALID_CODE - formato inválido.

- 404 STUDENT_NOT_FOUND - código inexistente.

- 409 ATTENDANCE_ALREADY_EXISTS - duplicado.

- 409 SESSION_NOT_OPEN - sesión no disponible.

- 403 STUDENT_NOT_IN_SECTION - alumno no pertenece a la clase.

- 401/403 - autenticación o permisos insuficientes.

# 14. QR y cámara

- El QR se escaneará con la cámara integrada o externa de la laptop
  mediante getUserMedia y biblioteca compatible con ZXing.

- HTTPS será obligatorio en producción para el uso confiable de cámara
  en navegador.

- El valor QR no debe contener nombres, DNI, edad ni sección en texto
  plano.

- Formato sugerido: identificador aleatorio/UUID o token opaco asociado
  al estudiante.

- El sistema debe permitir seleccionar cámara si el dispositivo tiene
  más de una.

- Debe existir fallback por código manual.

- No almacenar capturas de cámara ni video.

# 15. Text-to-Speech

- Usar window.speechSynthesis y SpeechSynthesisUtterance.

- Idioma preferido es-PE; fallback es-ES o voz española disponible.

- Velocidad sugerida 0.9-1.0 y mensajes cortos.

- Antes de reproducir una nueva confirmación, gestionar cola para evitar
  superposición.

- La voz solo debe pronunciar primer nombre y resultado inmediato; nunca
  historial, notas sensibles o motivos de ausencia.

- La confirmación visual siempre debe existir aunque TTS no esté
  disponible.

# 16. Autenticación y autorización

- Spring Security será responsable de autenticación y autorización.

- Roles iniciales: ADMIN y DOCENTE. Los estudiantes no requieren cuenta
  para el Modo Aula.

- Contraseñas almacenadas con BCrypt/Argon2, nunca en texto plano.

- Producción solo por HTTPS.

- Si se usa JWT: access token de vida corta y estrategia segura de
  refresh; si se usan cookies: HttpOnly, Secure, SameSite apropiado.

- Endpoints deben validar ownership/permisos: un docente no debe
  consultar clases ajenas salvo permiso explícito.

- Rate limiting básico en login y endpoints sensibles.

# 17. Privacidad y seguridad de menores

- Minimización de PII: guardar solo datos necesarios para la función
  escolar.

- No usar biometría ni reconocimiento facial en esta versión.

- No enviar datos completos de estudiantes al servicio de IA si no son
  necesarios.

- Para exposición/desarrollo usar datos ficticios o anonimizados.

- Registros de auditoría para cambios de asistencia.

- Definir política de retención y borrado con la institución antes de
  producción real.

- Evitar logs con nombres completos, tokens, contraseñas o payloads
  sensibles.

# 18. Servicio FastAPI de IA

## 18.1 Responsabilidades

- Calcular tendencias y patrones a partir de datasets ya autorizados.

- Generar métricas derivadas.

- Ejecutar modelos de ML cuando existan datos suficientes y hayan sido
  validados.

- Exponer endpoints internos consumidos únicamente por Spring Boot.

## 18.2 No responsabilidades

- No autenticar usuarios finales.

- No consultar directamente cualquier tabla de producción sin control.

- No modificar asistencias.

- No emitir sanciones ni diagnósticos.

## 18.3 Endpoints internos

- POST /internal/v1/analysis/summary

- POST /internal/v1/analysis/patterns

- POST /internal/v1/predict/attendance-risk (fase posterior)

# 19. Estrategia de IA por fases

| **Fase** | **Técnica** | **Datos** | **Salida** |
|----|----|----|----|
| IA-1 | Reglas + estadística | Asistencias agregadas | tendencias, alertas simples, porcentajes |
| IA-2 | Pandas + detección de patrones | Histórico suficiente | patrones por día/periodo |
| IA-3 | scikit-learn | Dataset validado y balanceado | probabilidad/score informativo con explicabilidad |
| IA-4 | LLM opcional | Resumen estructurado autorizado | consultas en lenguaje natural con grounding |

# 20. Reglas del asistente IA

- Toda respuesta sobre una persona debe derivar de datos autorizados por
  Spring Boot.

- Cuando no existan datos suficientes, responder que no hay evidencia
  suficiente.

- No inferir causas de faltas o tardanzas.

- Mostrar cifras verificables junto con resúmenes cuando sea posible.

- No exponer información de estudiantes de otras secciones al docente.

- Las alertas no cambian automáticamente el estado de una asistencia.

# 21. Reportes

- Filtros: rango de fechas, sección, curso, estudiante y estado.

- Exportación XLSX mediante Apache POI o equivalente compatible.

- Exportación PDF mediante biblioteca mantenida y compatible con
  licencia del proyecto.

- Los reportes deben respetar permisos de usuario.

- Datos calculados server-side para evitar manipulación desde frontend.

# 22. Manejo de concurrencia y consistencia

- Registro de asistencia dentro de transacción @Transactional.

- Índice UNIQUE(sesion_id, estudiante_id) como última barrera contra
  duplicados.

- Capturar DataIntegrityViolationException y devolver 409
  idempotente/amigable.

- Usar reloj del servidor; no aceptar la hora del frontend como
  autoridad.

- Cierre de sesión de clase debe consolidar ausencias de forma
  transaccional o mediante proceso controlado.

# 23. Migraciones y datos iniciales

- Flyway será el mecanismo de versionado de esquema.

- No utilizar ddl-auto=create/update en producción; preferir validate.

- Migraciones con nombres V1\_\_init.sql, V2\_\_attendance_indexes.sql,
  etc.

- Seeds de desarrollo separados de producción.

- Crear datos demo ficticios para exposición: admin, docente, 20-30
  estudiantes, una sección y horarios.

# 24. Observabilidad

- Logs estructurados con niveles INFO/WARN/ERROR.

- Correlación por requestId para errores relevantes.

- Health endpoints con Spring Boot Actuator.

- Métricas mínimas: registros exitosos, duplicados, errores QR, latencia
  API, fallos FastAPI.

- No registrar secretos ni PII innecesaria.

# 25. Estrategia de pruebas

| **Nivel** | **Herramienta** | **Cobertura mínima** | **Ejemplos** |
|----|----|----|----|
| Unitarias backend | JUnit + Mockito | Servicios críticos | clasificación presente/tardanza, permisos, duplicados |
| Integración backend | Spring Boot Test + Testcontainers | API + PostgreSQL | registro real, constraints, transacciones |
| Frontend unitarias | Angular TestBed/Jest | servicios/componentes | manejo de respuestas y voz |
| E2E | Playwright/Cypress | flujos principales | login, escaneo simulado, dashboard |
| FastAPI | pytest | endpoints y análisis | summary/patterns con datasets controlados |
| Seguridad | tests API | roles y accesos | docente no ve otra sección |
| Carga básica | k6/JMeter opcional | registro concurrente | 30-50 registros rápidos |

# 26. Casos técnicos obligatorios

- T-001: registrar estudiante correcto dentro de tolerancia → PRESENTE.

- T-002: registrar después de tolerancia → TARDANZA.

- T-003: mismo QR dos veces → una sola fila; segundo intento
  409/duplicado.

- T-004: código inexistente → no se crea registro.

- T-005: estudiante de otra sección → rechazado.

- T-006: sesión cerrada → rechazado.

- T-007: dos requests simultáneos del mismo estudiante → una sola
  asistencia.

- T-008: caída de FastAPI → asistencia sigue funcionando; solo análisis
  IA se degrada.

- T-009: navegador sin TTS → registro sigue siendo exitoso con mensaje
  visual.

- T-010: cámara denegada → permitir ingreso manual.

- T-011: docente intenta acceder a sección no autorizada → 403.

- T-012: corrección manual → requiere motivo y genera auditoría.

# 27. Despliegue

## 27.1 Desarrollo local

- Angular: localhost:4200

- Spring Boot: localhost:8080

- FastAPI: localhost:8000

- PostgreSQL: localhost:5432

- Docker Compose opcional para DB y servicios.

## 27.2 Producción/piloto

- Frontend Angular en Vercel o hosting estático HTTPS.

- Spring Boot en Render u otra plataforma Java compatible.

- PostgreSQL administrado en Neon o equivalente.

- FastAPI en Render/servicio Python separado.

- Variables sensibles mediante environment variables; nunca en
  repositorio.

- CORS restringido al dominio real del frontend.

- Backup y monitoreo antes de uso institucional real.

# 28. Variables de entorno

| **Variable** | **Servicio** | **Propósito** |
|----|----|----|
| DATABASE_URL / DB\_\* | Spring Boot | Conexión PostgreSQL |
| JWT_SECRET o SESSION_SECRET | Spring Boot | Seguridad autenticación |
| CORS_ALLOWED_ORIGINS | Spring Boot | Origen Angular permitido |
| AI_SERVICE_URL | Spring Boot | URL interna FastAPI |
| AI_SERVICE_KEY | Spring Boot/FastAPI | Autenticación servicio-servicio |
| SPRING_PROFILES_ACTIVE | Spring Boot | dev/test/prod |
| API_BASE_URL | Angular | URL backend |

# 29. CI/CD

- Cada pull request debe compilar frontend/backend y ejecutar pruebas.

- Backend: ./mvnw test o Gradle equivalente.

- Frontend: npm ci + npm run test + npm run build.

- FastAPI: pytest y lint.

- No desplegar automáticamente si las pruebas fallan.

- Main representa versión estable; development integra trabajo antes de
  release.

- Aplicar migraciones de BD de forma controlada antes/como parte del
  despliegue.

# 30. Convenciones para trabajar con agentes de IA

- Antes de modificar código, el agente debe inspeccionar archivos
  existentes y explicar brevemente la causa/alcance.

- No reescribir módulos completos si basta un cambio localizado.

- Mantener contratos API y nombres definidos en este TRD salvo decisión
  documentada.

- Cada implementación debe incluir o actualizar pruebas.

- El agente debe ejecutar compilación/tests antes de declarar una tarea
  terminada.

- No realizar commit, push, deploy o cambios de infraestructura salvo
  que se solicite explícitamente.

- No eliminar validaciones de seguridad para “hacer que funcione”.

- Toda nueva migración debe ser aditiva y segura para datos existentes
  cuando aplique.

- Si modifica DB, actualizar documentación/modelo correspondiente.

- Entregar al finalizar: archivos modificados, explicación, pruebas
  ejecutadas y pendientes manuales.

# 31. Definition of Done técnica

- Código compila sin errores.

- Pruebas relevantes pasan.

- Endpoint documentado en OpenAPI cuando corresponda.

- Validaciones server-side implementadas.

- Errores manejados con códigos HTTP consistentes.

- Migración incluida si cambia persistencia.

- Sin secretos hardcodeados.

- Permisos/roles verificados.

- Flujo principal probado manualmente o E2E.

- Documentación actualizada.

# 32. Orden recomendado de implementación

13. Inicializar repositorio, Docker Compose y entornos.

14. Crear PostgreSQL + migraciones base.

15. Implementar autenticación y roles.

16. Implementar catálogo académico: grados, secciones, cursos,
    estudiantes y docentes.

17. Implementar horarios y sesiones de clase.

18. Implementar registro de asistencia y reglas
    PRESENTE/TARDANZA/AUSENTE.

19. Construir Modo Aula Angular con código manual.

20. Agregar cámara y lectura QR.

21. Agregar Text-to-Speech.

22. Construir dashboard docente y reportes.

23. Agregar auditoría y correcciones/justificaciones.

24. Crear FastAPI con análisis estadístico.

25. Agregar patrones y alertas IA.

26. Agregar asistente conversacional opcional con permisos y grounding.

27. Preparar despliegue piloto y pruebas finales.

# 33. Arquitectura final resumida

> ESTUDIANTE → Cámara laptop / QR → Angular Modo Aula\
> ↓\
> Spring Boot\
> reglas + seguridad + API\
> ↙ ↘\
> PostgreSQL FastAPI\
> asistencias/datos análisis e IA\
> ↓\
> Dashboard docente\
> reportes + alertas + resumen

# 34. Criterio de éxito técnico del MVP

El MVP técnico se considera terminado cuando un docente autenticado
puede abrir una sesión de clase, un estudiante puede registrar su
asistencia por QR leído desde la cámara de una laptop o por código
manual, el backend valida pertenencia, horario y duplicidad usando la
hora del servidor, PostgreSQL persiste el resultado, Angular confirma
visualmente y por voz, el dashboard refleja el registro y el cierre de
clase produce ausentes correctamente, todo con pruebas automatizadas de
los casos críticos.

# 35. Entregables técnicos esperados

- Código fuente frontend Angular.

- Código fuente backend Spring Boot.

- Servicio FastAPI/Python.

- Migraciones PostgreSQL.

- OpenAPI/Swagger.

- Colección Postman opcional.

- Pruebas automatizadas.

- Docker Compose de desarrollo.

- README de instalación/ejecución.

- Documentación PRD, TRD, UI/UX, flujos, testing y plan de ejecución.
