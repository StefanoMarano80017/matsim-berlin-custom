# =========================
# 1) BUILD STAGE
# =========================
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /build

# Copia solo il POM (cache dipendenze)
COPY pom.xml .

# PROFILO DOCKER ATTIVO QUI
RUN mvn -B -Pdocker -DskipTests dependency:go-offline

# Copia il resto del progetto
COPY src ./src

# Build finale
RUN mvn -B -Pdocker package

# =========================
# 2) RUNTIME STAGE
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copia il jar shaded prodotto
COPY --from=build /build/target/*.jar app.jar
COPY ./input ./input

# MATSim ama la RAM → meglio dichiararla
ENV JAVA_OPTS="-Xms2g -Xmx8g -Djava.awt.headless=true"

# Se usi Spring Boot REST
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
