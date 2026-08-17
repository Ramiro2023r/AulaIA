# Flujos del Sistema — AulaIA

## 1. Información del documento

- **Proyecto:** AulaIA — Sistema Inteligente de Asistencia Escolar
- **Documento:** Flujos del Sistema
- **Versión:** 1.0
- **Fecha:** 16 de agosto de 2026
- **Objetivo:** Definir los flujos funcionales y operativos principales del sistema AulaIA para orientar diseño, desarrollo, pruebas y trabajo con agentes de IA.

---

# 2. Visión general

AulaIA tendrá tres actores principales:

- **Estudiante**
- **Docente**
- **Administrador**

Los flujos del sistema se centran en:

1. Acceso y autenticación.
2. Gestión académica.
3. Gestión de estudiantes.
4. Configuración de horarios.
5. Apertura de sesión de clase.
6. Registro de asistencia por QR.
7. Registro manual por código.
8. Confirmación visual y por voz.
9. Cálculo de presente o tardanza.
10. Cierre de sesión y generación de ausencias.
11. Corrección y justificación.
12. Reportes.
13. Inteligencia Artificial.
14. Auditoría.
15. Manejo de errores.

---

# 3. Flujo general del sistema

```text
ADMINISTRADOR
    │
    ├── Configura estructura académica
    ├── Registra docentes
    ├── Registra estudiantes
    ├── Configura horarios
    └── Genera QR
            │
            ▼
DOCENTE
    │
    ├── Inicia sesión
    ├── Abre clase
    └── Activa Modo Aula
            │
            ▼
ESTUDIANTE
    │
    ├── Escanea QR
    │       o
    └── Ingresa código manual
            │
            ▼
SPRING BOOT
    │
    ├── Valida sesión
    ├── Valida estudiante
    ├── Valida sección
    ├── Valida horario
    ├── Valida duplicado
    ├── Calcula estado
    └── Guarda asistencia
            │
            ▼
ANGULAR
    │
    ├── Muestra confirmación
    └── Reproduce voz
            │
            ▼
DOCENTE
    │
    ├── Revisa dashboard
    ├── Cierra sesión
    ├── Revisa ausencias
    ├── Corrige / justifica
    ├── Genera reportes
    └── Consulta IA
```

---

# 4. Flujo de autenticación

## Actor

```text
DOCENTE / ADMINISTRADOR
```

## Flujo

```text
Usuario abre AulaIA
      ↓
Pantalla Login
      ↓
Ingresa usuario y contraseña
      ↓
POST /api/v1/auth/login
      ↓
Spring Security valida credenciales
      ↓
¿Credenciales correctas?
      │
      ├── NO
      │     ↓
      │  Mostrar error
      │
      └── SÍ
            ↓
        Generar token
            ↓
        Obtener rol
            ↓
        Redirigir según rol
```

## Resultado por rol

```text
ADMIN
→ Dashboard administrativo

DOCENTE
→ Dashboard docente
```

---

# 5. Flujo de cierre de sesión

```text
Usuario
   ↓
Cerrar sesión
   ↓
Eliminar token local
   ↓
Invalidar refresh token si aplica
   ↓
Volver a Login
```

---

# 6. Flujo de creación de estudiante

## Actor

```text
ADMIN
```

## Flujo

```text
Dashboard
   ↓
Estudiantes
   ↓
Nuevo estudiante
   ↓
Completar:
- nombres
- apellidos
- código
- grado
- sección
   ↓
Validar datos
   ↓
¿Código ya existe?
   │
   ├── SÍ
   │    ↓
   │  Mostrar error
   │
   └── NO
        ↓
Generar qr_token
        ↓
Guardar estudiante
        ↓
Generar QR
        ↓
Mostrar confirmación
```

---

# 7. Flujo de generación de QR

```text
Estudiante creado
      ↓
Backend genera token aleatorio
      ↓
Token asociado al estudiante
      ↓
Frontend genera imagen QR
      ↓
Administrador puede:
- descargar
- imprimir
```

