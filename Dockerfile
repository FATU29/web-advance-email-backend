# Multi-stage build for smaller image size
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY --chmod=755 mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/awad-email-0.0.1-SNAPSHOT.jar app.jar

# Expose port (Render will use PORT environment variable)
EXPOSE 8080

# Run the application
# Use shell form to allow environment variable substitution
CMD java -Dserver.port=${PORT:-8080} -jar app.jar

