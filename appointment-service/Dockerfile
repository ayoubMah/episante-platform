# --- Stage 1: Build ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# OPTIMIZATION 1: Cache the episante-common dependencies
COPY episante-common/pom.xml /common/pom.xml
RUN mvn -f /common/pom.xml dependency:go-offline -DskipTests

# Now build the common library
COPY episante-common/src /common/src
RUN mvn -f /common/pom.xml clean install -DskipTests

# OPTIMIZATION 2: Cache the appointment-service dependencies
COPY appointment-service/pom.xml .
RUN mvn -q dependency:go-offline -DskipTests

# Now build the appointment service
COPY appointment-service/src ./src
RUN mvn clean package -DskipTests

# --- Stage 2: Runtime ---
# OPTIMIZATION 3: Use Alpine JRE (Saves ~130MB per container)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# OPTIMIZATION 4: Security - Run as non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar
EXPOSE 9093

ENTRYPOINT ["java", "-jar", "app.jar"]