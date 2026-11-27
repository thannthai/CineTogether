# Build the jar, then run it on a JRE only. Build context is the backend/ folder.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY gradle gradle
COPY docker/mirror.init.gradle /root/.gradle/init.d/mirror.gradle
COPY src src
# Tests need Docker (Testcontainers) and run in CI, not in the image build.
# Maven Central sometimes answers 429 (too many requests) to the shared address of a build machine. The downloads that
# worked stay in the Gradle cache, so each new attempt goes further; the build fails only if the jar is still missing.
RUN for attempt in 1 2 3 4 5; do ./gradlew bootJar -x test --no-daemon && break; echo "Gradle failed (attempt $attempt), retrying"; sleep 15; done; \
    ls build/libs/*-SNAPSHOT.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
# bootJar also leaves a *-plain.jar next to the runnable one; only the runnable jar is copied.
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar
# The container gets a small memory limit on a free plan: let the JVM size itself from it.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
