# Base de Datos — AulaIA

## 1. Información del documento

- **Proyecto:** AulaIA — Sistema Inteligente de Asistencia Escolar
- **Documento:** Diseño de Base de Datos
- **Versión:** 1.0
- **Fecha:** 16 de agosto de 2026
- **Motor recomendado:** PostgreSQL
- **Objetivo:** Definir el modelo de datos relacional, relaciones, restricciones, índices, reglas de integridad y lineamientos de evolución de la base de datos.

---

# 2. Objetivos del diseño de datos

La base de datos debe permitir:

- Administrar usuarios y roles.
- Registrar estudiantes y docentes.
- Gestionar grados, secciones y cursos.
- Configurar horarios.
- Crear sesiones reales de clase.
- Registrar asistencias.
- Calcular presentes, tardanzas, ausencias y justificaciones.
- Evitar duplicidades.
- Mantener trazabilidad de cambios.
- Proveer datos confiables para reportes.
- Entregar información controlada al módulo de Inteligencia Artificial.

---

# 3. Principios de diseño

## 3.1 Integridad referencial

Todas las relaciones importantes deberán estar protegidas por claves foráneas.

## 3.2 Normalización

El modelo se mantendrá normalizado al menos hasta 3FN para evitar duplicación innecesaria de información.

## 3.3 Auditoría

Las modificaciones relevantes de asistencia deberán registrarse.

## 3.4 Seguridad

No se almacenarán:

- Contraseñas en texto plano.
- Datos biométricos.
- Fotografías para reconocimiento facial.
- Información sensible dentro del QR.

## 3.5 Migraciones

Todos los cambios estructurales deberán implementarse mediante migraciones versionadas con Flyway.

---

# 4. Convenciones

## Tablas

Nombres en minúscula y plural:

```text
usuarios
estudiantes
docentes
grados
secciones
cursos
horarios
sesiones_clase
asistencias
auditoria
```

## Columnas

Formato:

```text
snake_case
```

Ejemplo:

```text
fecha_hora
created_at
updated_at
```

## Claves primarias

```text
id BIGSERIAL PRIMARY KEY
```

o identidad equivalente de PostgreSQL.

## Fechas

Usar:

```text
TIMESTAMP WITH TIME ZONE
```

cuando se requiera momento exacto.

Para fechas académicas:

```text
DATE
```

Para horas configurables:

```text
TIME
```

---

# 5. Modelo conceptual

```text
USUARIO
  │
  ├────────────── DOCENTE
  │
  └────────────── AUDITORIA


GRADO
  │
  ▼
SECCION
  │
  ├────────────── ESTUDIANTE
  │
  └────────────── HORARIO
                     │
                     ▼
               SESION_CLASE
                     │
                     ▼
                ASISTENCIA
                     │
                     ▼
                ESTUDIANTE


CURSO ───────────── HORARIO

DOCENTE ─────────── HORARIO
```

---

# 6. Entidades principales

## 6.1 usuarios

Almacena cuentas de acceso para administradores y docentes.