El QR contiene únicamente:

```text
AULAIA:STUDENT:<TOKEN>
```

No contiene datos personales.

---

# 8. Flujo de regeneración de QR

```text
Admin
  ↓
Detalle estudiante
  ↓
Regenerar QR
  ↓
Confirmación
  ↓
Backend genera nuevo token
  ↓
QR anterior queda inválido
  ↓
Guardar auditoría
  ↓
Mostrar nuevo QR
```

---

# 9. Flujo de creación de docente

```text
Admin
  ↓
Docentes
  ↓
Nuevo docente
  ↓
Ingresar datos
  ↓
Crear usuario
  ↓
Asignar rol DOCENTE
  ↓
Crear perfil docente
  ↓
Guardar
  ↓
Confirmación
```

---

# 10. Flujo de estructura académica

```text
Admin
  ↓
Crear grado
  ↓
Crear sección
  ↓
Crear curso
  ↓
Registrar docente
  ↓
Crear horario
```

Dependencias:

```text
GRADO
  ↓
SECCIÓN

CURSO + SECCIÓN + DOCENTE
  ↓
HORARIO
```

---

# 11. Flujo de creación de horario

```text
Admin
  ↓
Horarios
  ↓
Nuevo horario
  ↓
Seleccionar:
- curso
- sección
- docente
- día
- hora inicio
- hora fin
- tolerancia
- minutos antes de apertura
  ↓
Validaciones
  ↓
¿Hora fin > hora inicio?
  │
  ├── NO → Error
  └── SÍ
        ↓
¿Existe conflicto?
  │
  ├── SÍ → Advertir / rechazar según regla
  └── NO
        ↓
Guardar horario
```

---

# 12. Flujo de sesión de clase

Un horario es una plantilla.

Una sesión de clase es una ocurrencia real.

Ejemplo:

```text
Horario:
Martes 09:00 - 10:30

Sesión:
18/08/2026
```

---

# 13. Flujo de apertura de sesión

## Actor

```text
DOCENTE
```

## Flujo

```text
Dashboard docente
     ↓
Clase actual
     ↓
Abrir sesión
     ↓
Backend valida:
- horario
- fecha
- docente
- estado
     ↓
¿Sesión ya existe?
     │
     ├── SÍ
     │    ↓
     │  Usar sesión existente
     │
     └── NO
          ↓
      Crear sesión
          ↓
Estado = ABIERTA
          ↓
Registrar hora_apertura
          ↓
Modo Aula disponible
```

---

# 14. Flujo Modo Aula

```text
Docente
  ↓
Abrir Modo Aula
  ↓
Pantalla fullscreen
  ↓
Mostrar:
- curso
- sección
- cámara
- botón código manual
  ↓
Esperar estudiante
```

---

# 15. Flujo de permiso de cámara

```text
Modo Aula
   ↓
Solicitar acceso a cámara
   ↓
¿Permiso concedido?
   │
   ├── SÍ
   │    ↓
   │  Activar cámara
   │
   └── NO
        ↓
Mostrar:
"Necesitamos acceso a la cámara"
        ↓
Opciones:
- Reintentar
- Código manual
```

---

# 16. Flujo de escaneo QR

```text
Cámara activa
    ↓
Estudiante muestra QR
    ↓
ZXing detecta QR
    ↓
Extraer token
    ↓
Bloquear lectura temporal
    ↓
Enviar al backend
```

Request:

```json
{
  "codigo": "AULAIA:STUDENT:7QX9K2",
  "metodo": "QR"
}
```

---

# 17. Flujo backend de registro de asistencia

