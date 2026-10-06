# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Keep Maven dependencies in a regular layer so the CI cache exports them too.
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline

# Run the isolated regression suites before producing the image. MapperSmokeTest
# requires an external development MySQL database and is not part of this build.
COPY src ./src
RUN mvn -q -Dtest=WorkNoteIntegrationTest,ArticlePublicationDateTest package
RUN java -Djarmode=layertools -jar target/app.jar extract --destination /app/layers

FROM eclipse-temurin:17-jre
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Stable dependencies precede application code so code-only updates reuse them.
COPY --from=builder /app/layers/dependencies/ ./
COPY --from=builder /app/layers/spring-boot-loader/ ./
COPY --from=builder /app/layers/snapshot-dependencies/ ./
COPY --from=builder /app/layers/application/ ./

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
