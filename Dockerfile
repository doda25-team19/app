# Build stage for the Spring Boot frontend
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Copy frontend Maven project
COPY frontend/pom.xml frontend/pom.xml
COPY frontend/src frontend/src

# Build the jar
RUN mvn -f frontend/pom.xml -q package -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copy built jar from build stage
COPY --from=build /app/frontend/target/*.jar app.jar

# Environment variables
# APP_PORT controls the Spring Boot port, default 8080
ENV APP_PORT=8080
ENV SERVER_PORT=${APP_PORT}

# MODEL_HOST tells the app where the Python backend runs
ENV MODEL_HOST=http://model-service:8081

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