```text
POST /api/v1/asistencias/registrar
       ↓
Buscar sesión activa
       ↓
¿Existe sesión?
       │
       ├── NO
       │    ↓
       │  SESSION_NOT_ACTIVE
       │
       └── SÍ
            ↓
Resolver estudiante
            ↓
¿Existe estudiante?
       │
       ├── NO
       │    ↓
       │  STUDENT_NOT_FOUND
       │
       └── SÍ
            ↓
¿Estudiante activo?
       │
       ├── NO → Rechazar
       └── SÍ
            ↓
Validar sección
            ↓
¿Pertenece a la sección?
       │
       ├── NO
       │    ↓
       │  STUDENT_NOT_IN_SECTION
       │
       └── SÍ
            ↓
Validar duplicado
            ↓
¿Ya tiene asistencia?
       │
       ├── SÍ
       │    ↓
       │  ATTENDANCE_ALREADY_REGISTERED
       │
       └── NO
            ↓
Obtener hora servidor
            ↓
Calcular estado
            ↓
Guardar asistencia
            ↓
Responder
```

---

# 18. Flujo de cálculo de presente

Ejemplo:

```text
Hora inicio:
09:00

Tolerancia:
10 minutos
```

Regla:

```text
Registro <= 09:10
→ PRESENTE
```

---

# 19. Flujo de cálculo de tardanza

```text
Registro > 09:10
y sesión sigue abierta
→ TARDANZA
```

---

# 20. Flujo de confirmación visual

Respuesta backend:

```json
{
  "success": true,
  "nombre": "Juan",
  "hora": "09:03",
  "estado": "PRESENTE"
}
```

Angular:

```text
Recibe respuesta
      ↓
Detiene cámara temporalmente
      ↓
Muestra:
✅ ¡Hola Juan!
PRESENTE
09:03
      ↓
Activa voz
```

---

# 21. Flujo de voz

```text
Registro correcto
    ↓
Angular construye mensaje
    ↓
SpeechSynthesisUtterance
    ↓
Reproduce voz
    ↓
Esperar 3 - 5 segundos
    ↓
Limpiar pantalla
    ↓
Reactivar cámara
```

---

# 22. Voz para presente

```text
¡Hola Juan!
Tu asistencia fue registrada correctamente.
¡Que tengas una excelente clase de computación!
```

---

# 23. Voz para tardanza

```text
¡Hola Juan!
Tu asistencia fue registrada.
Puedes ingresar a tu clase.
```

---

# 24. Flujo de duplicado

```text
Alumno escanea QR
      ↓
Backend detecta registro existente
      ↓
No crea nueva asistencia
      ↓
Respuesta:
ATTENDANCE_ALREADY_REGISTERED
      ↓
Angular muestra:
"Tu asistencia ya fue registrada"
      ↓
Voz informativa
      ↓
Volver a escáner
```

---

# 25. Flujo de código inválido

```text
QR / código no corresponde
      ↓
Backend
      ↓
STUDENT_NOT_FOUND
      ↓
Angular
      ↓
Mostrar advertencia
      ↓
Voz:
"No pude encontrar ese código"
      ↓
Permitir reintento
```

---

# 26. Flujo de estudiante de otra sección

```text
Estudiante válido
    ↓
Código existe
    ↓
No pertenece a la sección
    ↓
Backend rechaza
    ↓
No registra asistencia
    ↓
Mensaje genérico y seguro
```

No revelar información académica innecesaria.

---

# 27. Flujo de registro manual por código

```text
Modo Aula
   ↓
Ingresar código manual
   ↓
Modal
   ↓
Alumno escribe:
EST-000145
   ↓
Registrar
   ↓
POST /api/v1/asistencias/registrar
   ↓
Misma lógica backend que QR
```

Regla:

```text
QR y código manual
→ mismo servicio
→ mismas validaciones
```

---

# 28. Flujo de cámara no disponible

```text
Modo Aula
   ↓
No existe cámara
   ↓
Mostrar:
"No encontramos una cámara disponible"
   ↓
Ofrecer:
"Ingresar código manual"
```

---

# 29. Flujo de error de red

