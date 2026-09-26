# Java 8 build and runtime for Railway.
FROM eclipse-temurin:8-jdk AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle gradle
COPY src src

# A Windows checkout can leave CRLF in the wrapper script.
RUN sed -i 's/\r$//' gradlew \
    && chmod +x gradlew \
    && ./gradlew bootJar --no-daemon -x test \
    && find build/libs -name '*.jar' ! -name '*-plain.jar' -exec cp {} /app/app.jar \;

FROM eclipse-temurin:8-jre
WORKDIR /app

RUN mkdir -p /app/uploads

COPY --from=build /app/app.jar /app/app.jar

ENV PORT=8080
EXPOSE 8080

# Stay inside Railway's memory limit.
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
