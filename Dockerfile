FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY backend ./backend/
COPY pom.xml .
COPY frontend/pom.xml ./frontend/pom.xml

RUN mvn -pl backend -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/backend/target/backend-1.0.0.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]