```text
Alumno escanea
   ↓
Frontend intenta registrar
   ↓
No hay conexión
   ↓
No confirmar asistencia
   ↓
Mostrar:
"No pudimos registrar la asistencia"
   ↓
Permitir reintento
```

Nunca mostrar éxito si backend no confirmó.

---

# 30. Flujo de dashboard docente

```text
Docente inicia sesión
     ↓
Dashboard
     ↓
Backend devuelve:
- clases del día
- sesión actual
- presentes
- tardanzas
- ausentes
- porcentaje
     ↓
Mostrar KPIs
```

---

# 31. Flujo actualización dashboard

Después de una asistencia:

```text
Nueva asistencia
   ↓
Base de datos
   ↓
Dashboard actualiza
```

Puede implementarse inicialmente mediante:

```text
Polling
```

Ejemplo:

```text
cada 5 - 10 segundos
```

En una fase futura:

```text
WebSocket / SSE
```

---

# 32. Flujo de cierre de sesión de clase

```text
Docente
  ↓
Cerrar sesión
  ↓
Confirmación
  ↓
Backend
  ↓
Validar sesión ABIERTA
  ↓
Generar AUSENTE faltantes
  ↓
estado = CERRADA
  ↓
registrar hora_cierre
  ↓
mostrar resumen
```

---

# 33. Flujo de generación de ausencias

```text
Cerrar sesión
    ↓
Obtener estudiantes activos de sección
    ↓
Comparar con asistencias existentes
    ↓
Por cada estudiante sin registro:
    ↓
Crear:
estado = AUSENTE
metodo = SISTEMA
```

---

# 34. Flujo resumen final de clase

Después de cerrar:

```text
Clase cerrada

Presentes:
26

Tardanzas:
2

Ausentes:
2

Asistencia:
86.7%
```

Acciones:

```text
Ver lista

Generar reporte

Consultar IA
```

---

# 35. Flujo de consulta de asistencias

```text
Docente/Admin
    ↓
Asistencias
    ↓
Aplicar filtros
    ↓
Backend consulta
    ↓
Mostrar resultados
```

Filtros:

```text
Fecha
Curso
Sección
Estado
Estudiante
```

---

# 36. Flujo de corrección de asistencia

## Actor

```text
DOCENTE autorizado / ADMIN
```

## Flujo

```text
Listado asistencia
      ↓
Seleccionar registro
      ↓
Modificar asistencia
      ↓
Mostrar estado actual
      ↓
Seleccionar nuevo estado
      ↓
Ingresar motivo obligatorio
      ↓
Guardar
      ↓
Backend valida permisos
      ↓
Actualizar asistencia
      ↓
Guardar auditoría:
- valor anterior
- valor nuevo
- usuario
- motivo
      ↓
Confirmación
```

---

# 37. Flujo de justificación

```text
Asistencia AUSENTE
      ↓
Registrar justificación
      ↓
Motivo
      ↓
Estado:
PENDIENTE
      ↓
Usuario autorizado revisa
      ↓
¿Aprobada?
      │
      ├── NO
      │     ↓
      │  RECHAZADA
      │
      └── SÍ
            ↓
      APROBADA
            ↓
asistencia.estado = JUSTIFICADO
            ↓
Auditoría
```

---

# 38. Flujo de reportes

```text
Docente/Admin
   ↓
Reportes
   ↓
Seleccionar filtros
   ↓
Generar
   ↓
Backend consulta datos
   ↓
Mostrar resumen y gráficos
   ↓
Opciones:
- Excel
- PDF
```

---

# 39. Flujo exportar Excel

```text
Usuario
  ↓
Exportar Excel
  ↓
Backend
  ↓
Aplicar filtros
  ↓
Generar XLSX
  ↓
Descargar archivo
```

---

# 40. Flujo exportar PDF

```text
Usuario
  ↓
Exportar PDF
  ↓
Backend
  ↓
Aplicar filtros
  ↓
Generar documento
  ↓
Descargar
```

