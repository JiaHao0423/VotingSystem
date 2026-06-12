# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 先複製 pom.xml 下載依賴，讓 Docker layer cache 加速後續建置
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn package -DskipTests -B -q

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# 專案打包為 Spring Boot 可執行 WAR
COPY --from=build /app/target/*.war app.war
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.war"]
