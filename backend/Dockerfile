FROM maven:3.9-eclipse-temurin-21 AS compilacion

WORKDIR /aplicacion
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress package -DskipTests

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S aplicacion && adduser -S aplicacion -G aplicacion
WORKDIR /aplicacion
COPY --from=compilacion /aplicacion/target/backend-0.0.1-SNAPSHOT.jar aplicacion.jar
USER aplicacion

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/aplicacion/aplicacion.jar"]
