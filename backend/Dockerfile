# Image du backend : le jar Spring Boot du module backend/app (API, sécurité, Liquibase).
# Contexte de build : racine du dépôt (docker build -f backend/Dockerfile .).

FROM eclipse-temurin:21-jdk-alpine AS build
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
RUN ./gradlew --no-daemon :backend:app:bootJar

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /app/backend/app/build/libs/carsupervision.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health > /dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
