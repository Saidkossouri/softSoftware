# Quickstart : valider la fonctionnalité 001 de bout en bout

**Feature**: `001-acces-membres-flotte` | **Plan**: [plan.md](./plan.md)

Guide de validation une fois la fonctionnalité implémentée. Il ne décrit pas l'implémentation :
les endpoints sont dans [contracts/openapi.yaml](./contracts/openapi.yaml), les règles dans
[data-model.md](./data-model.md).

## Prérequis

- JDK 21, Node.js LTS (avec npm), Docker ou Rancher Desktop (pour PostgreSQL et
  Testcontainers).
- Un client OAuth Google de type « Application Web » créé dans Google Cloud Console
  (API & Services > Identifiants), avec ces URI de redirection autorisées :
  - développement : `http://localhost:5173/login/oauth2/code/google`
  - production : `https://<domaine>/login/oauth2/code/google`
- Deux comptes Google pour les tests : celui de l'ADMIN initial et un second compte (membre).

## 1. Configuration locale

Copier `.env.example` en `.env` (fichier ignoré par git) et renseigner :

| Variable | Exemple |
|---|---|
| `INITIAL_ADMIN_EMAIL` | `said@example.com` |
| `GOOGLE_CLIENT_ID` | identifiant du client OAuth |
| `GOOGLE_CLIENT_SECRET` | secret du client OAuth |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/carsupervision` |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | identifiants de la base locale |

## 2. Lancer l'application en développement

```bash
docker compose up -d postgres   # PostgreSQL
./gradlew bootRun               # backend sur :8080 (génère le code OpenAPI, applique Liquibase)
cd frontend && npm install && npm run dev   # frontend sur :5173
```

Ouvrir `http://localhost:5173`.

## 3. Lancer les tests

```bash
./gradlew test                  # intégration backend (Testcontainers) + conformité au contrat
cd frontend && npm test         # tests de composants
```

Résultat attendu : tous les tests passent, dont ceux qui couvrent la liste blanche, les
refus 401/403, la limitation des véhicules d'un USER et la règle du dernier ADMIN.

## 4. Scénarios de validation manuelle

Chaque scénario reprend un critère d'acceptation de la [spec](./spec.md).

| # | Action | Résultat attendu | Réf. |
|---|---|---|---|
| 1 | Base vide, démarrer **sans** `INITIAL_ADMIN_EMAIL` | Le backend s'arrête avec un message explicite | FR-004 |
| 2 | Démarrer avec `INITIAL_ADMIN_EMAIL`, se connecter avec ce compte | Accueil affiché, nom, email et rôle ADMIN visibles, en moins d'une minute | US1-1, SC-001 |
| 3 | Se connecter avec le second compte (non ajouté) | Page « Accès refusé » en 403, aucune donnée | US1-2, SC-002 |
| 4 | Sans session, appeler `GET http://localhost:8080/api/vehicles` | 401, corps Problem Details, aucune donnée | US1-4 |
| 5 | En ADMIN, ajouter le second compte avec le rôle USER (depuis un téléphone ou une fenêtre de 360 px) | Membre listé ; il peut se connecter en USER | US2-1, SC-004 |
| 6 | Ajouter de nouveau le même email avec une autre casse | Refus « doublon » | US2-6 |
| 7 | Tenter de passer le seul ADMIN en USER, puis de le retirer | Refus « il faut au moins un ADMIN » | US2-4, SC-007 |
| 8 | En ADMIN, créer deux véhicules A et B, puis B avec l'immatriculation de A écrite `ab-123 cd` | Deux véhicules créés ; le troisième refusé pour doublon | US3-1, US3-3, SC-005 |
| 9 | Attribuer A seulement au USER ; se connecter en USER | Seul A est listé | US4-1 |
| 10 | En USER, ouvrir directement l'URL de B ou appeler `GET /api/vehicles/{idB}` | 403, aucune information sur B | US4-2, SC-003 |
| 11 | En USER, appeler `GET /api/members` ou `POST /api/vehicles` | 403 | US2-5, US3-5 |
| 12 | Archiver A en ADMIN, puis recharger la liste du USER | A n'apparaît plus ; message « aucun véhicule attribué » | US3-4, US4-5, FR-023 |
| 13 | Réactiver A | A réapparaît pour le USER (attribution conservée) | Cas limite archivage |
| 14 | Le USER garde sa session ouverte ; l'ADMIN le retire | Requête suivante du USER : 403 | US2-3, SC-006 |
| 15 | Se déconnecter | Retour à l'écran de connexion ; les pages protégées redemandent la connexion | US1-3 |
| 16 | Ouvrir Swagger UI (`/swagger-ui/index.html`) connecté | Documentation générée depuis `openapi.yaml` | Principe I |

## 5. Images Docker

Chaque brique a sa propre image ; Compose les lance ensemble ou séparément :

```bash
docker compose up -d --build            # base, backend (:8080) et frontend (:5173)
docker compose up -d --build backend    # backend seul (avec sa base)
docker compose up -d --build frontend   # frontend seul (nginx, relaie /api vers le backend)
```

Ouvrir `http://localhost:5173` : l'URI de redirection Google du développement suffit.
