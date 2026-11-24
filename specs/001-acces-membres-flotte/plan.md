# Implementation Plan: Accès sécurisé, membres et parc de véhicules

**Branch**: `feature/plan-acces-membres-flotte` | **Date**: 2026-11-23 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-acces-membres-flotte/spec.md`

## Summary

Première tranche de carSupervision : connexion par compte Google limitée à une liste blanche
persistée en PostgreSQL (premier ADMIN créé depuis `INITIAL_ADMIN_EMAIL`, refus 403 pour tout
autre compte), rôles ADMIN et USER, administration des membres, du parc de véhicules et des
véhicules attribués à chaque USER.

Approche technique : monolithe Spring Boot 3 / Java 21 qui sert aussi la SPA React. La
connexion utilise `oauth2Login()` de Spring Security avec une session serveur persistée en base
(Spring Session JDBC). Les droits sont relus en base à chaque requête pour qu'un retrait ou un
changement de rôle prenne effet immédiatement. L'API est définie d'abord dans
`openapi.yaml` ; les interfaces Spring et les types TypeScript en sont générés. Les règles
critiques (liste blanche, 401/403, accès véhicule, dernier ADMIN) sont couvertes par des tests
d'intégration sur un vrai PostgreSQL (Testcontainers). Détails et alternatives :
[research.md](./research.md).

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5 (frontend)

**Primary Dependencies**: Spring Boot 3.5.x (Web, Security, OAuth2 Client, Data JPA,
Validation, Actuator), Spring Session JDBC, Liquibase, springdoc-openapi, plugin Gradle
`org.openapi.generator` ; React, Vite, React Router, TanStack Query, Tailwind CSS + shadcn/ui,
react-hook-form, openapi-typescript + openapi-fetch. Versions exactes figées dans
`build.gradle.kts` et `package.json` au début de l'implémentation (dernières versions stables
de ces lignes majeures).

**Storage**: PostgreSQL 17 (membres, véhicules, attributions, sessions), schéma géré par
Liquibase. Pas de stockage de fichiers dans cette fonctionnalité.

**Testing**: JUnit 5, Spring Boot Test + MockMvc + `spring-security-test` (`oidcLogin()`),
Testcontainers PostgreSQL, `swagger-request-validator-mockmvc` pour la conformité au contrat ;
Vitest + Testing Library côté frontend.

**Target Platform**: conteneur Linux (image Docker unique) sur petit VPS (Docker Compose) ou
GCP Cloud Run ; navigateurs mobiles et desktop récents.

**Project Type**: application web (API REST + SPA servie par le même déployable).

**Performance Goals**: réponses API < 300 ms au 95e centile en conditions familiales ; écran
d'accueil utilisable en moins de 2 s sur mobile 4G.

**Constraints**: aucun mot de passe stocké ; droits réévalués à chaque requête ; secrets
uniquement en variables d'environnement ; interface en français, utilisable à 360 px de large.

**Scale/Scope**: 2 à 5 membres, 2 à 5 véhicules ; 8 chemins d'API, environ 7 écrans
(connexion, accès refusé, mes véhicules, détail véhicule, parc ADMIN, membres ADMIN, édition
membre avec attributions).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principe | Exigence | Comment le plan la respecte | Statut |
|---|---|---|---|
| I. Contract-First | Contrat `openapi.yaml` d'abord, interfaces et DTOs générés au build, jamais commités, frontend sur types générés, Swagger UI | Contrat rédigé dans [contracts/openapi.yaml](./contracts/openapi.yaml), copié en `backend/api-contract/src/main/resources/openapi/openapi.yaml` en première tâche ; `openApiGenerate` vers `backend/api-contract/build/generated/openapi` ; contrôleurs implémentant les `*Api` générés ; `openapi-typescript` + `openapi-fetch` côté frontend ; Swagger UI servi depuis le fichier. Les points d'entrée OAuth2 et `/logout` de Spring Security ne sont pas des contrôleurs REST et sont documentés dans la description du contrat (voir ci-dessous). | ✅ |
| II. Monolithe modulaire en couches | Un déployable, modules métier, Controller / Service / Repository / Mapper, pas d'entité exposée | Un seul jar/image ; modules `administration`, `vehicle`, `security`, `common` ; mappers MapStruct entité ↔ DTO ; logique dans les services. | ✅ |
| III. Auth déléguée à Google | OAuth2/OIDC via Spring Security, liste blanche en base, 403 pour les autres, `INITIAL_ADMIN_EMAIL`, secrets en variables d'environnement | `oauth2Login()` + `OidcUserService` qui vérifie la liste blanche et `email_verified` ; 403 avec page dédiée ; `InitialAdminInitializer` ; `.env` ignoré. | ✅ |
| IV. RBAC | Deux rôles, `@PreAuthorize("hasRole('ADMIN')")` sur l'administration, USER limité à ses véhicules | Rôle relu en base à chaque requête ; `@PreAuthorize` sur toutes les opérations ADMIN ; `VehicleAccessGuard` dans la couche service. | ✅ |
| V. Tests ciblés | Tests d'intégration RBAC (liste blanche, 403, ADMIN, accès véhicules) et conformité au contrat, en CI | Tests Testcontainers listés dans [research.md R15](./research.md) ; validation de chaque requête/réponse contre `openapi.yaml` ; CI GitHub Actions. Le calcul des échéances n'existe pas encore (hors périmètre). | ✅ |
| VI. Simplicité | Pas d'infrastructure non justifiée, complexité justifiée | Seul ajout : Spring Session JDBC, qui réutilise PostgreSQL (aucun nouveau service) ; justifié en Complexity Tracking. | ✅ (justifié) |
| Stack | Java 21, Spring Boot 3, JPA, Liquibase, Gradle Kotlin DSL, React TS, PostgreSQL | Conforme. | ✅ |
| Dev / CI/CD | Docker Compose pour PostgreSQL, backend et frontend natifs, pipeline OpenAPI → build → tests → image | `compose.yaml`, `bootRun`, `npm run dev`, `.github/workflows/ci.yml`. | ✅ |

**Point d'interprétation (principe I)** : la constitution interdit tout endpoint REST hors
contrat. Les URL `/oauth2/authorization/google`, `/login/oauth2/code/google` et `/logout` sont
des filtres du protocole OAuth2 fournis par Spring Security, pas des contrôleurs ; elles ne
peuvent pas implémenter une interface générée. Elles sont décrites dans la description du
contrat pour rester visibles en revue. De même, le repli des routes de la SPA vers
`index.html` passe par la configuration des ressources statiques, pas par un contrôleur.

**Résultat** : porte franchie avant la Phase 0, et confirmée après la Phase 1 (le design
n'ajoute aucun écart).

## Project Structure

### Documentation (this feature)

```text
specs/001-acces-membres-flotte/
├── spec.md              # Spécification (fusionnée, PR #2)
├── plan.md              # Ce fichier
├── research.md          # Phase 0 : décisions techniques
├── data-model.md        # Phase 1 : entités, contraintes, règles
├── quickstart.md        # Phase 1 : guide de validation
├── contracts/
│   └── openapi.yaml     # Phase 1 : contrat d'API (devient la source dans src/main/resources)
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks, pas encore créé)
```

### Source Code (repository root)

```text
settings.gradle.kts                  # liste des modules (sous-projets Gradle)
gradle/libs.versions.toml            # versions de toutes les dépendances
buildSrc/                            # convention commune des modules (Java 21, BOM Spring Boot, MapStruct, JUnit)
gradlew, gradle/wrapper/
compose.yaml                         # PostgreSQL pour le développement ; profil « full » : toute l'app
Dockerfile                           # build frontend + build Gradle + image JRE 21
.env.example
.github/workflows/ci.yml

