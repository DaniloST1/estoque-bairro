FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src src
RUN mvn -B -ntp -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd -r estoque && useradd -r -g estoque estoque
COPY --from=build /app/target/estoque-bairro-1.0.0.jar app.jar
USER estoque
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
