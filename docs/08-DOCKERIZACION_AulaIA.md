# Dockerización — AulaIA

## 1. Información del documento

- **Proyecto:** AulaIA — Sistema Inteligente de Asistencia Escolar
- **Documento:** Guía de Dockerización
- **Versión:** 1.0
- **Fecha:** 17 de agosto de 2026
- **Objetivo:** Definir cómo ejecutar AulaIA mediante Docker y Docker Compose para desarrollo, pruebas, demostración y preparación de producción.
- **Servicios incluidos:** Angular, Spring Boot, PostgreSQL y FastAPI.

---

# 2. Objetivo de Docker en AulaIA

Docker permitirá ejecutar todo el sistema con una configuración reproducible.

En lugar de instalar y configurar manualmente PostgreSQL, Java, Spring Boot, Python, FastAPI, Node y Angular, cada componente podrá ejecutarse en su propio contenedor.

```text
┌─────────────────────┐
│      Frontend       │
│       Angular       │
│       :4200         │
└──────────┬──────────┘
           │ HTTP
           ▼
┌─────────────────────┐
│       Backend       │
│     Spring Boot     │
│       :8080         │
└───────┬─────────────┘
        │
        ├──────────────────────┐
        ▼                      ▼
┌───────────────┐      ┌────────────────┐
│  PostgreSQL   │      │    FastAPI     │
│    :5432      │      │     :8000      │
└───────────────┘      └────────────────┘
```

---

# 3. Beneficios para el proyecto

Docker aporta:

- Entorno reproducible.
- Menos problemas de versiones.
- Fácil instalación en otra laptop.
- Facilidad para la exposición.
- Base para CI/CD.
- Preparación para despliegue.
- Aislamiento entre servicios.
- Menor dependencia del entorno local.

Para la demostración, el objetivo será poder ejecutar:

```bash
docker compose up -d
```

y levantar todo AulaIA.

---

# 4. Estructura esperada del repositorio

```text
aulaia/
│
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   └── ...
│
├── backend/
│   ├── Dockerfile
│   └── ...
│
├── data-science/
│   ├── Dockerfile
│   ├── requirements.txt
│   └── ...
│
├── docker/
│   └── postgres/
│
├── docs/
│
├── .env.example
├── .dockerignore
├── docker-compose.yml
└── README.md
```

---

# 5. Requisitos locales

Instalar:

```text
Docker Desktop
Docker Compose
Git
```

En Windows, Docker Desktop deberá estar ejecutándose.

Verificar:

```bash
docker --version
docker compose version
```

---

# 6. Variables de entorno

Crear:

```text
.env
```

No subirlo al repositorio.

Crear también:

```text
.env.example
```

Ejemplo:

```env
POSTGRES_DB=aulaia_db
POSTGRES_USER=aulaia_user
POSTGRES_PASSWORD=change_me

BACKEND_PORT=8080
FRONTEND_PORT=4200
FASTAPI_PORT=8000

JWT_SECRET=change_this_for_real_environment

SPRING_PROFILES_ACTIVE=dev

IA_SERVICE_URL=http://fastapi:8000
```

---

# 7. `.gitignore`

Debe contener:

```gitignore
.env

frontend/node_modules/
frontend/dist/

backend/target/

data-science/.venv/
data-science/__pycache__/
data-science/.pytest_cache/

.idea/
.vscode/

*.log
```

---

# 8. `.dockerignore` backend

Crear:

```text
backend/.dockerignore
```

Contenido:

```text
target
.git
.idea
.vscode
*.log
```

---

# 9. Dockerfile del backend Spring Boot

Ruta:

```text
backend/Dockerfile
```

Versión recomendada:

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

# 10. Consideración sobre tests del backend

Para desarrollo local:

```bash
mvn test
```

Para CI:

```bash
mvn clean verify
```

El Dockerfile puede usar `-DskipTests` únicamente porque los tests deberán ejecutarse antes en CI.

---

