# Build stage
FROM maven:3.9.2-eclipse-temurin-21 as builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
COPY --from=builder /app/target/matsim-berlin-6.4.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