---

# 41. Flujo módulo IA

```text
Docente
  ↓
AulaIA IA
  ↓
Selecciona o escribe consulta
  ↓
Angular
  ↓
Spring Boot
  ↓
Validar permisos
  ↓
Consultar datos necesarios
  ↓
Preparar dataset seguro
  ↓
FastAPI
  ↓
Analizar
  ↓
Responder
  ↓
Spring Boot
  ↓
Angular
  ↓
Mostrar insight
```

---

# 42. Flujo IA — resumen semanal

```text
Docente:
"Resumen de esta semana"
       ↓
Spring Boot obtiene:
- sesiones
- presentes
- tardanzas
- ausentes
       ↓
FastAPI analiza
       ↓
Devuelve:
- porcentaje
- tendencia
- observaciones
       ↓
Mostrar respuesta
```

---

# 43. Flujo IA — estudiantes con tardanzas

```text
Docente
  ↓
"Estudiantes con tardanzas"
  ↓
Backend consulta solo sus clases
  ↓
Agrega datos
  ↓
IA analiza recurrencia
  ↓
Devuelve lista informativa
```

---

# 44. Flujo IA — patrón de ausencias

```text
Historial
   ↓
Pandas
   ↓
Agrupar por estudiante / fecha
   ↓
Detectar recurrencia
   ↓
Generar insight
```

Ejemplo:

```text
"El estudiante EST-0018 registra
3 ausencias en sus últimas 4 sesiones."
```

---

# 45. Flujo IA con error

```text
Spring Boot
   ↓
FastAPI no disponible
   ↓
No afectar registro de asistencia
   ↓
Mostrar:
"El análisis inteligente no está disponible temporalmente"
```

Regla:

```text
IA nunca debe bloquear
el funcionamiento del sistema principal.
```

---

# 46. Flujo de auditoría

Eventos:

```text
Modificar asistencia
Regenerar QR
Modificar horario
Desactivar estudiante
Aprobar justificación
Cerrar sesión
```

Flujo:

```text
Acción relevante
   ↓
Guardar operación principal
   ↓
Registrar auditoría
   ↓
Usuario
Entidad
Acción
Fecha
Valor anterior
Valor nuevo
```

---

# 47. Flujo de consulta de auditoría

```text
ADMIN
  ↓
Auditoría
  ↓
Filtros:
- usuario
- entidad
- acción
- fecha
  ↓
Consultar
  ↓
Mostrar historial
```

---

# 48. Flujo desactivar estudiante

```text
Admin
  ↓
Estudiante
  ↓
Desactivar
  ↓
Confirmar
  ↓
activo = false
  ↓
No eliminar historial
  ↓
Auditoría
```

---

# 49. Flujo desactivar docente

```text
Admin
  ↓
Docente
  ↓
Desactivar
  ↓
usuario.activo = false
docente.activo = false
  ↓
Mantener históricos
```

---

# 50. Flujo de configuración de voz

```text
Admin / Docente autorizado
   ↓
Configuración
   ↓
Voz
   ↓
Activar / desactivar
   ↓
Idioma
   ↓
Velocidad
   ↓
Volumen
   ↓
Probar voz
```

---

# 51. Flujo de configuración de asistencia

```text
Admin
  ↓
Configuración
  ↓
Asistencia
  ↓
Definir:
- tolerancia predeterminada
- minutos antes de apertura
- cierre automático
- generación de ausentes
  ↓
Guardar
```

---

# 52. Flujo de sesión próxima

```text
Horario programado
    ↓
Faltan X minutos
    ↓
Dashboard muestra:
"Próxima clase"
    ↓
Al llegar ventana de apertura:
"Disponible para abrir"
```

---

# 53. Flujo de sesión fuera de horario

```text
Docente intenta abrir
fuera de ventana
   ↓
Backend valida regla
   ↓
Rechaza o solicita permiso especial
```

