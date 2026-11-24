# CLAUDE.md

Guide de travail pour Claude sur carSupervision : application web privée de suivi d'entretien
des véhicules d'une famille. Répondre en **français**.

## Règle de collaboration : contredire quand il y a mieux

Avant d'exécuter une demande, si elle n'est pas optimale, contredit une règle du projet ou si
une meilleure approche existe, **le dire d'abord** : expliquer pourquoi, proposer l'alternative
recommandée, puis suivre la décision du mainteneur (Said). Ne pas exécuter en silence une
solution que l'on juge moins bonne.

## Règles du projet

La constitution `.specify/memory/constitution.md` prévaut. En pratique :

- **Contrat d'abord** : toute API commence dans
  `backend/api-contract/src/main/resources/openapi/openapi.yaml`. Les interfaces `*Api.java`
  et DTOs sont générés au build dans `backend/api-contract/build/generated/openapi` (jamais
  édités ni commités). Le frontend utilise les types générés (`npm run generate:api` vers
  `frontend/src/api/generated/`).
- **Monolithe multi-modules** : un sous-projet Gradle par module dans `backend/`, un seul
  déployable (`backend/app`). Dépendances dans un seul sens :
  `app → vehicle → security → member → common → api-contract`. Ne jamais créer de cycle ni
  faire dépendre un module d'un module « au-dessus » ; un nouveau module métier est un nouveau
  sous-projet (ajouté dans `settings.gradle.kts` et comme dépendance de `app`).
- **Dans chaque module** : couches Controller / Service / Repository / Mapper (MapStruct).
  Jamais d'entité JPA exposée par l'API. Versions des dépendances uniquement dans
  `gradle/libs.versions.toml` ; réglages communs dans `buildSrc` (`carsupervision.java-module`).
- **Schéma** : uniquement via un nouveau changeset Liquibase dans le module concerné
  (`backend/<module>/src/main/resources/db/changelog/changes/`), inclus dans le changelog
  maître de `backend/app`, jamais en modifiant un changeset existant.
- **Sécurité** : connexion Google + liste blanche en base ; endpoints ADMIN protégés côté
  serveur. Un USER ne voit que ses véhicules.
- **Tests** : tests d'intégration (Testcontainers) pour les règles RBAC et les échéances, et
  validation de conformité au contrat.
- **Interface** : suivre la skill `charte-interface` avant de toucher à `frontend/`.
  Libellés uniquement dans `frontend/src/i18n/fr.ts`.
- **Simplicité** : pas d'infrastructure non justifiée.
- Fonctionnalités : workflow Spec Kit (`specs/<numéro>-<nom>/` : spec, plan, tasks, quickstart).

## Commandes

Prérequis : JDK 21 (Gradle le télécharge s'il manque), Node.js 22.22 ou plus, Docker.
Sous Windows, remplacer `./gradlew` par `.\gradlew.bat`.

| Action | Commande |
|---|---|
| Base PostgreSQL | `docker compose up -d postgres` |
| Backend (:8080, lit `.env`, profil dev) | `./gradlew bootRun` |
| Frontend (:5173, proxy vers :8080) | `cd frontend && npm install && npm run dev` |
| Tout en Docker (frontend :5173, backend :8080) | `docker compose up -d --build` (ou un service : `postgres`, `backend`, `frontend`) |
| Tests backend | `./gradlew test` |
| Tests frontend | `cd frontend && npm test -- --run` |
| Lint frontend | `cd frontend && npm run lint` |

- Configuration dans `.env` (copie de `.env.example`, jamais commité) : `INITIAL_ADMIN_EMAIL`,
  `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, accès base.
- La connexion Google passe par `http://localhost:5173/login/oauth2/code/google`, en natif
  (Vite) comme en Docker (nginx du frontend, qui relaie `/api`, `/oauth2`, `/login` vers le backend).
- Images Docker : `backend/Dockerfile` et `frontend/Dockerfile`, contexte de build = racine.
- Le premier build Gradle et les premiers tests Testcontainers sont lents (téléchargements).

## Repères dans le code

- `backend/api-contract/` : contrat OpenAPI et génération.
- `backend/common/` : erreurs Problem Details, normalisation, horloge, configuration web.
- `backend/member/` : membres, rôles, liste blanche, administrateur initial.
- `backend/security/` : connexion Google, session, membre connecté (`CurrentMember`), `/api/me`.
- `backend/vehicle/` : parc de véhicules et attributions (pour l'instant les migrations).
- `backend/app/` : point d'entrée, `application.yaml`, changelog maître ; ses tests
  (`src/test/java/`) sont les tests d'intégration `*IT` de l'application assemblée. Les tests
  unitaires `*Test` restent dans leur module.
- `frontend/src/` : `api/client.ts` (client typé), `auth/`, `pages/`, `components/ui/`
  (shadcn/ui), `i18n/fr.ts`.
