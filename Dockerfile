# Fraud Detection System - Dockerfile
# Pure Java implementation with built-in HttpServer

FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy source files
COPY src/main/java/fraud/ ./src/main/java/fraud/

# Compile Java sources
RUN javac -d target/classes src/main/java/fraud/*.java

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy compiled classes
COPY --from=builder /app/target/classes ./target/classes

# Copy static files
COPY src/main/resources/static ./src/main/resources/static

# Create working directory for static files
RUN mkdir -p src/main/resources/static

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-cp", "target/classes", "fraud.FraudDetectionApplication", "8080"]
