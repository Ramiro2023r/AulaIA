# ============================================
# AulaIA Frontend — Angular Dockerfile
# Multi-stage: Node build → Nginx serve
# ============================================

# Stage 1: Build Angular
FROM node:22-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

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