# Build the jar, then run it on a JRE only. Build context is the backend/ folder.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY gradle gradle
COPY src src
# Tests need Docker (Testcontainers) and run in CI, not in the image build.
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
# bootJar also leaves a *-plain.jar next to the runnable one; only the runnable jar is copied.
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar
# The container gets a small memory limit on a free plan: let the JVM size itself from it.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
