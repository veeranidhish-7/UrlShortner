# Multi-stage build for Spring Boot
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy dependency definition & maven wrapper
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B || true

# Copy source code and package
COPY src src
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

COPY --from=builder /app/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
