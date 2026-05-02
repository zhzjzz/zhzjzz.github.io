# Multi-stage Dockerfile for Travel System Backend
# Stage 1: Build with Maven
# Stage 2: Run with slim JDK

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first (layer caching)
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Create directory for H2 dev database
RUN mkdir -p /app/data/dev-db
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-jar", "app.jar"]
