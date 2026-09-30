FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/target/java-interview-rag-0.0.1-SNAPSHOT.jar /app/app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport"
EXPOSE 18080
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=10 \
    CMD curl --fail --silent http://127.0.0.1:18080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=storage"]
