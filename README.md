# carSupervision

Application web privée pour suivre l'entretien et les révisions des véhicules de la famille :
kilométrage, historique des entretiens, échéances et justificatifs. Connexion avec un compte
Google, accès réservé aux membres ajoutés par un administrateur.

Stack : Java 21 / Spring Boot 3, PostgreSQL, React + TypeScript (Vite, Tailwind, shadcn/ui),
contrat d'API OpenAPI.

## Structure du projet

Monolithe modulaire : un seul déployable, mais un sous-projet Gradle par module, avec des
dépendances déclarées dans un seul sens
(`app → vehicle → security → member → common → api-contract`).

```text
backend/
  api-contract/   contrat OpenAPI (openapi.yaml), interfaces et DTOs générés au build
  common/         erreurs Problem Details, normalisation, horloge, configuration web
  member/         membres, rôles, liste blanche, administrateur initial
  security/       connexion Google, session, membre connecté
  vehicle/        parc de véhicules et attributions
  app/            point d'entrée Spring Boot, configuration, Liquibase, tests d'intégration
                  (backend/Dockerfile : image du backend)
frontend/         application React (Vite), servie par nginx dans son image Docker
buildSrc/         réglages de build communs à tous les modules
gradle/libs.versions.toml   versions de toutes les dépendances
```

## Lancer le projet en local

### 1. Prérequis

| Outil | Version | Installation sous Windows |
|---|---|---|
| JDK | 21 | `winget install EclipseAdoptium.Temurin.21.JDK` (facultatif : Gradle télécharge Java 21 s'il manque) |
| Node.js | 22.22 ou plus | `winget install OpenJS.NodeJS.LTS` |
| Docker | Docker Desktop ou Rancher Desktop | [docker.com](https://www.docker.com/products/docker-desktop/) (nécessite WSL 2) |

Docker sert à la base PostgreSQL et aux tests backend (Testcontainers).

### 2. Client OAuth Google (une seule fois)

1. Dans [Google Cloud Console](https://console.cloud.google.com/), créer un projet.
2. Configurer l'écran de consentement OAuth (audience « Externe ») et ajouter les comptes
   Google de test dans « Audience ».
3. Dans « Clients », créer un client de type **Application Web** avec cette URI de redirection autorisée :
   - `http://localhost:5173/login/oauth2/code/google` (mode développement et mode Docker)
4. Noter l'ID client et le code secret.

### 3. Configuration

Copier `.env.example` en `.env` (le fichier est ignoré par git) et renseigner :

| Variable | Rôle |
|---|---|
| `INITIAL_ADMIN_EMAIL` | email Google du premier administrateur |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | client OAuth de l'étape 2 |
| `DATABASE_URL` / `DATABASE_USERNAME` / `DATABASE_PASSWORD` | base locale (les valeurs par défaut conviennent) |

### 4a. Mode développement (recommandé pour coder)

PostgreSQL tourne dans Docker, le backend et le frontend en natif avec rechargement rapide.
Dans trois terminaux, depuis la racine du projet :

```bash
docker compose up -d postgres                 # PostgreSQL sur :5432
./gradlew bootRun                             # backend sur :8080 (Windows : .\gradlew.bat bootRun)
cd frontend && npm install && npm run dev     # frontend sur :5173
```

Ouvrir **http://localhost:5173** et se connecter avec le compte de `INITIAL_ADMIN_EMAIL`.

`bootRun` lit automatiquement `.env` et active le profil `dev` (cookie de session utilisable
en http). Le premier lancement est long : Gradle télécharge ses dépendances et applique les
migrations Liquibase.

### 4b. Mode tout en Docker (démo, recette)

Aucune installation de Java ou Node nécessaire, seulement Docker et le fichier `.env`.
Chaque brique a sa propre image : `backend/Dockerfile` (jar Spring Boot) et
`frontend/Dockerfile` (application construite, servie par nginx qui relaie `/api` et la
connexion Google vers le backend).

| Commande | Effet |
|---|---|
| `docker compose up -d --build` | tout : base, backend sur :8080, frontend sur :5173 |
| `docker compose up -d postgres` | base seule |
| `docker compose up -d --build backend` | backend (démarre aussi la base dont il dépend) |
| `docker compose up -d --build frontend` | frontend seul (répond 502 sur l'API tant que le backend n'est pas lancé) |
| `docker compose logs -f backend` | journaux d'un service |
| `docker compose stop frontend` | arrêter un service |

Ouvrir **http://localhost:5173**. Le frontend en Docker utilise le même port que le serveur
Vite, donc l'URI de redirection Google du mode développement suffit ; on ne peut pas lancer
les deux en même temps. Toute modification du code demande de reconstruire l'image du
service concerné (`--build`).

### 5. Lancer les tests

```bash
./gradlew test                       # backend : intégration (Testcontainers) et conformité au contrat
cd frontend && npm test -- --run     # frontend : tests de composants
```

### Arrêter

`Ctrl+C` dans les terminaux du backend et du frontend, puis `docker compose down`. Les données
de la base sont conservées dans un volume Docker ; `docker compose down -v` les efface.

## Dépannage

- **Le backend s'arrête au démarrage avec un message sur l'administrateur initial** :
  `INITIAL_ADMIN_EMAIL` est vide dans `.env` alors que la base ne contient aucun ADMIN.
- **Erreur `redirect_uri_mismatch` chez Google** : l'URI de redirection affichée n'est pas
  déclarée dans le client OAuth (voir étape 2).
- **Port 5432, 8080 ou 5173 déjà utilisé** : arrêter l'autre PostgreSQL, backend ou frontend, ou les
  services Docker si le mode développement tourne en même temps (`docker compose stop backend frontend`).
- **Tests backend en échec avec une erreur Docker** : Docker Desktop doit être démarré.

## Documentation

- Constitution du projet : `.specify/memory/constitution.md`
- Spécifications par fonctionnalité : `specs/` (spec, plan, contrat, quickstart de validation)
- Contrat d'API : `backend/api-contract/src/main/resources/openapi/openapi.yaml`, servi par Swagger UI sur
  `/swagger-ui/index.html` une fois connecté
- Guide de travail pour Claude : `CLAUDE.md`
