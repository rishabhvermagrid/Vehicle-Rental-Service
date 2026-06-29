# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy root pom and install it so child modules can resolve the parent
COPY pom.xml .
RUN mvn -N install -q

# Copy service pom and download dependencies (layer cached separately from source)
COPY vehicle-service/pom.xml vehicle-service/
RUN mvn -pl vehicle-service dependency:go-offline -q

# Copy source and build
COPY vehicle-service/src vehicle-service/src
RUN mvn -pl vehicle-service package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

COPY --from=builder /app/vehicle-service/target/vehicle-service-*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
