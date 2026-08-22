# ============================================
# AulaIA Backend — Spring Boot Dockerfile
# Multi-stage build: Maven build → JRE runtime
# ============================================

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar pom.xml y descargar dependencias (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Build sin tests (tests corren en CI aparte)
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiar JAR del build
COPY --from=build /app/target/*.jar app.jar

# Usuario no-root para seguridad
RUN addgroup --system --gid 1001 aulaia && \
    adduser --system --uid 1001 --gid 1001 aulaia && \
    chown aulaia:aulaia app.jar

USER aulaia

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]