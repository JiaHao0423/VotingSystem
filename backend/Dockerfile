# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn package -DskipTests -B -q

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY --from=build /app/target/*.war app.war
RUN chown app:app app.war

USER app

# Zeabur 會注入 PORT；本機 docker-compose 預設 8081
ENV PORT=8081
EXPOSE 8081

ENTRYPOINT ["sh", "-c", "exec java -jar app.war --server.port=${PORT}"]
