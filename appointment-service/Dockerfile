# ----------------------
# 1. Build Stage
# ----------------------
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy only pom first for caching
COPY pom.xml .

# Download dependencies before copying source (build cache optimization)
RUN mvn -q -e -DskipTests dependency:go-offline

# Now copy the source
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

# Expose service port
EXPOSE 9093

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
