# ============================================================
# Stage 1 — Build
# ============================================================
FROM gradle:9.6.0-jdk21 AS build

WORKDIR /app

# Copy Gradle configuration first
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Resolve dependencies
RUN gradle dependencies --no-daemon

# Copy source
COPY src ./src

# Build application
RUN gradle bootJar -x test --no-daemon


# ============================================================
# Stage 2 — Runtime
# ============================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Run as non-root user
RUN useradd \
    --system \
    --create-home \
    --shell /usr/sbin/nologin \
    swachhbot

COPY --from=build /app/build/libs/*.jar app.jar

RUN chown swachhbot:swachhbot app.jar

USER swachhbot

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]