La política exacta debe definirse con la institución.

---

# 54. Flujo de sesión cancelada

```text
Admin / Docente autorizado
   ↓
Cancelar sesión
   ↓
Motivo
   ↓
estado = CANCELADA
   ↓
No generar ausencias
   ↓
Auditoría
```

---

# 55. Flujo de reapertura de sesión

Solo si se habilita:

```text
Sesión CERRADA
   ↓
Usuario autorizado
   ↓
Reabrir sesión
   ↓
Motivo obligatorio
   ↓
estado = ABIERTA
   ↓
Auditoría
```

---

# 56. Flujo de datos de IA

```text
PostgreSQL
   ↓
Spring Boot
   ↓
Seleccionar datos mínimos necesarios
   ↓
Anonimizar cuando corresponda
   ↓
FastAPI
   ↓
Resultado
```

FastAPI no modifica la base.

---

# 57. Flujo de error genérico backend

```text
Request
  ↓
Controller
  ↓
Service
  ↓
Error
  ↓
Global Exception Handler
  ↓
Respuesta estándar
```

Ejemplo:

```json
{
  "status": 409,
  "code": "ATTENDANCE_ALREADY_REGISTERED",
  "message": "La asistencia ya fue registrada"
}
```

---

# 58. Flujo de permisos

```text
Request autenticado
   ↓
Spring Security
   ↓
Validar token
   ↓
Validar rol
   ↓
¿Tiene permiso?
   │
   ├── NO → 403
   └── SÍ → continuar
```

---

# 59. Matriz de acciones por rol

| Acción | ADMIN | DOCENTE | ESTUDIANTE |
|---|---:|---:|---:|
| Login | Sí | Sí | No |
| Registrar estudiante | Sí | No | No |
| Generar QR | Sí | No | No |
| Crear horario | Sí | No | No |
| Abrir clase | Sí | Sí | No |
| Registrar asistencia QR | No directo | No directo | Sí |
| Ver dashboard docente | Sí | Sí | No |
| Corregir asistencia | Sí | Según permiso | No |
| Ver reportes | Sí | Sus clases | No |
| Consultar IA | Sí | Sus clases | No |
| Ver auditoría | Sí | No | No |

---

# 60. Flujo de primer uso del sistema

```text
Instalar / desplegar AulaIA
    ↓
Crear ADMIN
    ↓
Configurar periodo
    ↓
Crear grados
    ↓
Crear secciones
    ↓
Crear cursos
    ↓
Crear docentes
    ↓
Crear estudiantes
    ↓
Generar QR
    ↓
Crear horarios
    ↓
Primera clase
```

---

# 61. Flujo de exposición / demo

```text
1. Login docente
2. Mostrar dashboard
3. Abrir clase 6.º A
4. Activar Modo Aula
5. Escanear QR de Juan
6. Mostrar PRESENTE + voz
7. Repetir QR
8. Mostrar duplicado
9. Registrar otro alumno como TARDANZA
10. Volver al dashboard
11. Cerrar sesión
12. Mostrar ausentes
13. Generar reporte
14. Consultar IA:
    "Resume la clase de hoy"
```

---

# 62. Flujo de recuperación ante fallo de cámara

```text
Cámara falla
   ↓
Mostrar alternativa
   ↓
Código manual
   ↓
Misma validación backend
   ↓
Continuar operación
```

---

# 63. Flujo de recuperación ante fallo de IA

```text
FastAPI falla
   ↓
Registrar log
   ↓
Mostrar mensaje
   ↓
Sistema principal sigue funcionando
```

---

# 64. Flujo de recuperación ante base de datos no disponible

```text
Backend intenta operación
   ↓
PostgreSQL no disponible
   ↓
No confirmar registro
   ↓
Mostrar error temporal
   ↓
Log técnico
```

No se debe simular éxito.

---

# 65. Flujo de cambio de estado de estudiante

