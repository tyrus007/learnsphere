FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY common/jwt-common ./common/jwt-common
COPY progress-service ./progress-service

RUN mvn -f common/jwt-common/pom.xml clean install -DskipTests \
    && mvn -f progress-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /build/progress-service/target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
