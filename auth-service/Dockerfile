# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy root pom and install it so child modules can resolve the parent
COPY pom.xml .
RUN mvn -N install -q

# Copy service pom and download dependencies (layer cached separately from source)
COPY auth-service/pom.xml auth-service/
RUN mvn -pl auth-service dependency:go-offline -q

# Copy source and build
COPY auth-service/src auth-service/src
RUN mvn -pl auth-service package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

COPY --from=builder /app/auth-service/target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8081

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
