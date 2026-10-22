# syntax=docker/dockerfile:1

# ---------------------------------------------------------------- build stage
FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /build

# Dependency layer: only re-resolved when the POM or wrapper changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests \
    && mv target/*.jar application.jar \
    # Unpack so the JVM can load unchanged library layers from cache.
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---------------------------------------------------------------- run stage
FROM eclipse-temurin:25-jre-alpine AS runtime

RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

COPY --from=build --chown=spring:spring /build/extracted/dependencies/ ./
COPY --from=build --chown=spring:spring /build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=spring:spring /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /build/extracted/application/ ./

USER spring

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseZGC -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "-jar", "application.jar"]
