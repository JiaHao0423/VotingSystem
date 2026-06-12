# ====== Stage 1: 建置前端 ======
FROM node:22-alpine AS frontend-build
WORKDIR /app/frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

# ====== Stage 2: 建置後端 ======
FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /app/backend

COPY backend/pom.xml ./
RUN mvn dependency:go-offline -B -q

COPY backend/src ./src
RUN mvn package -DskipTests -B -q

# ====== Stage 3: 執行環境（nginx + Spring Boot 同容器）======
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache nginx gettext curl \
    && mkdir -p /etc/nginx/http.d /run/nginx /var/log/nginx

# 後端
COPY --from=backend-build /app/backend/target/*.war /app/app.war

# 前端靜態檔
COPY --from=frontend-build /app/frontend/dist /usr/share/nginx/html

# nginx 設定與啟動腳本
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY docker-entrypoint.sh /docker-entrypoint.sh
# 修正 Windows CRLF，避免 Linux 容器無法執行啟動腳本
RUN sed -i 's/\r$//' /docker-entrypoint.sh && chmod +x /docker-entrypoint.sh

# Zeabur 注入 PORT 給 nginx；後端固定在本機 8081
ENV PORT=8080
ENV HOST=0.0.0.0
ENV BACKEND_PORT=8081
EXPOSE 8080

ENTRYPOINT ["/docker-entrypoint.sh"]
