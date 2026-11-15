FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY lake-domain/pom.xml lake-domain/pom.xml
COPY lake-console/pom.xml lake-console/pom.xml
COPY s3-gateway/pom.xml s3-gateway/pom.xml
COPY lake-domain/src lake-domain/src
COPY lake-console/src lake-console/src
RUN mvn -B -pl lake-console -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/lake-console/target/app.jar /app/app.jar
RUN useradd --system --uid 10001 --home /app lake && chown -R lake:lake /app
USER lake
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
