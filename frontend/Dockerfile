# Image du frontend : application React construite, servie par nginx, qui relaie vers le
# backend les appels /api et les routes de connexion (même origine pour le cookie de session).
# Contexte de build : racine du dépôt (docker build -f frontend/Dockerfile .), car les types
# d'API sont générés depuis le contrat backend/api-contract.

FROM node:24-alpine AS build
WORKDIR /app
COPY backend/api-contract/src/main/resources/openapi/openapi.yaml backend/api-contract/src/main/resources/openapi/openapi.yaml
COPY frontend/package.json frontend/package-lock.json frontend/
# Cache npm conservé d'un build à l'autre (cache BuildKit) : un package-lock modifié ne
# retélécharge que les paquets manquants.
RUN --mount=type=cache,target=/root/.npm \
    cd frontend && npm ci --no-audit --no-fund
COPY frontend frontend
RUN cd frontend && npm run build

FROM nginx:1.29-alpine
# Adresse du backend vue depuis ce conteneur (nom du service Compose par défaut).
ENV BACKEND_URL=http://backend:8080
COPY frontend/nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/frontend/dist /usr/share/nginx/html
EXPOSE 80
