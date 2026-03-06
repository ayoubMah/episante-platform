# --- Stage 1: Build ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Cache and build the common library FIRST
COPY episante-common/pom.xml /common/pom.xml
RUN mvn -f /common/pom.xml dependency:go-offline -DskipTests

COPY episante-common/src /common/src
RUN mvn -f /common/pom.xml clean install -DskipTests

# Cache and build the doctor service
COPY doctor-service/pom.xml .
RUN mvn -q dependency:go-offline -DskipTests

COPY doctor-service/src ./src
RUN mvn clean package -DskipTests

# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Security: Run as non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar
EXPOSE 9092

ENTRYPOINT ["java", "-jar", "app.jar"]