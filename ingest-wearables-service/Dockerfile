# --- Stage 1: Build ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY episante-common/pom.xml /common/pom.xml
RUN mvn -f /common/pom.xml dependency:go-offline -DskipTests

COPY episante-common/src /common/src
RUN mvn -f /common/pom.xml clean install -DskipTests

COPY ingest-wearables-service/pom.xml .
RUN mvn -q dependency:go-offline -DskipTests

COPY ingest-wearables-service/src ./src
RUN mvn clean package -DskipTests

# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar
EXPOSE 9096

ENTRYPOINT ["java", "-jar", "app.jar"]
