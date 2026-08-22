# ============================================
# AulaIA Frontend — Angular Dockerfile
# Multi-stage: Node build → Nginx serve
# ============================================

# Stage 1: Build Angular
FROM node:22-alpine AS build

WORKDIR /app

COPY package*.json ./
# ng2-charts@6 declara @angular/cdk como peer opcional no fijado por este
# proyecto; se conserva el lockfile actual y su instalación compatible.
RUN npm ci --legacy-peer-deps

COPY . .
RUN npm run build

# Stage 2: Nginx
FROM nginx:alpine

# Copiar configuración nginx para SPA
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copiar build de Angular (verificar angular.json para output path)
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
