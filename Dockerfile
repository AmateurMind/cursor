# See https://docs.docker.com/engine/reference/builder/
# Multi-stage build: compile with Maven, run on JRE

FROM maven:3.9.7-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/expenses-tracker-0.0.1-SNAPSHOT.jar /app/app.jar
# Spring Boot default from application.properties
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]

# --- Dev stage with hot reload ---
FROM maven:3.9.7-eclipse-temurin-17 AS dev
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends inotify-tools && rm -rf /var/lib/apt/lists/*
# Warm dependency cache
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
# src is bind-mounted in docker-compose.dev.yml
EXPOSE 8081
CMD bash -lc "(inotifywait -r -m -e modify,create,delete src | while read; do mvn -q -DskipTests -o compile; done) & mvn -q -DskipTests -o spring-boot:run"
