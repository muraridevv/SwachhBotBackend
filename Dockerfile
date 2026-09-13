# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy gradle wrapper and config
COPY gradlew .
COPY gradlew.bat .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Ensure gradlew is executable and fix Windows line endings
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Pre-download dependencies (layer caching)
# This will fail build if offline, but helps speed up subsequent builds
RUN ./gradlew build -x test --no-daemon || true

# Copy source and build
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar from stage 1
COPY --from=build /app/build/libs/*.jar app.jar

# Expose app port
EXPOSE 8080

# Run with container-specific overrides
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.datasource.url=jdbc:postgresql://postgres:5432/swachhbot"]
