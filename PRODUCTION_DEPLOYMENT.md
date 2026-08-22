# AulaIA — Guía de Despliegue en Producción

## 📋 Checklist Pre-Despliegue

- [ ] Dominio configurado (DNS A/AAAA → IP servidor)
- [ ] Certificados SSL (Let's Encrypt via Traefik - automático)
- [ ] Variables de entorno en `.env` (ver `.env.example`)
- [ ] Secretos gestionados (ver `SECRETS_MANAGEMENT.md`)
- [ ] Backups PostgreSQL configurados (cron 2 AM daily)
- [ ] Monitoreo: Prometheus + Grafana + Loki accesibles
- [ ] Firewall: Solo puertos 80, 443, 22 (SSH) expuestos
- [ ] Usuario no-root en contenedores ✓
- [ ] Health checks en todos los servicios ✓

---

## 🚀 Despliegue Inicial

```bash
# 1. Clonar repo
git clone https://github.com/tu-org/aulaia.git
cd aulaia

# 2. Configurar variables
cp .env.example .env
# Editar .env con valores REALES (JWT_SECRET min 32 chars, passwords fuertes)

# 3. Crear red Docker (si no existe)
docker network create aulaia-network

# 4. Levantar stack
docker compose -f docker-compose.prod.yml up -d --build

# 5. Verificar
docker compose -f docker-compose.prod.yml ps
# Todos deben estar "healthy" o "running"

# 6. Verificar endpoints
curl -f https://tudominio.com/           # Frontend
curl -f https://api.tudominio.com/actuator/health  # Backend
curl -f https://api.tudominio.com/api/v1/analisis/health  # FastAPI
curl -f https://grafana.tudominio.com/   # Grafana
curl -f https://traefik.tudominio.com/   # Traefik Dashboard
```

---

## 🔐 SSL/TLS (Let's Encrypt Automático)

Traefik gestiona certificados automáticamente:

- **Entrada HTTP (80)** → Redirige a HTTPS
- **Entrada HTTPS (443)** → Termina TLS
- **Certificados** → Almacenados en volumen `traefik_letsencrypt`
- **Renovación** → Automática (cada 60 días)

```bash
# Ver logs de ACME
docker compose -f docker-compose.prod.yml logs traefik | grep -i acme

# Forzar renovación (solo si hay problemas)
docker compose -f docker-compose.prod.yml restart traefik
```

---

## 📊 Monitoreo

| Servicio | URL | Credenciales |
|----------|-----|--------------|
| **Grafana** | https://grafana.tudominio.com | `GRAFANA_USER` / `GRAFANA_PASSWORD` |
| **Prometheus** | https://api.tudominio.com/actuator/prometheus | (interno) |
| **Traefik Dashboard** | https://traefik.tudominio.com | (interno, solo red) |
| **Loki (Logs)** | http://loki:3100 | (interno) |

### Dashboards Incluidos

- **AulaIA Overview** (`aulaia-overview.json`): Requests, latencia, JVM, asistencia, salud servicios

### Métricas Clave (Alertas Recomendadas)

```promql
# Backend caído
up{job="spring-boot"} == 0

# Latencia p95 > 2s
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 2

# Memoria JVM > 85%
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.85

# Postgres conexiones > 80%
pg_stat_database_numbackends / pg_settings_max_connections > 0.8

# Espacio disco < 10%
(node_filesystem_avail_bytes / node_filesystem_size_bytes) < 0.1
```

---

## 💾 Backups PostgreSQL

### Automáticos (Cron en contenedor `backup`)

- **Horario**: 2:00 AM daily
- **Retención**: 30 días
- **Ubicación**: `/backup/aulaia_YYYYMMDD.sql.gz`

```bash
# Ver backups
ls -la backup/

# Restaurar manual
gunzip -c backup/aulaia_20260820.sql.gz | docker exec -i aulaia-postgres psql -U aulaia_user -d aulaia_db

# Backup manual inmediato
docker exec aulaia-backup pg_dump -U aulaia_user aulaia_db | gzip > backup/aulaia_manual_$(date +%Y%m%d).sql.gz
```

### Off-site (Recomendado)

```bash
# Sync a S3/Wasabi/Backblaze B2
aws s3 sync backup/ s3://tu-bucket/aulaia-backups/ --storage-class GLACIER
```

---

## 📝 Logs Centralizados (Loki + Promtail)

```bash
# Consultar logs en Grafana (Explore > Loki)
# Queries útiles:
{job="spring-boot"} |= "ERROR"
{job="nginx"} |= " 500 "
{service="backend"} |~ "asistencia.*registrada"
{job="traefik"} |= "acme"
```

### Retención

- **Loki**: 30 días (configurado en `loki-config.yaml`)
- **Promtail**: Shipper sidecar, no almacena localmente

---

## 🔧 Mantenimiento Común

### Actualizar Imágenes

```bash
# Pull nuevas versiones
docker compose -f docker-compose.prod.yml pull

# Rebuild y restart sin downtime (rolling)
docker compose -f docker-compose.prod.yml up -d --build --remove-orphans

# Verificar
docker compose -f docker-compose.prod.yml ps
curl -f https://tudominio.com/
```

### Escalar Backend (si hay carga alta)

```yaml
# En docker-compose.prod.yml, bajo backend:
deploy:
  replicas: 3
  resources:
    limits:
      memory: 2G
```

```bash
docker compose -f docker-compose.prod.yml up -d --scale backend=3
```

### Ver Logs en Tiempo Real

```bash
# Todos los servicios
docker compose -f docker-compose.prod.yml logs -f --tail=100

# Solo backend
docker compose -f docker-compose.prod.yml logs -f backend

# Filtrar errores
docker compose -f docker-compose.prod.yml logs backend | grep -i error
```

---

## 🚨 Troubleshooting

| Problema | Diagnóstico | Solución |
|----------|-------------|----------|
| **502 Bad Gateway** | Backend no healthy | `docker logs aulaia-backend` → revisar BD, migraciones Flyway |
| **Certificado SSL inválido** | Traefik no obtuvo cert | Verificar DNS, puerto 80/443 abiertos, email ACME válido |
| **FastAPI 503** | IA no disponible | `docker logs aulaia-fastapi` → revisar memoria, dependencias |
| **Postgres no arranca** | Volumen corrupto / password | `docker volume rm aulaia_postgres_data` (¡pierde datos!) |
| **Frontend 404 en refresh** | Nginx config SPA | Verificar `try_files $uri $uri/ /index.html;` en `nginx.conf` |
| **Memoria alta** | JVM heap / leaks | Revisar `jvm_memory_used_bytes` en Grafana, reiniciar backend |

---

## 📞 Contactos de Emergencia

| Rol | Nombre | Contacto |
|-----|--------|----------|
| **DevOps Lead** | | |
| **Backend Lead** | | |
| **Frontend Lead** | | |
| **DBA** | | |
| **Infraestructura** | | |

---

## 📚 Referencias

- [Traefik Let's Encrypt](https://doc.traefik.io/traefik/https/acme/)
- [Prometheus Alerting](https://prometheus.io/docs/alerting/latest/)
- [Grafana Loki](https://grafana.com/docs/loki/latest/)
- [Docker Compose Production](https://docs.docker.com/compose/production/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)