backend/
├── api-contract/                    # contrat et code généré
│   ├── src/main/resources/openapi/openapi.yaml   # contrat (source unique)
│   └── build/generated/openapi/     # interfaces *Api et DTOs, générés, jamais commités
├── common/                          # fr.carsupervision.common
│   ├── error/                       # ApiException, codes, @RestControllerAdvice (Problem Details)
│   ├── time/                        # horloge partagée
│   └── web/                         # préfixe /api, contrat servi, repli SPA vers index.html
├── member/                          # fr.carsupervision.member
│   ├── Member, Role, MemberRepository, MemberMapper
│   ├── InitialAdminInitializer      # ADMIN initial depuis INITIAL_ADMIN_EMAIL
│   ├── MemberService, MembersController   # US2 : gestion des membres, dernier ADMIN
│   └── changeset 001-create-member
├── security/                        # fr.carsupervision.security
│   ├── SecurityConfig               # oauth2Login, CSRF, 401 JSON, règles d'accès
│   ├── WhitelistOidcUserService, AccessDeniedLoginHandler, MemberLoginService
│   ├── CurrentMemberFilter          # relit le membre et son rôle à chaque requête
│   ├── CurrentMember, AuthenticatedMember, CurrentMemberMapper, SessionController (/api/me)
│   └── static/access-denied.html, changeset 004-create-spring-session
├── vehicle/                         # fr.carsupervision.vehicle (US3, US4)
│   ├── Vehicle, VehicleStatus, EnergyType, VehicleRepository, VehicleService, VehicleMapper
│   ├── VehiclesController
│   ├── VehicleAccess, VehicleAccessRepository, VehicleAccessService, VehicleAccessController
│   ├── VehicleAccessGuard           # règle de lecture ADMIN / USER réutilisable
│   └── changesets 002-create-vehicle, 003-create-vehicle-access
└── app/                             # le déployable unique
    ├── CarSupervisionApplication    # point d'entrée, assemble tous les modules
    ├── application.yaml             # profils dev / prod, variables d'environnement
    ├── db/changelog/db.changelog-master.yaml   # inclut les changesets des modules
    └── src/test/                    # tests d'intégration (Testcontainers) sur l'application assemblée
        ├── support/                 # base Testcontainers, validateur de contrat, fabriques de membres
        ├── security/                # liste blanche, email non vérifié, 401, retrait immédiat
        ├── member/                  # ADMIN initial, membres, dernier ADMIN (dont concurrence)
        └── vehicle/                 # CRUD, doublons, archivage, attributions, accès USER

