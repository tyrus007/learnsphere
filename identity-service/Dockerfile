FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY common/jwt-common ./common/jwt-common
COPY identity-service ./identity-service

RUN mvn -f common/jwt-common/pom.xml clean install -DskipTests \
    && mvn -f identity-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /build/identity-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
