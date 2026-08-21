# AulaIA Backend

Backend Spring Boot del Sistema Inteligente de Asistencia Escolar (AulaIA).
Contiene la API REST y las reglas de negocio del sistema; es la fuente de
verdad de las validaciones de asistencia (ver `/docs` — fuente oficial del proyecto).

## Requisitos

| Herramienta | Versión |
|---|---|
| Java | **21** (versión oficial de desarrollo de AulaIA) |
| Spring Boot | 3.5.16 |
| Maven | 3.9.16 (incluido en el repo como Maven Wrapper) |

> **Java 21:** AulaIA requiere Java 21 como versión de desarrollo oficial
> (configurado en `pom.xml` con `release 21`). El Maven Wrapper usa la JDK
> indicada por `JAVA_HOME`; si el `PATH` del sistema contiene otra versión
> de Java (p. ej. Java 25), los builds vía `mvnw` seguirán usando la JDK 21
> siempre que `JAVA_HOME` apunte a ella. No se modifica configuración
> global de Windows.

## Estructura de paquetes

```text
com.aulaia
├── AulaiaBackendApplication.java   # Punto de entrada
├── config/     # Configuración general
├── controller/ # Controladores REST (/api/v1)
├── service/    # Reglas de negocio
├── repository/ # Acceso a datos (Spring Data JPA)
├── entity/     # Entidades JPA
├── dto/        # Objetos de transferencia
├── mapper/     # Mapeo DTO <-> Entidad (MapStruct)
├── exception/  # Manejo global de errores REST
├── security/   # Seguridad (JWT en Sprint 2)
├── client/     # Clientes HTTP (servicio IA FastAPI)
└── audit/      # Auditoría de modificaciones
```

## Cómo ejecutar los tests

```bash
./mvnw test
```

Los tests usan el perfil `test` (aislado): el contexto carga sin necesidad
de una base de datos externa. `AulaiaBackendApplicationTests.contextLoads()`
valida que el contexto Spring arranca correctamente.

## Cómo empaquetar

```bash
./mvnw clean package
```

Genera `target/aulaia-backend-0.1.0-SNAPSHOT.jar`.

## Cómo iniciar localmente

El backend usa perfiles de Spring (`default`, `dev`, `test`, `prod`).
Actualmente solo el perfil `test` puede arrancar sin base de datos:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=test
# o con el jar:
java -jar target/aulaia-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=test
```

Endpoints disponibles en desarrollo:

- Salud: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- API docs: `http://localhost:8080/v3/api-docs`

> **Importante:** el perfil `dev` requiere PostgreSQL local (sección
> "PostgreSQL local" más abajo). No crear tablas manualmente: Flyway
> aplica las migraciones al arrancar.

## PostgreSQL local

El backend se conecta a PostgreSQL local para desarrollo:

| Dato | Valor de ejemplo (este entorno) |
|---|---|
| Host | `localhost` |
| Puerto | `5433` |
| Base de datos | `aulaia_db` |
| Usuario | `aulaia_user` |

Todos los valores son configurables mediante variables de entorno
(`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`); el
puerto puede variar según la instalación de cada desarrollador.

Ejemplo de ejecución con perfil `dev` (PowerShell):

```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="5433"
$env:DB_NAME="aulaia_db"
$env:DB_USERNAME="aulaia_user"
$env:DB_PASSWORD="TU_PASSWORD_LOCAL"
$env:SPRING_PROFILES_ACTIVE="dev"

.\mvnw.cmd spring-boot:run
```

> La contraseña se define únicamente como variable de entorno local;
> nunca se guarda en el repositorio, en el README ni en migraciones.

## API Documentation

Swagger UI está pensado principalmente para desarrollo y pruebas.

| Recurso | URL (desarrollo) |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

Perfiles:

- `dev` y `test`: documentación habilitada por defecto.
- `prod`: deshabilitada por defecto (configuración segura); solo se
  habilita si la infraestructura la activa explícitamente con
  `SPRINGDOC_API_DOCS_ENABLED=true` y `SPRINGDOC_SWAGGER_UI_ENABLED=true`.

La especificación es OpenAPI 3 (configuración en `config/OpenApiConfig`).
No hay endpoints funcionales documentados todavía; la documentación se
poblará a medida que se implementen los módulos (Sprint 2 en adelante).

## Profiles

| Perfil | Archivo | Propósito |
|---|---|---|
| `default` | `application.properties` | Configuración común a todos los entornos (sin credenciales) |
| `dev` | `application-dev.properties` | Desarrollo local con PostgreSQL |
| `test` | `application-test.properties` | Pruebas automatizadas |
| `prod` | `application-prod.properties` | Producción con configuración exclusivamente externa |

### dev

