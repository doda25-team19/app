# --- Stage 1: Build ---
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Copy pom.xml and download dependencies first to leverage Docker layer caching
COPY frontend/pom.xml frontend/pom.xml
RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    mvn -f frontend/pom.xml dependency:go-offline

# Copy source code and build the application
COPY frontend/src frontend/src
RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    mvn -f frontend/pom.xml -q package -DskipTests

# --- Stage 2: Runtime ---
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copy the compiled Jar from the build stage
COPY --from=build /app/frontend/target/*.jar app.jar

# Install necessary utilities and clean up the apt cache to reduce image size
RUN apt-get update && apt-get install -y curl \
    && rm -rf /var/lib/apt/lists/*

# Configuration
ENV APP_PORT=8080
ENV SERVER_PORT=${APP_PORT}
ENV MODEL_HOST=http://model-service:8081

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=$APP_PORT -jar app.jar"]