# 11. Configuración Spring Boot para Docker

La conexión no debe apuntar a `localhost` porque PostgreSQL estará en otro contenedor.

Dentro de Docker, `postgres` será el hostname.

Ejemplo:

```properties
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:aulaia_db}
spring.datasource.username=${DB_USERNAME:aulaia_user}
spring.datasource.password=${DB_PASSWORD:aulaia_password}
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
server.port=${PORT:8080}
aulaia.ia.url=${IA_SERVICE_URL:http://localhost:8000}
```

---

# 12. Dockerfile FastAPI

Ruta:

```text
data-science/Dockerfile
```

Ejemplo:

```dockerfile
FROM python:3.12-slim

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

---

# 13. `requirements.txt`

Ejemplo:

```text
fastapi
uvicorn[standard]
pandas
numpy
scikit-learn
pydantic
pytest
httpx
```

Las versiones deberán fijarse cuando el proyecto se estabilice.

---

# 14. Endpoint de salud FastAPI

Debe existir:

```http
GET /health
```

Respuesta:

```json
{
  "status": "ok"
}
```

---

# 15. Dockerfile Angular — desarrollo

Durante desarrollo puede utilizarse Node:

```dockerfile
FROM node:22-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
EXPOSE 4200
CMD ["npm", "start", "--", "--host", "0.0.0.0"]
```

Para producción se recomienda construir Angular y servirlo mediante Nginx.

---

# 16. Dockerfile Angular — producción

Ruta:

```text
frontend/Dockerfile
```

Ejemplo:

```dockerfile
FROM node:22-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/ /usr/share/nginx/html/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

La ruta exacta dentro de `dist/` dependerá del nombre configurado en Angular. La IA deberá comprobar `angular.json` antes de fijarla.

---

# 17. Configuración Nginx para SPA

Ruta:

```text
frontend/nginx.conf
```

Ejemplo:

