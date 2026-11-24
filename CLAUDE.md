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

- **Contrat d'abord** : toute API commence dans `src/main/resources/openapi/openapi.yaml`.
  Les interfaces `*Api.java` et DTOs sont générés au build dans `build/generated/openapi`
  (jamais édités ni commités). Le frontend utilise les types générés
  (`npm run generate:api` vers `frontend/src/api/generated/`).
- **Backend** : modules métier, couches Controller / Service / Repository / Mapper (MapStruct).
  Jamais d'entité JPA exposée par l'API.
- **Schéma** : uniquement via un nouveau changeset Liquibase dans
  `src/main/resources/db/changelog/changes/`, jamais en modifiant un changeset existant.
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
| Base PostgreSQL | `docker compose up -d` |
| Backend (:8080, lit `.env`, profil dev) | `./gradlew bootRun` |
| Frontend (:5173, proxy vers :8080) | `cd frontend && npm install && npm run dev` |
| Tout en Docker (:8080) | `docker compose --profile full up --build` |
| Tests backend | `./gradlew test` |
| Tests frontend | `cd frontend && npm test -- --run` |
| Lint frontend | `cd frontend && npm run lint` |

- Configuration dans `.env` (copie de `.env.example`, jamais commité) : `INITIAL_ADMIN_EMAIL`,
  `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, accès base.
- La connexion Google en dev passe par `http://localhost:5173/login/oauth2/code/google`
  (et `http://localhost:8080/...` pour le mode tout en Docker).
- Le premier build Gradle et les premiers tests Testcontainers sont lents (téléchargements).

## Repères dans le code

- `src/main/java/fr/carsupervision/` : `security/` (OAuth, liste blanche, session),
  `administration/` (membres, admin initial), `common/` (erreurs Problem Details, web).
- `src/test/java/` : tests d'intégration `*IT` et tests unitaires `*Test`.
- `frontend/src/` : `api/client.ts` (client typé), `auth/`, `pages/`, `components/ui/`
  (shadcn/ui), `i18n/fr.ts`.