Desarrollo local con PostgreSQL. Usa variables de entorno con valores
de respaldo solo para desarrollo: `DB_HOST`, `DB_PORT`, `DB_NAME`,
`DB_USERNAME`, `DB_PASSWORD`. Incluye `ddl-auto=validate` y Flyway activo.

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
.\mvnw.cmd spring-boot:run
```

> Requiere PostgreSQL corriendo en `localhost:5433` en este entorno
> (instalación local; el puerto puede variar por desarrollador).

### test

Pruebas automatizadas. No depende de PostgreSQL externo: excluye
temporalmente la auto-configuración de DataSource (JPA y Flyway
retroceden solos). Esta exclusión es **temporal** hasta integrar
Testcontainers (Sprint 18); no se usa H2.

```powershell
$env:SPRING_PROFILES_ACTIVE="test"
.\mvnw.cmd spring-boot:run
```

### prod

Producción usando exclusivamente configuración externa. Exige las
variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`,
`JWT_SECRET` e `IA_SERVICE_URL` — **sin valores por defecto**: si falta
alguna, el arranque falla. Nunca usar `localhost` como valor obligatorio
de producción.

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
.\mvnw.cmd spring-boot:run
```

## Variables de entorno

| Variable | Uso | Default (solo dev) |
|---|---|---|
| `PORT` | Puerto HTTP del backend | `8080` |
| `SPRING_PROFILES_ACTIVE` | Perfil activo | `dev` |
| `DB_HOST` | Host de PostgreSQL | `localhost` |
| `DB_PORT` | Puerto de PostgreSQL | `5433` |
| `DB_NAME` | Base de datos | `aulaia_db` |
| `DB_USERNAME` | Usuario de BD | `aulaia_user` |
| `DB_PASSWORD` | Contraseña de BD | vacío (nunca real) |
| `JWT_SECRET` | Firma JWT (Sprint 2) | — (requerida en prod) |
| `AULAIA_BOOTSTRAP_ADMIN_ENABLED` | Crea el primer ADMIN al arrancar (Sprint 2) | `false` |
| `AULAIA_BOOTSTRAP_ADMIN_USERNAME` | Username del primer ADMIN | — (vacío, sin fallback) |
| `AULAIA_BOOTSTRAP_ADMIN_PASSWORD` | Contraseña del primer ADMIN (≥ 12 caracteres) | — (vacío, sin fallback) |
| `IA_SERVICE_URL` | URL del servicio IA (Sprint 16) | — (requerida en prod) |

Referencia de ejemplo: `.env.example` en la raíz del repositorio
(solo placeholders, nunca secretos reales).

## Seguridad (estado actual)

Spring Security está instalado pero **sin autenticación real todavía**:
la configuración actual (`security/SecurityConfig.java`) es temporal y
permite únicamente rutas públicas mínimas — documentación
(`/swagger-ui/**`, `/v3/api-docs/**`), salud (`/actuator/health`) y
`/test/**` (controladores solo de pruebas). El resto queda autenticado,
aunque aún no existen endpoints funcionales. JWT, login y roles
(ADMIN/DOCENTE) se implementarán en el Sprint 2.

## Security foundation

Base de seguridad lista (Sprint 2, Prompt 2.2):

- **PasswordEncoder BCrypt**: `BCryptPasswordEncoder` como bean único
  (`security/SecurityConfig`). Nunca se guardan contraseñas planas; el
  hash se almacenará en `usuarios.password_hash`.
- **API stateless**: `SessionCreationPolicy.STATELESS` — no se crean
  sesiones HTTP; cada solicitud se autentica por su Bearer JWT.
- **CSRF deshabilitado** (decisión de arquitectura): al ser una API REST
  stateless sin cookies ni sesiones, el token CSRF no aplica. Se
  documenta en el javadoc de `SecurityConfig`.
- **401/403 en JSON**: `RestAuthenticationEntryPoint`,
  `RestAccessDeniedHandler` y el `JwtAuthenticationFilter` devuelven
  `ApiErrorResponse` consistente (códigos `UNAUTHORIZED`/`FORBIDDEN`),
  nunca HTML ni stack traces.
- **Carga de usuarios real**: `CustomUserDetailsService` consulta la
  tabla `usuarios` (JPA). Usuarios inexistentes o inactivos
  (`activo=false`) → 401 (tratados como inexistentes, sin revelar estado
  de cuenta).

## Login

Endpoint público (no requiere Bearer):

```text
POST /api/v1/auth/login
```

Ejemplo (sin contraseña real):

```json
{
  "username": "admin",
  "password": "<PASSWORD>"
}
```

Respuesta `200`:

```json
{
  "accessToken": "<JWT>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": { "id": 1, "username": "admin", "rol": "ADMIN" }
}
```

- `accessToken`: JWT para `Authorization: Bearer <accessToken>`.
- `tokenType`: siempre `Bearer`.
- `expiresIn`: segundos hasta la expiración (configurable con
  `JWT_EXPIRATION`, default 1 hora).
- `user`: solo `id`, `username`, `rol` (nunca `passwordHash`).
- Credenciales incorrectas (username inexistente, password incorrecta o
  usuario inactivo) → **401** `INVALID_CREDENTIALS` "Usuario o contraseña
  incorrectos" — misma respuesta en todos los casos (sin enumeración de
  usuarios).
- `ultimoLoginAt` se actualiza (hora del servidor) solo en login exitoso.
- Nunca se registran passwords ni JWT en logs; el login no usa sesiones
  HTTP ni cache.

## JWT

Autenticación Bearer JWT integrada con Spring Security (Prompt 2.4).

Flujo de autenticación de una request:

```text
Authorization: Bearer <JWT>
        ↓
JwtAuthenticationFilter (OncePerRequestFilter)
        ↓
JwtService (firma HS256, claims, expiración)
        ↓
CustomUserDetailsService (tabla usuarios: existe + activo)
        ↓
SecurityContext (UsernamePasswordAuthenticationToken + roles)
```

- **Token Bearer**: `Authorization: Bearer <token>` — única fuente
  aceptada (no query params, cookies ni body).
- **Claims**: `sub` (username), `userId`, `rol` (ADMIN | DOCENTE),
  `iat`, `exp`. Nunca incluye `passwordHash` ni datos personales.
- **Firma**: HS256 (HMAC-SHA256) con `JWT_SECRET` (mínimo 32 caracteres).
- **Expiración**: `JWT_EXPIRATION` en milisegundos (default `3600000` =
  1 hora), definida en `jwt.expiration-ms`.
- **Fail fast**: si `JWT_SECRET` falta o es corto, el backend no arranca.
- **Token inválido** (expirado, firma inválida, malformado, usuario
  inexistente o inactivo) → **401** `{"code":"UNAUTHORIZED","message":
  "Token inválido o expirado"}` — sin exponer la razón criptográfica.
- **Roles**: `ADMIN → ROLE_ADMIN`, `DOCENTE → ROLE_DOCENTE`
  (autorización con `hasRole(...)`).
- **Nunca almacenar secretos en Git**: `JWT_SECRET` se define solo por
  variable de entorno (`JWT_SECRET=CHANGE_ME` en `.env.example` es un
  placeholder). Los tokens completos no se registran en logs.

## Initial administrator bootstrap

Creación segura y controlada del **primer** ADMIN de una instalación
(Sprint 2, `service/InitialAdminBootstrap`). Especialmente útil para:
entorno local, Docker, instalaciones demo y despliegues nuevos. No existe
registro público ni endpoint para crear administradores.

Variables de entorno (`application.properties`, sin fallback real):

| Variable | Default | Descripción |
|---|---|---|
| `AULAIA_BOOTSTRAP_ADMIN_ENABLED` | `false` | Habilita el bootstrap. **Deshabilitado por defecto en todos los perfiles** |
| `AULAIA_BOOTSTRAP_ADMIN_USERNAME` | vacío | Username del primer ADMIN (≤ 100 caracteres) |
| `AULAIA_BOOTSTRAP_ADMIN_PASSWORD` | vacío | Contraseña del primer ADMIN (**mínimo 12 caracteres**) |

Flujo al iniciar el backend:

1. **Configurar las variables** en el entorno (`.env`, Docker, hosting):

   ```text
   AULAIA_BOOTSTRAP_ADMIN_ENABLED=true
   AULAIA_BOOTSTRAP_ADMIN_USERNAME=admin
   AULAIA_BOOTSTRAP_ADMIN_PASSWORD=<STRONG_PASSWORD>
   ```

   > `<STRONG_PASSWORD>` es un placeholder: usa una contraseña fuerte real
   > (mínimo 12 caracteres), nunca una por defecto y nunca en el repositorio.

2. **Iniciar el backend** (Flyway y JPA ya aplicaron las migraciones).
3. Si **no existe** ningún ADMIN, se crea uno con rol `ADMIN`,
   `activo=true`, `ultimoLoginAt=null` y contraseña con **BCrypt**.
4. Si **ya existe** un ADMIN, el bootstrap se omite de forma idempotente
   (nunca crea duplicados).

Comportamientos de seguridad:

- Bootstrap **deshabilitado** → no hace nada ni crea usuarios.
- Variables **inválidas** con bootstrap habilitado (username vacío o > 100
  caracteres, password vacía o < 12 caracteres) → **el arranque falla**
  (no se crea un admin parcialmente configurado, ni contraseña débil).
- Si `AULAIA_BOOTSTRAP_ADMIN_USERNAME` ya pertenece a otro usuario
  (p. ej. un DOCENTE) → el arranque falla sin sobrescribirlo ni cambiar su
  rol silenciosamente.
- Nunca se registran en logs: password, hash, JWT ni secretos.
- No se inserta ningún administrador en migraciones Flyway
  (las migraciones solo describen estructura).

**Recomendación**: después de crear el administrador, desactiva el
bootstrap:

```text
AULAIA_BOOTSTRAP_ADMIN_ENABLED=false
```

Así se evita la re-creación (aunque sea idempotente) y se mantiene la
creación de cuentas bajo control administrativo.

## Notas

- Java compilado con `release 21` (configurado en `pom.xml`).
- Lombok y MapStruct configurados como procesadores de anotaciones.
- La configuración de Actuator expone únicamente `health` (nada más).
- Documentación oficial del proyecto: `/docs` (referencia principal `.md`).