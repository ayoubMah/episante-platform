FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# 1. Install episante-common FIRST
COPY episante-common /common
RUN mvn -f /common/pom.xml clean install -DskipTests

# 2. Now copy appointment-service pom and download deps
COPY appointment-service/pom.xml .
RUN mvn -q dependency:go-offline -DskipTests

# 3. Copy source code
COPY appointment-service/src ./src

# 4. Build
RUN mvn clean package -DskipTests

# ----------------------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 9093
ENTRYPOINT ["java", "-jar", "app.jar"]
