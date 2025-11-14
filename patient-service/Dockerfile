# ----------------------
# 1. Build Stage
# ----------------------
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy everything
COPY pom.xml .
COPY src ./src

# Build the jar
RUN mvn clean package -DskipTests

# ----------------------
# 2. Runtime Stage
# ----------------------
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy built jar
COPY --from=build /app/target/*.jar app.jar

# Expose port (same as server.port in YAML)
EXPOSE 9091

# Run
ENTRYPOINT ["java", "-jar", "app.jar"]
