# ---- Build stage ----
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---- Runtime stage ----
FROM nginx:alpine

RUN apk add --no-cache gettext

COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

COPY --from=build /app/dist /usr/share/nginx/html

# Zeabur 會注入 PORT；本機 docker-compose 預設 80
ENV PORT=80
# 後端服務網址（Zeabur 部署時改為後端服務的公開或內部 URL）
ENV BACKEND_URL=http://backend:8081

EXPOSE 80

ENTRYPOINT ["/docker-entrypoint.sh"]
