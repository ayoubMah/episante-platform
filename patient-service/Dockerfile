FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY episante-common /common
RUN mvn -f /common/pom.xml clean install -DskipTests

COPY patient-service/pom.xml .
RUN mvn -q dependency:go-offline -DskipTests

COPY patient-service/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 9091
ENTRYPOINT ["java", "-jar", "app.jar"]
