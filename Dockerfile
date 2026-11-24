# Image unique : frontend React embarqué dans le jar Spring Boot (research R1).

FROM node:24-alpine AS frontend
WORKDIR /app
COPY src/main/resources/openapi/openapi.yaml src/main/resources/openapi/openapi.yaml
COPY frontend/package.json frontend/package-lock.json frontend/
RUN cd frontend && npm ci --no-audit --no-fund
COPY frontend frontend
RUN cd frontend && npm run build

FROM eclipse-temurin:21-jdk-alpine AS backend
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null
COPY src src
COPY --from=frontend /app/frontend/dist frontend/dist
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=backend /app/build/libs/carsupervision-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
