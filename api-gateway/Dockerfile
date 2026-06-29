# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy root pom and install it so child modules can resolve the parent
COPY pom.xml .
RUN mvn -N install -q

# Copy service pom and download dependencies (layer cached separately from source)
COPY api-gateway/pom.xml api-gateway/
RUN mvn -pl api-gateway dependency:go-offline -q

# Copy source and build
COPY api-gateway/src api-gateway/src
RUN mvn -pl api-gateway package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

COPY --from=builder /app/api-gateway/target/api-gateway-*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