```nginx
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

---

# 18. PostgreSQL

Se utilizará la imagen oficial:

```text
postgres:17-alpine
```

Los datos deberán persistir mediante un volumen Docker.

---

# 19. `docker-compose.yml`

Archivo raíz:

```text
docker-compose.yml
```

Configuración base:

```yaml
services:
  postgres:
    image: postgres:17-alpine
    container_name: aulaia-postgres
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - aulaia_postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - aulaia-network

  fastapi:
    build:
      context: ./data-science
      dockerfile: Dockerfile
    container_name: aulaia-fastapi
    ports:
      - "${FASTAPI_PORT:-8000}:8000"
    environment:
      ENVIRONMENT: docker
    healthcheck:
      test: ["CMD", "python", "-c", "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - aulaia-network

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: aulaia-backend
    depends_on:
      postgres:
        condition: service_healthy
    ports:
      - "${BACKEND_PORT:-8080}:8080"
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: ${POSTGRES_DB}
      DB_USERNAME: ${POSTGRES_USER}
      DB_PASSWORD: ${POSTGRES_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      IA_SERVICE_URL: http://fastapi:8000
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
    networks:
      - aulaia-network

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: aulaia-frontend
    depends_on:
      - backend
    ports:
      - "${FRONTEND_PORT:-4200}:80"
    networks:
      - aulaia-network

volumes:
  aulaia_postgres_data:

networks:
  aulaia-network:
    driver: bridge
```

---

# 20. Comunicación entre contenedores

Dentro de Docker:

```text
backend → postgres:5432
backend → fastapi:8000
```

Nunca usar `localhost` para comunicar contenedores entre sí.

---

# 21. Comunicación del frontend

El navegador del usuario no puede resolver el hostname `backend` de la red Docker.

Para desarrollo:

```text
http://localhost:8080
```

Para producción:

```text
https://api.aulaia...
```

Angular debe utilizar configuración por environment.

---

# 22. Angular environment local

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1'
};
```

Producción:

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://aulaia-api.onrender.com/api/v1'
};
```

No escribir secretos en Angular.

---

# 23. CORS

Spring Boot deberá permitir el frontend configurado.

Desarrollo:

```text
http://localhost:4200
```

Producción:

```text
https://aulaia.vercel.app
```

No utilizar `*` indiscriminadamente en producción.

---

# 24. Comandos principales

Construir y levantar:

```bash
docker compose up -d --build
```

Ver estado:

```bash
docker compose ps
```

Ver logs:

```bash
docker compose logs -f
```

Backend:

```bash
docker compose logs -f backend
```

FastAPI:

```bash
docker compose logs -f fastapi
```

PostgreSQL:

```bash
docker compose logs -f postgres
```

Frontend:

```bash
docker compose logs -f frontend
```

Detener:

```bash
docker compose down
```

Eliminar también datos locales:

```bash
docker compose down -v
```

Este último comando elimina el volumen PostgreSQL y solo debe ejecutarse intencionalmente.

---

# 25. Accesos locales

Frontend:

```text
http://localhost:4200
```

Backend:

```text
http://localhost:8080
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

FastAPI:

```text
http://localhost:8000
```

Health FastAPI:

```text
http://localhost:8000/health
```

PostgreSQL:

```text
localhost:5432
```

---

# 26. Migraciones Flyway

Al iniciar backend:

```text
Spring Boot
   ↓
Flyway
   ↓
PostgreSQL
   ↓
Migraciones
   ↓
Hibernate validate
```

No utilizar `ddl-auto=update` en la arquitectura definitiva.

---

# 27. FastAPI no debe bloquear AulaIA

La arquitectura de AulaIA establece que la IA es complementaria.

Por tanto, Spring Boot debe poder registrar asistencia incluso si FastAPI está detenido.

Flujo esperado:

```text
FastAPI caído
   ↓
Registro de asistencia sigue funcionando
   ↓
Módulo IA muestra:
"Servicio de análisis no disponible temporalmente"
```

Se recomienda que `backend` dependa estrictamente de PostgreSQL, pero no de FastAPI para poder arrancar.

---

# 28. Healthcheck backend

Agregar Spring Boot Actuator cuando corresponda.

Endpoint recomendado:

```http
GET /actuator/health
```

No exponer endpoints administrativos innecesarios.

---

# 29. Docker Compose para demo

Para la exposición puede existir posteriormente:

```text
docker-compose.demo.yml
```

Con:

- PostgreSQL local.
- FastAPI local.
- Spring Boot local.
- Angular local.
- Datos ficticios.

Así se reduce la dependencia de Internet durante la presentación.

---

# 30. Flujo de exposición con Docker

Antes de presentar:

```bash
docker compose up -d --build
```

Comprobar:

```bash
docker compose ps
```

Abrir:

```text
http://localhost:4200
```

Validar:

```text
Login
Cámara
QR
Voz
Dashboard
IA
```

---

# 31. QR y cámara dentro de Docker

Docker no necesita acceso directo a la webcam.

La cámara la utiliza el navegador:

```text
Navegador
   ↓
Angular
   ↓
navigator.mediaDevices
```

El permiso de cámara se concede al navegador.

---

# 32. Voz dentro de Docker

La Web Speech API también se ejecuta en el navegador.

No se necesita acceso de audio desde los contenedores.

---

# 33. HTTPS y cámara

En producción, la cámara debe utilizarse desde un contexto seguro HTTPS.

`localhost` funciona normalmente para desarrollo.

Producción:

```text
HTTPS obligatorio
```

---

# 34. Persistencia PostgreSQL

Volumen:

```yaml
volumes:
  aulaia_postgres_data:
```

La información debe conservarse aunque se recree el contenedor.

Prueba:

```bash
docker compose down
docker compose up -d
```

Los estudiantes y asistencias deben seguir existiendo.

---

# 35. Backup local

Ejemplo:

```bash
docker exec aulaia-postgres pg_dump -U aulaia_user aulaia_db > aulaia_backup.sql
```

Restauración:

```bash
cat aulaia_backup.sql | docker exec -i aulaia-postgres psql -U aulaia_user -d aulaia_db
```

Adaptar usuario y base a `.env`.

---

# 36. Troubleshooting

## Puerto 5432 ocupado

Cambiar puerto host:

```yaml
ports:
  - "5433:5432"
```

Dentro de Docker PostgreSQL sigue escuchando en `5432`.

## Backend no conecta a PostgreSQL

Comprobar:

```text
DB_HOST=postgres
```

No `localhost`.

## Backend no conecta a FastAPI

Usar:

```text
http://fastapi:8000
```

## Angular no conecta al backend

Desde el navegador usar:

```text
http://localhost:8080
```

No `http://backend:8080`.

## CORS

Revisar configuración de Spring Security y orígenes permitidos. No desactivar seguridad globalmente.

## Cámara no funciona

Revisar permiso del navegador, contexto HTTPS/localhost, dispositivo seleccionado y `navigator.mediaDevices`.

## Voz no funciona

Revisar `window.speechSynthesis`, volumen, navegador y configuración de idioma.

---

# 37. Seguridad de imágenes y secretos

- Usar imágenes oficiales.
- Evitar copiar `.env` a imágenes.
- No guardar passwords en Dockerfile.
- Utilizar versiones controladas.
- Mantener imágenes pequeñas mediante multi-stage builds.
- No registrar JWT completos en logs.

---

# 38. Validación final de Docker

```text
[ ] docker compose build funciona
[ ] postgres inicia
[ ] migraciones aplican
[ ] backend inicia
[ ] FastAPI inicia
[ ] Angular carga
[ ] login funciona
[ ] QR funciona
[ ] código manual funciona
[ ] voz funciona
[ ] dashboard funciona
[ ] backend llama FastAPI
[ ] si FastAPI cae, asistencia sigue funcionando
[ ] reiniciar contenedores conserva PostgreSQL
[ ] no existen secretos en imágenes/repositorio
```

---

# 39. Prompt para IA — Docker backend

```text
Estás trabajando en AulaIA.

Lee primero:
- 01-PRD_AulaIA.docx
- 02-TRD_AulaIA.docx
- 03-ARQUITECTURA_AulaIA.md
- 04-BASE_DE_DATOS_AulaIA.md
- 05-UI_UX_AulaIA.md
- 06-FLUJOS_AulaIA.md
- 07-PLAN_EJECUCION_AulaIA.md
- 08-DOCKERIZACION_AulaIA.md

TAREA:
Implementa únicamente la dockerización del backend Spring Boot.

Debes:
1. revisar pom.xml,
2. comprobar versión Java,
3. crear backend/.dockerignore,
4. crear Dockerfile multi-stage,
5. construir imagen,
6. ejecutar container,
7. validar endpoint de salud o arranque,
8. ejecutar pruebas backend fuera del build Docker,
9. documentar cualquier ajuste.

No trabajes todavía frontend, PostgreSQL ni FastAPI.
No commit.
No push.
No deploy.

Al finalizar informa:
- archivos creados,
- comandos ejecutados,
- resultado del build,
- resultado de tests,
- problemas encontrados.
```

---

# 40. Prompt para IA — Docker PostgreSQL

```text
Implementa únicamente PostgreSQL dentro de Docker Compose para AulaIA.

Debes:
1. crear servicio postgres,
2. utilizar postgres:17-alpine,
3. usar variables .env,
4. crear volumen persistente,
5. agregar healthcheck,
6. crear .env.example,
7. asegurar que .env esté ignorado,
8. levantar PostgreSQL,
9. comprobar conexión.

No borres bases locales existentes.
No commit.
No push.
```

---

# 41. Prompt para IA — Integrar backend + PostgreSQL

```text
Integra únicamente backend Spring Boot con PostgreSQL Docker.

Debes:
1. usar DB_HOST=postgres,
2. mantener variables externas,
3. ejecutar Flyway,
4. usar ddl-auto=validate,
5. validar arranque,
6. validar migraciones,
7. ejecutar tests afectados.

No agregues FastAPI ni frontend todavía.
No commit.
No push.
No deploy.
```

---

# 42. Prompt para IA — Docker FastAPI

```text
Dockeriza únicamente el servicio FastAPI de AulaIA.

Debes:
1. revisar estructura Python,
2. crear Dockerfile,
3. crear .dockerignore,
4. instalar requirements,
5. exponer 8000,
6. validar /health,
7. agregar servicio fastapi al compose,
8. agregar healthcheck,
9. ejecutar pytest,
10. no modificar funcionalidad IA.

No commit.
No push.
```

---

# 43. Prompt para IA — Integrar Spring Boot + FastAPI

```text
Integra únicamente Spring Boot Docker con FastAPI Docker.

Debe utilizar:
IA_SERVICE_URL=http://fastapi:8000

Verifica:
1. comunicación entre servicios,
2. timeouts,
3. fallback,
4. backend sigue funcionando si FastAPI se detiene,
5. pruebas de integración.

No trabajes frontend todavía.
No commit.
No push.
```

---

# 44. Prompt para IA — Docker Angular

```text
Dockeriza únicamente Angular.

Debes:
1. revisar angular.json,
2. identificar ruta real de dist,
3. crear Dockerfile multi-stage,
4. crear nginx.conf,
5. soportar Angular Router,
6. validar build production,
7. levantar container,
8. comprobar pantalla inicial.

No cambiar diseño UI.
No commit.
No push.
```

---

# 45. Prompt para IA — Integración completa

```text
Integra los cuatro servicios de AulaIA:

postgres
backend
fastapi
frontend

No agregues funcionalidades nuevas.

Valida:
- redes,
- puertos,
- variables,
- healthchecks,
- persistencia,
- CORS,
- API URL,
- migraciones,
- logs.

Ejecuta flujo mínimo:
1. login,
2. crear/listar estudiante,
3. registrar asistencia manual,
4. consultar dashboard,
5. health FastAPI.

Entrega resultado.
No commit.
No push.
No deploy.
```

---

# 46. Prompt para IA — Validación de demo

```text
Prepara y valida el entorno Docker de AulaIA para una demostración escolar.

Comprueba:
1. docker compose up -d funciona,
2. todos los servicios arrancan,
3. datos persisten,
4. login funciona,
5. cámara funciona desde navegador,
6. QR funciona,
7. voz funciona,
8. asistencia funciona,
9. dashboard funciona,
10. IA funciona,
11. si IA cae, asistencia continúa.

Usa exclusivamente datos ficticios.
Entrega un checklist de demo.
No deploy.
```

---

# 47. Definition of Done Docker

Docker se considera terminado cuando:

- Todos los servicios tienen Dockerfile apropiado.
- `docker compose up -d --build` funciona.
- PostgreSQL tiene persistencia.
- Flyway ejecuta correctamente.
- Angular carga.
- Spring Boot responde.
- FastAPI responde.
- Los contenedores se comunican.
- La cámara funciona desde el navegador.
- La voz funciona.
- Los secretos están fuera del repositorio.
- Las pruebas pasan.
- La caída de FastAPI no rompe la asistencia.
- Existe documentación para levantar y detener todo.

---

# 48. Resultado esperado

Al finalizar:

```text
git clone AulaIA
       ↓
crear .env
       ↓
docker compose up -d --build
       ↓
abrir navegador
       ↓
http://localhost:4200
       ↓
AulaIA funcionando
```

Para una exposición, esta arquitectura permitirá ejecutar gran parte del sistema de forma local, reduciendo la dependencia de servicios externos y facilitando la demostración.

Docker no sustituye el despliegue en producción, pero proporciona una base sólida y reproducible para desarrollo, pruebas, demo y futuras migraciones a infraestructura real.
