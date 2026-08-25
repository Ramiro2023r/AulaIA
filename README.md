# AulaIA

Sistema web de asistencia escolar para aulas de computación. AulaIA permite administrar la estructura académica, registrar asistencia mediante QR o código, trabajar en **Modo Aula**, consultar reportes y notificar a apoderados por Telegram cuando su vinculación está habilitada.

> Estado: proyecto académico / demostración. Usa datos ficticios para desarrollo y nunca cargues datos reales de menores en entornos de prueba.

## Índice

- [Características](#características)
- [Arquitectura](#arquitectura)
- [Inicio rápido con Docker](#inicio-rápido-con-docker)
- [Configuración](#configuración)
- [Accesos y URLs](#accesos-y-urls)
- [Uso inicial recomendado](#uso-inicial-recomendado)
- [Telegram](#telegram)
- [Desarrollo sin Docker](#desarrollo-sin-docker)
- [Pruebas](#pruebas)
- [Operación y solución de problemas](#operación-y-solución-de-problemas)
- [Seguridad y privacidad](#seguridad-y-privacidad)
- [Documentación del proyecto](#documentación-del-proyecto)

## Características

- Gestión de grados, secciones, cursos, docentes, horarios y estudiantes.
- Sesiones de clase y registro de asistencia por QR o código manual.
- Estados de asistencia: presente, tardanza, ausencia y justificación.
- Modo Aula con cámara, confirmación visual y voz.
- Dashboards para administración y docentes, reportes Excel/PDF y auditoría.
- Autenticación JWT y roles `ADMIN` / `DOCENTE`.
- Apoderados reutilizables: un apoderado puede asociarse a varios estudiantes.
- Vinculación opcional con Telegram y aviso de asistencia posterior al commit.
- Servicio FastAPI para análisis y consultas de apoyo.

## Arquitectura

```text
 Navegador
     │
     ▼
 Angular + Nginx (puerto 4200)
     │  /api/v1
     ▼
 Spring Boot (puerto 8080) ───── FastAPI / análisis (puerto 8000)
     │
     ▼
 PostgreSQL 17 (red interna Docker)
```

| Componente | Tecnología | Responsabilidad |
|---|---|---|
| `frontend/` | Angular 18, TypeScript | Interfaz administrativa, Modo Aula y QR. |
| `backend/` | Java 21, Spring Boot 3.5 | API, seguridad, reglas de negocio, Flyway y notificaciones. |
| `data-science/` | Python 3.12, FastAPI | Análisis y consultas de apoyo. |
| `postgres` | PostgreSQL 17 | Persistencia de datos y migraciones Flyway. |

El backend es la fuente de verdad: el frontend no determina si una asistencia es válida ni modifica datos críticos por sí mismo.

---

## Inicio rápido con Docker

Esta es la forma recomendada de instalar AulaIA en una PC nueva. Docker instala y conecta todos los componentes sin requerir Java, Node.js, Python ni PostgreSQL en el sistema anfitrión.

### 1. Requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) actualizado y en ejecución.
- Docker Compose v2 (incluido con Docker Desktop actual).
- [Git](https://git-scm.com/downloads).

Comprueba la instalación:

```powershell
docker --version
docker compose version
git --version
```

### 2. Clonar el repositorio

```powershell
git clone https://github.com/Ramiro2023r/AulaIA.git
Set-Location AulaIA
```

### 3. Crear la configuración local

Nunca se sube el archivo `.env`; contiene secretos de tu instalación.

```powershell
Copy-Item .env.example .env
```

Abre `.env` y cambia al menos estos valores:

```env
POSTGRES_PASSWORD=usa_una_contrasena_local_segura
JWT_SECRET=usa_un_secreto_aleatorio_de_al_menos_32_caracteres
```

Mantén las demás variables con sus valores por defecto para una primera ejecución. No uses valores reales ni reutilices secretos de producción.

### 4. Construir e iniciar

```powershell
docker compose up --build -d
docker compose ps
```

En el primer inicio puede tardar varios minutos porque Docker descarga imágenes y construye el frontend y backend. Cuando los servicios indiquen `healthy` o `running`, abre [http://localhost:4200](http://localhost:4200).

Para ver el arranque si algo demora:

```powershell
docker compose logs -f backend
```

Flyway crea y actualiza automáticamente el esquema de PostgreSQL. No ejecutes migraciones SQL manualmente ni modifiques migraciones ya aplicadas.

---

## Configuración

La plantilla completa está en [.env.example](.env.example). Estas son las variables habituales:

| Variable | Propósito | Valor inicial |
|---|---|---|
| `POSTGRES_DB` | Nombre de base de datos | `aulaia_db` |
| `POSTGRES_USER` | Usuario PostgreSQL | `aulaia_user` |
| `POSTGRES_PASSWORD` | Contraseña PostgreSQL | **debes cambiarla** |
| `JWT_SECRET` | Firma de tokens JWT; mínimo 32 caracteres | **debes cambiarla** |
| `SPRING_PROFILES_ACTIVE` | Perfil Spring | `dev` |
| `APP_TIME_ZONE` | Zona horaria del backend Docker | `America/Lima` |
| `APP_DEMO_USERS_ENABLED` | Crea cuentas demo solo en `dev` | `true` |
| `FRONTEND_PORT` | Puerto publicado de Angular/Nginx | `4200` |
| `BACKEND_PORT` | Puerto publicado del backend | `8080` |
| `DATA_SCIENCE_PORT` | Puerto publicado de FastAPI | `8000` |
| `TELEGRAM_ENABLED` | Activa la integración Telegram | `false` |
| `TELEGRAM_BOT_TOKEN` | Token del bot de Telegram | vacío |
| `TELEGRAM_BOT_USERNAME` | Usuario del bot, sin `@` | vacío |
| `GROQ_API_KEY` | Clave opcional para funcionalidades de IA | vacío |

Después de cambiar variables, reinicia los servicios dependientes:

```powershell
docker compose up -d --build backend frontend
```

## Accesos y URLs

| Recurso | Dirección |
|---|---|
| Aplicación web | [http://localhost:4200](http://localhost:4200) |
| API backend | [http://localhost:8080](http://localhost:8080) |
| Swagger UI | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) |
| Salud backend | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) |
| FastAPI | [http://localhost:8000](http://localhost:8000) |
| Documentación FastAPI | [http://localhost:8000/docs](http://localhost:8000/docs) |
| Salud FastAPI | [http://localhost:8000/health](http://localhost:8000/health) |

PostgreSQL no se publica en un puerto del anfitrión en la configuración Docker por defecto. Se mantiene accesible solo dentro de la red `aulaia_network`, lo cual reduce exposición innecesaria.

## Cuentas demo

Con `SPRING_PROFILES_ACTIVE=dev` y `APP_DEMO_USERS_ENABLED=true`, el backend crea estas cuentas **solo si no existen**. No sobrescribe usuarios, roles ni contraseñas existentes.

| Rol | Usuario | Contraseña |
|---|---|---|
| Administrador | `admin` | `Admin12345678!` |
| Docente | `docente@aulaia.com` | `123456` |

Son credenciales de demostración. Desactiva `APP_DEMO_USERS_ENABLED` y usa cuentas seguras antes de cualquier despliegue fuera de desarrollo.

---

## Uso inicial recomendado

1. Inicia sesión con la cuenta `admin`.
2. Crea los grados y sus secciones.
3. Registra cursos, docentes y horarios.
4. Registra estudiantes en su sección.
5. Abre una sesión/clase desde el flujo docente y usa **Modo Aula** para registrar asistencia.
6. Si usarás Telegram, registra o asocia los apoderados del estudiante y realiza una sola vinculación por apoderado.

### Apoderados y varios hijos

Un apoderado representa una persona y puede estar asociado a más de un estudiante. En el detalle del segundo hijo, usa **Asociar apoderado existente** en vez de crear otra ficha de la misma madre, padre o tutor.

Así, si ese apoderado ya vinculó Telegram, recibirá notificaciones de asistencia de todos los estudiantes asociados sin necesitar otra cuenta ni otro QR de Telegram.

---

## Telegram

Telegram es opcional. El sistema inicia normalmente cuando está deshabilitado.

### Habilitarlo

1. Crea un bot mediante [@BotFather](https://t.me/BotFather).
2. Configura en `.env`:

   ```env
   TELEGRAM_ENABLED=true
   TELEGRAM_BOT_TOKEN=token_entregado_por_botfather
   TELEGRAM_BOT_USERNAME=NombreDeTuBot
   ```

3. Reinicia el backend:

   ```powershell
   docker compose up -d --build backend
   ```

4. Desde el detalle del estudiante, entra a **Código QR → Vinculación con Telegram**, selecciona un apoderado y genera el QR.
5. El apoderado abre/escanea el enlace desde su cuenta de Telegram y usa el comando seguro `/start TOKEN` que incluye el enlace.

La vinculación obtiene el `chat_id` exclusivamente desde la actualización recibida por Telegram. Un fallo al enviar un mensaje de Telegram no revierte una asistencia ni una vinculación ya confirmada.

No expongas ni subas el token del bot. Si se filtra, revócalo desde BotFather y genera uno nuevo.

---

## Desarrollo sin Docker

Para trabajar con cada servicio por separado necesitas:

| Herramienta | Versión recomendada |
|---|---|
| Java | 21 |
| Maven | 3.9+ (el proyecto incluye Maven Wrapper) |
| Node.js | 22.x |
| Python | 3.12+ |
| PostgreSQL | 17 |

### Backend

Configura PostgreSQL local y define las variables antes de iniciar. El puerto de ejemplo para desarrollo local es `5433`; usa el puerto real de tu instalación.

```powershell
Set-Location backend
$env:DB_HOST="localhost"
$env:DB_PORT="5433"
$env:DB_NAME="aulaia_db"
$env:DB_USERNAME="aulaia_user"
$env:DB_PASSWORD="TU_PASSWORD_LOCAL"
$env:JWT_SECRET="un_secreto_local_de_al_menos_32_caracteres"
$env:SPRING_PROFILES_ACTIVE="dev"
.\mvnw.cmd spring-boot:run
```

Si tu entorno no puede ejecutar el wrapper, instala Maven 3.9+ y usa:

```powershell
mvn spring-boot:run
```

### Frontend

En otra terminal:

```powershell
Set-Location frontend
npm ci --legacy-peer-deps
npm start
```

La aplicación queda disponible en [http://localhost:4200](http://localhost:4200). En desarrollo el frontend se comunica con `http://localhost:8080/api/v1`.

### FastAPI

En otra terminal:

```powershell
Set-Location data-science
py -3.12 -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

---

## Pruebas

Ejecuta las pruebas antes de abrir un pull request o actualizar una instalación.

```powershell
# Backend
Set-Location backend
.\mvnw.cmd clean test

# Frontend
Set-Location ..\frontend
npm ci --legacy-peer-deps
npm test -- --watch=false --browsers=ChromeHeadless
npm run build

# FastAPI
Set-Location ..\data-science
python -m pytest app/tests/ -v --tb=short
```

El repositorio incluye flujos de GitHub Actions para validación y despliegue. No subas `.env`, tokens, contraseñas, JWT ni archivos de datos reales.

---

## Operación y solución de problemas

### Comandos Docker frecuentes

```powershell
# Estado de los servicios
docker compose ps

# Logs de todos los contenedores
docker compose logs -f

# Logs de un servicio concreto
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f data-science

# Reiniciar sin borrar datos
docker compose restart backend

# Detener contenedores y conservar PostgreSQL
docker compose down

# Eliminar también el volumen de PostgreSQL: operación irreversible local
docker compose down -v
```

### El frontend no abre

1. Ejecuta `docker compose ps` y confirma que `frontend` esté en ejecución.
2. Comprueba que el puerto `4200` no esté ocupado; si lo está, cambia `FRONTEND_PORT` en `.env` y reinicia Compose.
3. Consulta `docker compose logs frontend`.

### El backend no queda saludable

1. Ejecuta `docker compose logs backend`.
2. Comprueba que `POSTGRES_PASSWORD` y `JWT_SECRET` no sigan con los placeholders de ejemplo.
3. Espera a que `postgres` esté saludable; el backend depende de él.
4. No borres el volumen de datos como primer intento. Usa `docker compose down` y vuelve a iniciar; reserva `down -v` para una base local descartable.

### Una asistencia aparece como tardanza en Docker

Verifica que el backend reciba la zona configurada:

```powershell
docker compose exec backend printenv TZ
docker compose exec backend date
```

El valor esperado por defecto es `America/Lima`. Puedes ajustarlo con `APP_TIME_ZONE` en `.env`.

### El QR o Telegram no funcionan

- El QR de asistencia debe usarse dentro de una sesión/clase activa y con un estudiante perteneciente a la sección de esa clase.
- Confirma que el apoderado esté asociado al estudiante y activo.
- Para Telegram, comprueba `TELEGRAM_ENABLED=true`, el token, el username sin `@` y los logs del backend.
- Nunca pegues el token del bot en issues, capturas de pantalla o commits.

---

## Seguridad y privacidad

- Contraseñas almacenadas mediante BCrypt; nunca en texto plano en la base de datos.
- API protegida con JWT y control de roles.
- Migraciones versionadas con Flyway; Hibernate valida el esquema.
- QR de asistencia sin información personal legible.
- Sin reconocimiento facial ni biometría de estudiantes.
- Los secretos se suministran mediante variables de entorno, no mediante código ni migraciones.

Para producción, usa secretos administrados por la plataforma de despliegue, `SPRING_PROFILES_ACTIVE=prod`, HTTPS, contraseñas únicas y cuentas de usuario no demo.

## Documentación del proyecto

La documentación funcional y técnica de referencia está en [`docs/`](docs/):

- [PRD](docs/01-PRD_AulaIA.md)
- [Documento técnico](docs/02-TRD_AulaIA.md)
- [Arquitectura](docs/03-ARQUITECTURA_AulaIA.md)
- [Base de datos](docs/04-BASE_DE_DATOS_AulaIA.md)
- [UI/UX](docs/05-UI_UX_AulaIA.md)
- [Flujos](docs/06-FLUJOS_AulaIA.md)
- [Plan de ejecución](docs/07-PLAN_EJECUCION_AulaIA.md)
- [Dockerización](docs/08-DOCKERIZACION_AulaIA.md)

---

Desarrollado como proyecto académico AulaIA.
