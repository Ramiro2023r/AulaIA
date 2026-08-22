# ============================================
# AulaIA — Gestión de Secretos (Producción)
# ============================================
# Opciones recomendadas (elegir una):
#
# 1. **SOPS + Age** (recomendado para GitOps)
# 2. **HashiCorp Vault** (empresa)
# 3. **Docker Secrets** (solo Swarm)
# 4. **1Password CLI / Bitwarden CLI** (equipos pequeños)
# 5. **GitHub Environments + Encrypted Secrets** (CI/CD)
#
# Este documento usa SOPS + Age como ejemplo.
# ============================================

# ─── SOPS + Age Setup ──────────────────────────────────────────────────

# 1. Instalar age y sops:
#   macOS: brew install age sops
#   Linux: curl -sSL https://github.com/getsops/sops/releases/download/v3.8.1/sops-v3.8.1.linux.amd64 -o /usr/local/bin/sops

# 2. Generar clave Age:
#   age-keygen -o key.txt
#   # Guarda key.txt en lugar seguro (1Password, Bitwarden, Vault)
#   # Exporta la clave pública: age-keygen -y key.txt

# 3. Crear .sops.yaml en la raíz:
#   creation_rules:
#     - path_regex: \.env\.prod$
#       key_groups:
#         - age:
#             - age1xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx  # Clave pública

# 4. Crear archivo de secretos:
#   # .env.prod (texto plano ANTES de cifrar)
#   POSTGRES_PASSWORD=super_secreto_123
#   JWT_SECRET=clave_muy_larga_minimo_32_caracteres_aqui
#   GRAFANA_PASSWORD=grafana_admin_seguro
#   ACME_EMAIL=admin@tudominio.com
#   AULAIA_BOOTSTRAP_ADMIN_PASSWORD=Admin_Seguro_2024!

# 5. Cifrar:
#   sops -e .env.prod > .env.prod.enc

# 6. En CI/CD (GitHub Actions), descifrar:
#   - name: Decrypt secrets
#     run: |
#       echo "${{ secrets.SOPS_KEY }}" > key.txt
#       sops -d .env.prod.enc > .env
#     env:
#       SOPS_AGE_KEY_FILE: key.txt

# 7. Subir .env.prod.enc al repo (NO .env.prod ni key.txt)

# ─── Docker Secrets (Alternativa simple) ──────────────────────────────

# En docker-compose.prod.yml:
# secrets:
#   postgres_password:
#     file: ./secrets/postgres_password.txt
#   jwt_secret:
#     file: ./secrets/jwt_secret.txt
#
# services:
#   postgres:
#     environment:
#       POSTGRES_PASSWORD_FILE: /run/secrets/postgres_password
#     secrets:
#       - postgres_password

# ─── GitHub Environments (Para CD) ───────────────────────────────────

# Settings > Environments > production:
#   - Required reviewers: 1-2 personas
#   - Wait timer: 5 min
#   - Deployment branches: main only
#   - Environment secrets:
#       DEPLOY_HOST
#       DEPLOY_USER
#       DEPLOY_SSH_KEY
#       SOPS_KEY (contenido de key.txt)

# ─── Rotación de Secretos ─────────────────────────────────────────────

# Programar rotación cada 90 días:
#   - JWT_SECRET
#   - POSTGRES_PASSWORD
#   - GRAFANA_PASSWORD
#
# Automatizar con GitHub Actions programado:
#   on:
#     schedule:
#       - cron: '0 0 1 */3 *'  # Cada 3 meses, día 1 a medianoche