```text
ACTIVO
  ↓
Admin desactiva
  ↓
INACTIVO
```

Un estudiante inactivo:

```text
no puede registrar asistencia
```

pero conserva históricos.

---

# 66. Flujo de consulta de historial del estudiante

```text
Admin / Docente autorizado
   ↓
Detalle estudiante
   ↓
Asistencia
   ↓
Backend devuelve historial
   ↓
Mostrar:
- fecha
- curso
- estado
- hora
```

---

# 67. Flujo de cálculo de porcentaje

Ejemplo:

```text
Total sesiones válidas = 20

PRESENTE = 15
TARDANZA = 3
AUSENTE = 2
```

La fórmula institucional debe definirse.

Versión inicial:

```text
asistencia =
(PRESENTE + TARDANZA) / TOTAL * 100
```

Si la institución requiere que tardanza tenga otro peso, será configurable en una evolución.

---

# 68. Flujo de notificaciones futuras

Fuera del MVP, pero arquitectura preparada:

```text
Ausencia / patrón
   ↓
Regla autorizada
   ↓
Notificación
   ↓
Padre / tutor / coordinación
```

No implementar sin política institucional y autorización.

---

# 69. Flujo futuro multi-colegio

```text
Institución
   ↓
Sede
   ↓
Grados
   ↓
Secciones
   ↓
Estudiantes
```

No forma parte del MVP.

---

# 70. Reglas transversales

## Regla 1

Nunca confiar en el frontend para determinar estado de asistencia.

## Regla 2

Nunca crear asistencia sin sesión válida.

## Regla 3

Nunca permitir duplicado por estudiante y sesión.

## Regla 4

Nunca utilizar IA para modificar asistencias automáticamente.

## Regla 5

Nunca confirmar éxito si el backend no persistió el registro.

## Regla 6

Toda modificación administrativa relevante debe auditarse.

---

# 71. Flujos críticos para testing

Deben existir pruebas para:

```text
Login correcto

Login incorrecto

Crear estudiante

Código duplicado

QR válido

QR inválido

Registro PRESENTE

Registro TARDANZA

Registro duplicado

Alumno otra sección

Sesión cerrada

Código manual

Cierre de sesión

Generación AUSENTE

Corrección asistencia

Justificación

Reportes

IA disponible

IA no disponible
```

---

# 72. Definition of Done de un flujo

Un flujo se considera completado cuando:

- Frontend está implementado.
- Backend está implementado.
- Persistencia está implementada.
- Validaciones existen.
- Permisos existen.
- Manejo de errores existe.
- Loading existe.
- Success existe.
- Error existe.
- Pruebas existen.
- Auditoría existe cuando corresponde.
- Documentación está actualizada.

---

# 73. Reglas para agentes de IA

Cuando una IA implemente un flujo deberá:

1. Leer PRD, TRD, Arquitectura, Base de Datos, UI/UX y este documento.
2. Identificar actor, precondiciones y resultado esperado.
3. Revisar implementación existente antes de modificar.
4. Mantener una única fuente de lógica de negocio.
5. No duplicar validaciones entre QR y código manual.
6. Respetar contratos API.
7. Añadir validaciones backend.
8. Añadir manejo de errores frontend.
9. Añadir pruebas.
10. Ejecutar pruebas afectadas.
11. Documentar lo implementado.
12. No hacer commit, push ni deploy sin autorización.

---

# 74. Resumen de flujos principales

```text
ADMIN
  ↓
Configura sistema
  ↓
DOCENTE
  ↓
Abre clase
  ↓
ESTUDIANTE
  ↓
QR / Código
  ↓
BACKEND
  ↓
Validación
  ↓
PRESENTE / TARDANZA
  ↓
VOZ
  ↓
CIERRE
  ↓
AUSENTES
  ↓
REPORTES
  ↓
IA
```

Este documento define el comportamiento esperado de AulaIA desde la preparación académica hasta el registro, análisis y cierre de la asistencia.