```sql
CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(30) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    ultimo_login_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Valores iniciales de `rol`:

```text
ADMIN
DOCENTE
```

### Reglas

- `username` debe ser único.
- `password_hash` nunca almacena contraseña plana.
- Un usuario inactivo no podrá iniciar sesión.

---

## 6.2 docentes

Información académica asociada a un usuario con rol DOCENTE.

```sql
CREATE TABLE docentes (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL UNIQUE,
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_docentes_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
);
```

### Relación

```text
usuarios 1 ─── 0..1 docentes
```

---

## 6.3 grados

Catálogo de grados escolares.

```sql
CREATE TABLE grados (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    nivel VARCHAR(50) NOT NULL DEFAULT 'PRIMARIA',
    orden SMALLINT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Ejemplos:

```text
5.º Primaria
6.º Primaria
```

---

## 6.4 secciones

Representa una sección académica dentro de un grado y periodo.

```sql
CREATE TABLE secciones (
    id BIGSERIAL PRIMARY KEY,
    grado_id BIGINT NOT NULL,
    nombre VARCHAR(20) NOT NULL,
    periodo_academico VARCHAR(20) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_secciones_grado
        FOREIGN KEY (grado_id)
        REFERENCES grados(id),

    CONSTRAINT uq_seccion_grado_periodo
        UNIQUE (grado_id, nombre, periodo_academico)
);
```

Ejemplo:

```text
Grado: 6.º Primaria
Sección: A
Periodo: 2026
```

---

## 6.5 estudiantes

Contiene los estudiantes registrados en el sistema.

```sql
CREATE TABLE estudiantes (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    qr_token VARCHAR(120) NOT NULL UNIQUE,
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    seccion_id BIGINT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_estudiantes_seccion
        FOREIGN KEY (seccion_id)
        REFERENCES secciones(id)
);
```

### Reglas

- `codigo` es el código escolar del estudiante.
- `qr_token` debe ser único y no debe contener datos personales.
- Un estudiante solo puede pertenecer a una sección activa en esta primera versión.

Ejemplos:

```text
codigo: EST-000145
qr_token: AULAIA:STUDENT:7QX9K2
```

---

## 6.6 cursos

Catálogo de cursos.

```sql
CREATE TABLE cursos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Ejemplo:

```text
Computación
```

---

## 6.7 horarios

Define cuándo se dicta un curso para una sección determinada.

```sql
CREATE TABLE horarios (
    id BIGSERIAL PRIMARY KEY,
    curso_id BIGINT NOT NULL,
    seccion_id BIGINT NOT NULL,
    docente_id BIGINT NOT NULL,
    dia_semana SMALLINT NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    tolerancia_minutos SMALLINT NOT NULL DEFAULT 10,
    minutos_antes_apertura SMALLINT NOT NULL DEFAULT 15,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_horarios_curso
        FOREIGN KEY (curso_id)
        REFERENCES cursos(id),

    CONSTRAINT fk_horarios_seccion
        FOREIGN KEY (seccion_id)
        REFERENCES secciones(id),

    CONSTRAINT fk_horarios_docente
        FOREIGN KEY (docente_id)
        REFERENCES docentes(id),

    CONSTRAINT ck_horarios_dia_semana
        CHECK (dia_semana BETWEEN 1 AND 7),

    CONSTRAINT ck_horarios_tolerancia
        CHECK (tolerancia_minutos >= 0),

    CONSTRAINT ck_horarios_apertura
        CHECK (minutos_antes_apertura >= 0),

    CONSTRAINT ck_horarios_horas
        CHECK (hora_fin > hora_inicio)
);
```

Convención para `dia_semana`:

```text
1 = Lunes
2 = Martes
3 = Miércoles
4 = Jueves
5 = Viernes
6 = Sábado
7 = Domingo
```

---

# 7. Sesiones reales de clase

## 7.1 sesiones_clase

Un horario representa una plantilla semanal.

Una sesión de clase representa una ocurrencia real en una fecha específica.

Ejemplo:

```text
Horario:
Martes 09:00 - 10:30

Sesión:
Martes 18/08/2026
09:00 - 10:30
```

Tabla:

```sql
CREATE TABLE sesiones_clase (
    id BIGSERIAL PRIMARY KEY,
    horario_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    hora_apertura TIMESTAMPTZ NULL,
    hora_cierre TIMESTAMPTZ NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PROGRAMADA',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_sesiones_horario
        FOREIGN KEY (horario_id)
        REFERENCES horarios(id),

    CONSTRAINT uq_sesion_horario_fecha
        UNIQUE (horario_id, fecha)
);
```

Estados permitidos:

```text
PROGRAMADA
ABIERTA
CERRADA
CANCELADA
```

---

# 8. Tabla de asistencias

## 8.1 asistencias

```sql
CREATE TABLE asistencias (
    id BIGSERIAL PRIMARY KEY,
    sesion_clase_id BIGINT NOT NULL,
    estudiante_id BIGINT NOT NULL,
    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(30) NOT NULL,
    metodo VARCHAR(30) NOT NULL,
    observacion VARCHAR(500) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_asistencias_sesion
        FOREIGN KEY (sesion_clase_id)
        REFERENCES sesiones_clase(id),

    CONSTRAINT fk_asistencias_estudiante
        FOREIGN KEY (estudiante_id)
        REFERENCES estudiantes(id),

    CONSTRAINT uq_asistencia_sesion_estudiante
        UNIQUE (sesion_clase_id, estudiante_id),

    CONSTRAINT ck_asistencia_estado
        CHECK (estado IN (
            'PRESENTE',
            'TARDANZA',
            'AUSENTE',
            'JUSTIFICADO'
        )),

    CONSTRAINT ck_asistencia_metodo
        CHECK (metodo IN (
            'QR',
            'CODIGO',
            'MANUAL_DOCENTE',
            'SISTEMA'
        ))
);
```

### Restricción más importante

```sql
UNIQUE (sesion_clase_id, estudiante_id)
```

Esto evita duplicados incluso si dos solicitudes llegan al backend casi al mismo tiempo.

---

# 9. Ausencias

Existen dos estrategias posibles.

## Estrategia recomendada

Cuando una sesión se cierra, el sistema genera registros `AUSENTE` para los estudiantes activos de la sección que no tengan asistencia.

Ventajas:

- Reportes simples.
- Historial explícito.
- Fácil consulta.
- Fácil justificación posterior.

Ejemplo:

```text
Juan     PRESENTE
Carlos   TARDANZA
Lucía    AUSENTE
```

---

# 10. Justificaciones

Se recomienda separar la justificación del registro principal.

```sql
CREATE TABLE justificaciones (
    id BIGSERIAL PRIMARY KEY,
    asistencia_id BIGINT NOT NULL UNIQUE,
    motivo VARCHAR(500) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    revisado_por_usuario_id BIGINT NULL,
    fecha_revision TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_justificaciones_asistencia
        FOREIGN KEY (asistencia_id)
        REFERENCES asistencias(id),

    CONSTRAINT fk_justificaciones_usuario
        FOREIGN KEY (revisado_por_usuario_id)
        REFERENCES usuarios(id),

    CONSTRAINT ck_justificaciones_estado
        CHECK (estado IN (
            'PENDIENTE',
            'APROBADA',
            'RECHAZADA'
        ))
);
```

Cuando se aprueba una justificación:

```text
asistencias.estado = JUSTIFICADO
```

La justificación conserva el motivo y usuario que realizó la revisión.

---

# 11. Auditoría

## 11.1 auditoria

```sql
CREATE TABLE auditoria (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NULL,
    entidad VARCHAR(80) NOT NULL,
    entidad_id BIGINT NULL,
    accion VARCHAR(80) NOT NULL,
    valor_anterior JSONB NULL,
    valor_nuevo JSONB NULL,
    ip_origen VARCHAR(64) NULL,
    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_auditoria_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
);
```

Ejemplos de acciones:

```text
CREAR_ASISTENCIA_MANUAL
MODIFICAR_ASISTENCIA
JUSTIFICAR_ASISTENCIA
REABRIR_SESION
CERRAR_SESION
MODIFICAR_HORARIO
MODIFICAR_ESTUDIANTE
GENERAR_NUEVO_QR
```

---

# 12. Relación completa

```text
usuarios
  │
  └── docentes
        │
        └── horarios
              │
              └── sesiones_clase
                    │
                    └── asistencias
                          │
                          ├── estudiantes
                          │     │
                          │     └── secciones
                          │           │
                          │           └── grados
                          │
                          └── justificaciones


cursos
  │
  └── horarios


usuarios
  │
  └── auditoria
```

---

# 13. Diagrama ER simplificado

```text
┌─────────────┐
│  usuarios   │
└──────┬──────┘
       │ 1
       │
       │ 0..1
┌──────▼──────┐
│  docentes   │
└──────┬──────┘
       │
       │ 1:N
       ▼
┌─────────────┐      ┌─────────────┐
│  horarios   │◄─────│   cursos    │
└──────┬──────┘      └─────────────┘
       │
       │ 1:N
       ▼
┌─────────────────┐
│ sesiones_clase  │
└──────┬──────────┘
       │
       │ 1:N
       ▼
┌─────────────┐
│ asistencias │
└──────┬──────┘
       │
       │ N:1
       ▼
┌─────────────┐
│ estudiantes │
└──────┬──────┘
       │
       │ N:1
       ▼
┌─────────────┐
│  secciones  │
└──────┬──────┘
       │
       │ N:1
       ▼
┌─────────────┐
│   grados    │
└─────────────┘
```

---

# 14. Índices

Además de claves primarias y UNIQUE, se recomiendan:

```sql
CREATE INDEX idx_estudiantes_seccion
ON estudiantes(seccion_id);

CREATE INDEX idx_horarios_seccion
ON horarios(seccion_id);

CREATE INDEX idx_horarios_docente
ON horarios(docente_id);

CREATE INDEX idx_horarios_curso
ON horarios(curso_id);

CREATE INDEX idx_sesiones_fecha
ON sesiones_clase(fecha);

CREATE INDEX idx_sesiones_estado
ON sesiones_clase(estado);

CREATE INDEX idx_asistencias_estudiante
ON asistencias(estudiante_id);

CREATE INDEX idx_asistencias_estado
ON asistencias(estado);

CREATE INDEX idx_asistencias_fecha_hora
ON asistencias(fecha_hora);

CREATE INDEX idx_auditoria_fecha
ON auditoria(fecha_hora);
```

Índice compuesto recomendado:

```sql
CREATE INDEX idx_asistencias_sesion_estado
ON asistencias(sesion_clase_id, estado);
```

---

# 15. Reglas de integridad

## RF-DATA-001

Un estudiante no puede registrar más de una asistencia por sesión.

```text
UNIQUE (sesion_clase_id, estudiante_id)
```

## RF-DATA-002

Un `qr_token` solo puede pertenecer a un estudiante.

## RF-DATA-003

Un código escolar debe ser único.

## RF-DATA-004

Una sesión solo puede existir una vez por horario y fecha.

## RF-DATA-005

Una sección no puede repetirse para el mismo grado y periodo académico.

## RF-DATA-006

Una asistencia siempre debe pertenecer a:

- Un estudiante válido.
- Una sesión válida.

## RF-DATA-007

Los estados de asistencia deben estar restringidos.

## RF-DATA-008

Los métodos de registro deben estar restringidos.

---

# 16. Flujo transaccional de registro

La operación de registro debe ejecutarse dentro de una transacción.

Pseudo flujo:

```text
BEGIN

1. Buscar sesión activa.
2. Resolver estudiante por qr_token o código.
3. Validar estudiante activo.
4. Validar que pertenece a la sección del horario.
5. Verificar duplicado.
6. Obtener hora del servidor.
7. Calcular estado.
8. INSERT asistencia.
9. COMMIT
```

En caso de error:

```text
ROLLBACK
```

---

# 17. Cálculo de estado

Ejemplo:

```text
Horario inicio:
09:00

Tolerancia:
10 minutos
```

Regla:

```text
fecha_hora <= 09:10
→ PRESENTE

fecha_hora > 09:10
→ TARDANZA
```

El cálculo se realiza en Spring Boot.

La base de datos conserva el resultado.

---

# 18. Cierre de sesión y generación de ausentes

Cuando una sesión cambia a:

```text
CERRADA
```

el backend puede ejecutar:

```sql
INSERT INTO asistencias (
    sesion_clase_id,
    estudiante_id,
    estado,
    metodo
)
SELECT
    :sesionId,
    e.id,
    'AUSENTE',
    'SISTEMA'
FROM estudiantes e
JOIN sesiones_clase sc
    ON sc.id = :sesionId
JOIN horarios h
    ON h.id = sc.horario_id
WHERE e.seccion_id = h.seccion_id
  AND e.activo = TRUE
  AND NOT EXISTS (
      SELECT 1
      FROM asistencias a
      WHERE a.sesion_clase_id = sc.id
        AND a.estudiante_id = e.id
  );
```

Esta operación debe ejecutarse transaccionalmente.

---

# 19. Consultas importantes

## Asistencia de una sesión

```sql
SELECT
    e.codigo,
    e.nombres,
    e.apellidos,
    a.estado,
    a.fecha_hora
FROM asistencias a
JOIN estudiantes e
    ON e.id = a.estudiante_id
WHERE a.sesion_clase_id = :sesionId
ORDER BY e.apellidos, e.nombres;
```

---

## Resumen por estado

```sql
SELECT
    estado,
    COUNT(*) AS total
FROM asistencias
WHERE sesion_clase_id = :sesionId
GROUP BY estado;
```

---

## Historial de un estudiante

```sql
SELECT
    sc.fecha,
    c.nombre AS curso,
    a.estado,
    a.fecha_hora
FROM asistencias a
JOIN sesiones_clase sc
    ON sc.id = a.sesion_clase_id
JOIN horarios h
    ON h.id = sc.horario_id
JOIN cursos c
    ON c.id = h.curso_id
WHERE a.estudiante_id = :estudianteId
ORDER BY sc.fecha DESC;
```

---

# 20. Vista opcional de resumen

Podría crearse una vista:

```sql
CREATE VIEW vw_resumen_asistencia_sesion AS
SELECT
    sc.id AS sesion_id,
    sc.fecha,
    h.seccion_id,
    h.curso_id,
    COUNT(a.id) AS total_registros,
    COUNT(*) FILTER (WHERE a.estado = 'PRESENTE') AS presentes,
    COUNT(*) FILTER (WHERE a.estado = 'TARDANZA') AS tardanzas,
    COUNT(*) FILTER (WHERE a.estado = 'AUSENTE') AS ausentes,
    COUNT(*) FILTER (WHERE a.estado = 'JUSTIFICADO') AS justificados
FROM sesiones_clase sc
JOIN horarios h
    ON h.id = sc.horario_id
LEFT JOIN asistencias a
    ON a.sesion_clase_id = sc.id
GROUP BY
    sc.id,
    sc.fecha,
    h.seccion_id,
    h.curso_id;
```

---

# 21. Datos para IA

El servicio de IA no debe consultar directamente la base de datos productiva.

Spring Boot debe preparar datasets controlados.

Ejemplo:

```json
{
  "estudianteId": 145,
  "totalSesiones": 20,
  "presentes": 15,
  "tardanzas": 3,
  "ausentes": 2,
  "porcentajeAsistencia": 75.0
}
```

Para análisis por sección:

```json
{
  "seccionId": 12,
  "periodo": "2026-08",
  "asistenciaPromedio": 91.4,
  "totalTardanzas": 18,
  "totalAusencias": 9
}
```

---

# 22. Privacidad y protección de datos

## El QR NO debe contener

```text
nombre completo
dirección
teléfono
documento de identidad
fecha de nacimiento
información familiar
```

El QR solo contiene:

```text
token aleatorio
```

Ejemplo:

```text
AULAIA:STUDENT:7QX9K2
```

---

# 23. Regeneración de QR

Si un código QR se pierde o se comparte indebidamente:

```text
Administrador
      ↓
Regenerar QR
      ↓
Nuevo qr_token
      ↓
QR anterior queda inválido
```

La acción debe quedar auditada.

---

# 24. Eliminación de registros

No se recomienda eliminar físicamente estudiantes con historial.

Utilizar:

```text
activo = false
```

Para asistencias, evitar DELETE físico salvo casos excepcionales.

Preferir:

- Corrección.
- Anulación controlada.
- Auditoría.

---

# 25. Evolución futura: matrículas

La primera versión tiene:

```text
estudiantes.seccion_id
```

Si AulaIA crece a varios años escolares, se recomienda evolucionar a:

```text
matriculas
```

Ejemplo:

```sql
CREATE TABLE matriculas (
    id BIGSERIAL PRIMARY KEY,
    estudiante_id BIGINT NOT NULL,
    seccion_id BIGINT NOT NULL,
    periodo_academico VARCHAR(20) NOT NULL,
    estado VARCHAR(30) NOT NULL
);
```

Entonces se eliminaría progresivamente la relación directa:

```text
estudiantes.seccion_id
```

Esta evolución debe realizarse mediante migración controlada.

---

# 26. Evolución futura: múltiples colegios

Si se convierte en SaaS, agregar:

```text
instituciones
sedes
```

y `institucion_id` o `sede_id` en entidades principales.

Para la versión inicial no es necesario.

---

# 27. Migraciones Flyway

Estructura:

```text
backend/
└── src/main/resources/
    └── db/
        └── migration/
            ├── V1__create_usuarios.sql
            ├── V2__create_academic_structure.sql
            ├── V3__create_estudiantes.sql
            ├── V4__create_horarios.sql
            ├── V5__create_sesiones_clase.sql
            ├── V6__create_asistencias.sql
            ├── V7__create_justificaciones.sql
            ├── V8__create_auditoria.sql
            └── V9__create_indexes.sql
```

Regla:

Una migración aplicada en producción nunca debe editarse.

Para corregir:

```text
crear una nueva migración
```

---

# 28. Datos iniciales

Ejemplo de seed para desarrollo:

```text
ADMIN
DOCENTE
```

Grados:

```text
5.º Primaria
6.º Primaria
```

Curso:

```text
Computación
```

Sección:

```text
6.º A
```

Todos los estudiantes utilizados en desarrollo deben ser ficticios.

---

# 29. Backup

Para producción se recomienda:

- Backup automático diario si el proveedor lo permite.
- Retención de varios días.
- Exportación periódica.
- Prueba de restauración.

La existencia de backups no reemplaza la auditoría.

---

# 30. Seguridad de conexión

Producción:

```text
SSL/TLS obligatorio
```

Variables:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
```

Nunca:

```text
password hardcodeado
```

---

# 31. Pool de conexiones

Spring Boot deberá utilizar HikariCP.

Configuración ejemplo:

```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=30000
```

Los valores finales dependerán del hosting.

---

# 32. Zona horaria

La aplicación deberá definir claramente la zona horaria de la institución.

Para Lima:

```text
America/Lima
```

Recomendación:

- Persistir momentos exactos como `TIMESTAMPTZ`.
- Aplicar `America/Lima` para reglas y visualización escolar.

---

# 33. Retención de datos

Debe definirse con la institución.

Categorías:

```text
Estudiantes
Asistencias
Auditoría
Justificaciones
Logs técnicos
```

El sistema debe permitir eventualmente anonimizar o archivar históricos.

---

# 34. Rendimiento esperado

Para un piloto escolar:

```text
100 - 1000 estudiantes
```

El modelo propuesto es suficiente.

Una tabla de asistencias con cientos de miles de registros seguirá siendo manejable con PostgreSQL e índices adecuados.

---

# 35. Casos críticos que debe soportar la base

## Caso 1

Dos solicitudes intentan registrar al mismo estudiante al mismo tiempo.

Resultado:

```text
solo una INSERT puede completarse
```

gracias a:

```text
UNIQUE(sesion_clase_id, estudiante_id)
```

---

## Caso 2

Un estudiante escanea QR de otra sección.

Resultado:

```text
Spring Boot lo rechaza
```

No se crea registro.

---

## Caso 3

Se cierra la clase.

Resultado:

```text
se generan AUSENTE para estudiantes sin registro
```

---

## Caso 4

Docente corrige una asistencia.

Resultado:

```text
asistencia actualizada
+
registro en auditoria
```

---

# 36. Definition of Done para cambios de base de datos

Un cambio se considera terminado cuando:

- Existe migración Flyway.
- La migración aplica desde una base limpia.
- La migración aplica desde la versión anterior.
- Las foreign keys son válidas.
- Los constraints funcionan.
- Se crean índices necesarios.
- JPA está alineado con el esquema.
- Existen pruebas.
- No se pierden datos existentes.
- La documentación está actualizada.

---

# 37. Reglas para agentes de IA

Cuando una IA modifique la base de datos debe:

1. Leer las migraciones existentes.
2. No modificar migraciones ya aplicadas.
3. Crear una nueva migración.
4. Mantener compatibilidad de datos.
5. No eliminar columnas sin una estrategia explícita.
6. Crear claves foráneas cuando corresponda.
7. Crear restricciones de unicidad cuando la regla lo exija.
8. Agregar índices basados en consultas reales.
9. Actualizar entidades JPA.
10. Actualizar DTOs si corresponde.
11. Actualizar pruebas.
12. Ejecutar migraciones y tests antes de declarar el cambio terminado.
13. No hacer commit, push ni deploy sin autorización.

---

# 38. Resumen final

Las entidades principales de AulaIA son:

```text
usuarios
docentes
grados
secciones
estudiantes
cursos
horarios
sesiones_clase
asistencias
justificaciones
auditoria
```

La relación más importante del sistema es:

```text
Estudiante
    +
Sesión de clase
    =
Asistencia única
```

garantizada mediante:

```sql
UNIQUE (sesion_clase_id, estudiante_id)
```

El modelo está diseñado para:

- Registrar asistencia rápida por QR.
- Evitar duplicados.
- Calcular tardanzas.
- Generar ausencias.
- Permitir justificaciones.
- Mantener auditoría.
- Generar reportes.
- Alimentar análisis de IA.
- Evolucionar a un sistema escolar de mayor escala.
