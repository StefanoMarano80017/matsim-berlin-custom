# Stage build
FROM maven:3.8.8-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copio solo il pom per il caching delle dipendenze
COPY pom.xml .

# Ora copio il resto del progetto
COPY src ./src

# Compilo
RUN mvn clean package -DskipTests

# Stage runtime
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]
