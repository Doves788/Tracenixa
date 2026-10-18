# Multi-stage Dockerfile for Tracenixa Backend
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy Maven wrapper & POM
COPY pom.xml .
# Download dependencies
RUN apk add --no-cache maven && mvn dependency:go-offline -B || true

# Copy source code and build production jar
COPY src src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add unprivileged user for security
RUN addgroup -S tracenixa && adduser -S tracenixa -G tracenixa

COPY --from=builder /workspace/target/tracenixa-platform-*.jar app.jar
RUN chown -R tracenixa:tracenixa /app

USER tracenixa
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --retries=5 --start-period=30s \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
