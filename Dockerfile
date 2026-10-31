# ============================================================
# Stage 1: Build with Maven
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy POM first (layer cache for dependencies)
COPY backend/pom.xml ./pom.xml

RUN mvn dependency:go-offline -B

# Copy source and build
COPY backend/src ./src
RUN mvn clean package -DskipTests -B

# ============================================================
# Stage 2: Slim runtime image
# ============================================================
FROM eclipse-temurin:21-jre-jammy AS runtime

LABEL maintainer="VIT Privacy Federated Analytics"
LABEL description="Privacy-Preserving Federated Database Analytics Backend"

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Create non-root user for security
RUN groupadd --gid 1001 fpa && \
    useradd --uid 1001 --gid fpa --shell /bin/bash --create-home fpa

# Copy the built JAR
COPY --from=build /app/target/*.jar app.jar

RUN chown fpa:fpa app.jar
USER fpa

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