frontend/
├── package.json, vite.config.ts, tsconfig.json, index.html
├── src/
│   ├── api/                         # client openapi-fetch, CSRF, gestion 401/403 ; generated/ ignoré par git
│   ├── auth/                        # contexte du membre connecté, garde de routes
│   ├── components/ui/               # composants shadcn/ui
│   ├── pages/
│   │   ├── LoginPage.tsx
│   │   ├── VehiclesPage.tsx         # « mes véhicules » / parc courant
│   │   ├── VehicleDetailPage.tsx
│   │   └── admin/                   # MembersPage, MemberEditPage, FleetPage, VehicleFormPage
│   ├── i18n/fr.ts                   # libellés et messages en français
│   └── main.tsx, App.tsx
└── tests/
```

**Structure Decision** : monolithe modulaire **multi-modules Gradle** (décision du mainteneur
du 2026-11-24), conforme au principe II : un seul déployable (`backend/app`) et un
sous-projet Gradle par module, frontend dans `frontend/`. Les dépendances entre modules sont
déclarées explicitement et vont dans un seul sens :

```text
app ──> vehicle ──> security ──> member ──> common ──> api-contract
```

`vehicle` dépend de `security` pour le membre connecté (accès d'un USER à ses véhicules) ;
`member` ne dépend pas de `security`, ce qui évite un cycle. Chaque module porte ses entités,
services, contrôleurs et changesets Liquibase ; les changesets gardent le chemin
`db/changelog/changes/…`, donc l'historique Liquibase des bases existantes reste valide, et le
changelog maître est dans `app`. Les tests d'intégration vivent dans `app`, qui assemble
l'application ; les tests unitaires restent dans leur module. Les modules futurs (kilométrages,
entretiens, échéances, justificatifs) seront de nouveaux sous-projets `backend/<module>` qui
dépendront de `vehicle` et réutiliseront `VehicleAccessGuard`.

## Découpage de livraison suggéré

Pour `/speckit-tasks`, dans l'ordre des priorités de la spec, chaque étape restant
démontrable :

1. **Socle** : projet Gradle, contrat, génération, Liquibase, Compose, CI, squelette frontend.
2. **US1 Connexion** : liste blanche, ADMIN initial, refus 403, 401, déconnexion, `/me`, écran
   de connexion et page de refus.
3. **US2 Membres** : CRUD membres, règle du dernier ADMIN, effet immédiat, écrans ADMIN.
4. **US3 Parc** : CRUD véhicules, unicité de l'immatriculation, archivage, écrans ADMIN.
5. **US4 Attributions** : attributions, filtrage USER, 403 sans fuite, écran « mes véhicules ».
6. **Finition** : Dockerfile, Swagger UI, vérification mobile 360 px, quickstart.

## Complexity Tracking

| Écart ou ajout | Pourquoi c'est nécessaire | Alternative plus simple écartée parce que |
|---|---|---|
| Spring Session JDBC (sessions en base) | Sur Cloud Run, les instances s'arrêtent et changent à chaque déploiement : avec des sessions en mémoire, la famille devrait se reconnecter très souvent | Sessions en mémoire : perdues à chaque redémarrage. Redis : nouveau composant d'infrastructure. Spring Session JDBC n'ajoute que deux tables dans la base existante. |
| Validation de contrat dans les tests (`swagger-request-validator-mockmvc`) | Exigée par le principe V (« conformité … validée par des tests ») | Se reposer sur la compilation des interfaces générées ne vérifie ni les codes HTTP ni le corps des erreurs. |
