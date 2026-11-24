# Image unique : frontend React embarqué dans le jar Spring Boot du module backend/app (research R1).

FROM node:24-alpine AS frontend
WORKDIR /app
COPY backend/api-contract/src/main/resources/openapi/openapi.yaml backend/api-contract/src/main/resources/openapi/openapi.yaml
COPY frontend/package.json frontend/package-lock.json frontend/
RUN cd frontend && npm ci --no-audit --no-fund
COPY frontend frontend
RUN cd frontend && npm run build

FROM eclipse-temurin:21-jdk-alpine AS backend
WORKDIR /app
# Fichiers de build d'abord, pour mettre en cache le téléchargement des dépendances.
COPY gradlew settings.gradle.kts ./
COPY gradle gradle
COPY buildSrc buildSrc
COPY backend/api-contract/build.gradle.kts backend/api-contract/
COPY backend/common/build.gradle.kts backend/common/
COPY backend/member/build.gradle.kts backend/member/
COPY backend/security/build.gradle.kts backend/security/
COPY backend/vehicle/build.gradle.kts backend/vehicle/
COPY backend/app/build.gradle.kts backend/app/
RUN ./gradlew --no-daemon :backend:app:dependencies > /dev/null
COPY backend backend
COPY --from=frontend /app/frontend/dist frontend/dist
RUN ./gradlew --no-daemon :backend:app:bootJar

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=backend /app/backend/app/build/libs/carsupervision.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
