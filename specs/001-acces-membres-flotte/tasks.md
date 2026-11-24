---
description: "Liste des tâches de la fonctionnalité 001-acces-membres-flotte"
---

# Tasks: Accès sécurisé, membres et parc de véhicules

**Input**: Documents de conception de `/specs/001-acces-membres-flotte/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/openapi.yaml](./contracts/openapi.yaml),
[quickstart.md](./quickstart.md)

**Tests**: OBLIGATOIRES pour cette fonctionnalité. La constitution (principe V) impose des tests
d'intégration sur les règles RBAC (liste blanche, 403, endpoints ADMIN, accès d'un USER limité
à ses véhicules) et la validation de la conformité au contrat OpenAPI. Dans chaque histoire,
les tests sont écrits d'abord et doivent échouer avant l'implémentation.

**Organization**: tâches regroupées par histoire utilisateur, chacune livrable et testable
seule.

## Format: `[ID] [P?] [Story] Description`

- **[P]** : parallélisable (fichiers différents, aucune dépendance sur une tâche non terminée)
- **[Story]** : histoire concernée (US1 à US4)
- Chaque tâche indique le chemin exact du fichier

## Path Conventions

- Backend : monolithe multi-modules Gradle (2026-11-24), un sous-projet par module dans
  `backend/<module>/` (`api-contract`, `common`, `member`, `security`, `vehicle`, `app`), voir
  la structure dans [plan.md](./plan.md). Les tâches terminées gardent les chemins de l'ancienne
  structure à un seul projet ; les tâches restantes utilisent les nouveaux chemins. Tests
  d'intégration : `backend/app/src/test/java/fr/carsupervision/`.
- Code généré (jamais commité) : `backend/api-contract/build/generated/openapi/`
- Frontend : `frontend/src/`, `frontend/tests/`
- Abréviation : `FE` = `frontend/src`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: initialiser le dépôt, le build, l'environnement local et la CI.

- [X] T001 Créer le projet Gradle Kotlin DSL à la racine : `settings.gradle.kts` (nom `carsupervision`), wrapper Gradle (`gradlew`, `gradlew.bat`, `gradle/wrapper/`), et `build.gradle.kts` avec Java 21 (toolchain), Spring Boot 3.5.x et les dépendances Web, Security, OAuth2 Client, Data JPA, Validation, Actuator, Spring Session JDBC, Liquibase, PostgreSQL, springdoc-openapi (Swagger UI), MapStruct ; tests : Spring Boot Test, `spring-security-test`, Testcontainers PostgreSQL + JUnit 5, `swagger-request-validator-mockmvc`
- [X] T002 Créer la classe de démarrage `src/main/java/fr/carsupervision/CarSupervisionApplication.java`
- [X] T003 Copier le contrat `specs/001-acces-membres-flotte/contracts/openapi.yaml` vers `src/main/resources/openapi/openapi.yaml` (source unique du contrat à partir de maintenant)
- [X] T004 Configurer la tâche `openApiGenerate` dans `build.gradle.kts` (plugin `org.openapi.generator`, générateur `spring`, `interfaceOnly=true`, `useSpringBoot3=true`, `useTags=true`, `skipDefaultInterface=true`, `useBeanValidation=true`, `openApiNullable=false`, sortie `build/generated/openapi`, packages `fr.carsupervision.api` et `fr.carsupervision.api.model`), ajouter la sortie aux sources Java et faire dépendre `compileJava` de `openApiGenerate`
- [X] T005 [P] Compléter `.gitignore` : `build/`, `.gradle/`, `.env`, `frontend/node_modules/`, `frontend/dist/`, `frontend/src/api/generated/`, fichiers d'IDE
- [X] T006 [P] Créer `compose.yaml` avec un service PostgreSQL 17 (base `carsupervision`, port 5432, volume nommé, identifiants lus depuis `.env`)
- [X] T007 [P] Créer `.env.example` listant `INITIAL_ADMIN_EMAIL`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` avec des valeurs d'exemple non sensibles
- [X] T008 Créer `src/main/resources/application.yaml` : datasource depuis les variables d'environnement, `spring.jpa.hibernate.ddl-auto=validate`, Liquibase (`db/changelog/db.changelog-master.yaml`), client OAuth2 `google` (scopes `openid,email,profile`, identifiants depuis `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`), `spring.session.store-type=jdbc` avec `initialize-schema=never`, `server.servlet.session.timeout=30d`, cookie de session `HttpOnly`, `Secure`, `SameSite=Lax`, `server.forward-headers-strategy=framework`, `app.initial-admin-email=${INITIAL_ADMIN_EMAIL:}` ; profil `dev` avec cookie non `Secure` pour `http://localhost`
- [X] T009 [P] Initialiser le frontend Vite + React + TypeScript dans `frontend/` (`package.json`, `tsconfig.json`, `vite.config.ts`, `index.html`, `src/main.tsx`, `src/App.tsx`) avec React Router, TanStack Query, react-hook-form, openapi-fetch ; dev : openapi-typescript, Vitest, Testing Library, ESLint
- [ ] T010 [P] Configurer Tailwind CSS et shadcn/ui dans `frontend/` (`tailwind` config, `frontend/src/index.css`, `frontend/components.json`) et ajouter les composants de base (button, input, label, select, checkbox, card, dialog, badge, alert) dans `frontend/src/components/ui/` (fait : button, card, alert, badge, input, label, select natif ; checkbox à ajouter avec les attributions de US4 ; confirmation en ligne plutôt qu'une boîte de dialogue)
- [X] T011 Configurer le proxy Vite dans `frontend/vite.config.ts` vers `http://localhost:8080` pour `/api`, `/oauth2`, `/login`, `/logout`, `/openapi.yaml`, `/swagger-ui`, `/v3` en conservant l'en-tête `Host` (`changeOrigin: false`)
- [X] T012 Ajouter le script `generate:api` dans `frontend/package.json` (`openapi-typescript ../src/main/resources/openapi/openapi.yaml -o src/api/generated/schema.d.ts`) exécuté avant `dev`, `build` et `test` (`predev`, `prebuild`, `pretest`)
- [X] T013 Créer la CI `.github/workflows/ci.yml` (sur push et pull request) : job backend (JDK 21, `./gradlew build` qui enchaîne `openApiGenerate`, compilation et tests Testcontainers), job frontend (Node LTS, `npm ci`, `npm run generate:api`, `npm run lint`, `npm test -- --run`, `npm run build`), puis job de construction de l'image Docker (sans publication)

**Checkpoint**: `./gradlew build` génère les interfaces depuis le contrat et compile ; `npm run build` passe dans `frontend/`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: schéma, sécurité de base, erreurs, accès au membre courant et outillage de test, nécessaires à toutes les histoires.

**⚠️ CRITICAL**: aucune histoire ne peut commencer avant la fin de cette phase.

### Base de données (Liquibase)

- [X] T014 Créer `src/main/resources/db/changelog/db.changelog-master.yaml` incluant les changesets 001 à 004
- [X] T015 [P] Créer `src/main/resources/db/changelog/changes/001-create-member.yaml` : table `member` avec `id uuid` PK, `email varchar(320) NOT NULL UNIQUE`, `display_name varchar(100) NULL`, `role varchar(10) NOT NULL CHECK (role IN ('ADMIN','USER'))`, `created_at timestamptz NOT NULL`, `last_login_at timestamptz NULL`, index sur `role`
- [X] T016 [P] Créer `src/main/resources/db/changelog/changes/002-create-vehicle.yaml` : table `vehicle` avec `id uuid` PK, `nickname varchar(50) NOT NULL`, `make varchar(50) NOT NULL`, `model varchar(50) NOT NULL`, `registration varchar(20) NOT NULL`, `registration_key varchar(20) NOT NULL UNIQUE`, `first_registration_year smallint NULL CHECK (>= 1900)`, `initial_mileage_km integer NOT NULL CHECK (>= 0)`, `vin varchar(17) NULL`, `energy_type varchar(12) NULL CHECK (IN ('PETROL','DIESEL','HYBRID','ELECTRIC','LPG','OTHER'))`, `status varchar(10) NOT NULL DEFAULT 'ACTIVE' CHECK (IN ('ACTIVE','ARCHIVED'))`, `created_at timestamptz NOT NULL`, `updated_at timestamptz NOT NULL`, index sur `status`
- [X] T017 [P] Créer `src/main/resources/db/changelog/changes/003-create-vehicle-access.yaml` : table `vehicle_access` avec `member_id uuid` FK → `member.id` ON DELETE CASCADE, `vehicle_id uuid` FK → `vehicle.id`, `granted_at timestamptz NOT NULL`, PK (`member_id`, `vehicle_id`)
- [X] T018 [P] Créer `src/main/resources/db/changelog/changes/004-create-spring-session.yaml` reprenant le script PostgreSQL officiel de Spring Session JDBC (`spring_session`, `spring_session_attributes` et leurs index)

### Entités partagées

- [X] T019 [P] Créer l'énumération `Role` (`ADMIN`, `USER`) et l'entité JPA `Member` (mappée sur `member`, champs du data-model) dans `src/main/java/fr/carsupervision/administration/member/Role.java` et `src/main/java/fr/carsupervision/administration/member/Member.java`
- [X] T020 [P] Créer `MemberRepository` dans `src/main/java/fr/carsupervision/administration/member/MemberRepository.java` avec `findByEmail(String)`, `existsByEmail(String)`, `countByRole(Role)` et `findAllByRoleForUpdate(Role)` annoté `@Lock(PESSIMISTIC_WRITE)` (verrou `FOR UPDATE` de research R9)
- [X] T021 [P] Créer l'utilitaire de normalisation dans `src/main/java/fr/carsupervision/common/Normalizers.java` : `normalizeEmail` (`trim` + minuscules, Locale.ROOT), `normalizeRegistrationDisplay` (`trim` + majuscules), `registrationKey` (majuscules, sans espaces ni tirets)

### Erreurs (Problem Details)

- [X] T022 [P] Créer `ErrorCode` (valeurs du schéma `Problem.code` du contrat) et `ApiException` (statut HTTP, code, message français, erreurs de champ) dans `src/main/java/fr/carsupervision/common/error/`
- [X] T023 Créer `ApiExceptionHandler` (`@RestControllerAdvice`) dans `src/main/java/fr/carsupervision/common/error/ApiExceptionHandler.java` : traduit `ApiException`, les erreurs Bean Validation (400 `VALIDATION_ERROR` avec `fieldErrors` en français), les violations d'unicité de la base (409), `AccessDeniedException` (403 `ADMIN_ONLY`) et les erreurs inattendues (500 `INTERNAL_ERROR`) en `application/problem+json` conforme au schéma `Problem`

### Sécurité de base

- [X] T024 Créer `SecurityConfig` dans `src/main/java/fr/carsupervision/security/SecurityConfig.java` : `oauth2Login()`, `@EnableMethodSecurity`, CSRF par `CookieCsrfTokenRepository.withHttpOnlyFalse()` avec le gestionnaire SPA de Spring Security 6, accès public à `/`, `/index.html`, `/assets/**`, `/access-denied.html`, `/oauth2/**`, `/login/**`, `/actuator/health` ; `/api/**`, `/swagger-ui/**`, `/v3/**` et `/openapi.yaml` authentifiés ; point d'entrée `/api/**` non authentifié = 401 Problem Details `UNAUTHENTICATED` (pas de redirection) ; refus CSRF = 403 `CSRF_INVALID` ; `logout` sur `POST /logout` répondant 204
- [X] T025 [P] Créer `MemberAuthentication` (porte l'id du membre, son email, son rôle et l'autorité `ROLE_ADMIN` ou `ROLE_USER`) dans `src/main/java/fr/carsupervision/security/MemberAuthentication.java`
- [X] T026 Créer `CurrentMemberFilter` dans `src/main/java/fr/carsupervision/security/CurrentMemberFilter.java`, enregistré après l'authentification pour `/api/**` : relit le membre par email à chaque requête ; introuvable → invalide la session et répond 403 `MEMBER_REVOKED` ; trouvé → remplace l'`Authentication` du contexte par une `MemberAuthentication` construite depuis la base (research R4)
- [X] T027 [P] Créer `CurrentMember` dans `src/main/java/fr/carsupervision/security/CurrentMember.java` : accès au membre courant (id, rôle, `isAdmin()`) pour les services
- [X] T028 [P] Créer `SpaWebConfig` (`WebMvcConfigurer`, résolution de ressources statiques avec repli vers `index.html` pour les routes hors `/api`, `/oauth2`, `/login`, `/logout`, `/swagger-ui`, `/v3`, sans contrôleur REST) et exposer `src/main/resources/openapi/openapi.yaml` en ressource statique `/openapi.yaml` dans `src/main/java/fr/carsupervision/common/web/SpaWebConfig.java` ; configurer Springdoc (`springdoc.swagger-ui.url=/openapi.yaml`, génération automatique désactivée) dans `src/main/resources/application.yaml`
- [X] T029 Configurer `build.gradle.kts` pour copier `frontend/dist` dans `static/` lors de `processResources` quand ce dossier existe

### Outillage de test

- [X] T030 Créer la base de tests d'intégration `IntegrationTestBase` dans `src/test/java/fr/carsupervision/support/IntegrationTestBase.java` : `@SpringBootTest`, `@AutoConfigureMockMvc`, conteneur Testcontainers PostgreSQL 17 partagé (`@ServiceConnection`), `INITIAL_ADMIN_EMAIL` de test, nettoyage des tables entre les tests
- [X] T031 [P] Créer `ContractValidation` dans `src/test/java/fr/carsupervision/support/ContractValidation.java` : `ResultMatcher` `swagger-request-validator-mockmvc` chargé depuis `src/main/resources/openapi/openapi.yaml`, à appliquer à chaque appel MockMvc des tests d'intégration
- [X] T032 [P] Créer `TestMembers` dans `src/test/java/fr/carsupervision/support/TestMembers.java` : fabriques de membres en base et post-processeurs `oidcLogin()` (email, `email_verified`, `name`) pour simuler un ADMIN, un USER, un compte inconnu et un compte non vérifié

### Socle frontend

- [X] T033 [P] Créer le client API dans `frontend/src/api/client.ts` : `openapi-fetch` typé par `frontend/src/api/generated/schema.d.ts`, `baseUrl` `/api`, `credentials: 'same-origin'`, ajout de l'en-tête `X-XSRF-TOKEN` depuis le cookie `XSRF-TOKEN` pour les méthodes modifiantes, et transformation des réponses Problem Details en erreur typée (`code`, `detail`, `fieldErrors`)
- [X] T034 [P] Créer les libellés français dans `frontend/src/i18n/fr.ts` (navigation, rôles, statuts, types d'énergie `PETROL` → « Essence », `DIESEL` → « Diesel », `HYBRID` → « Hybride », `ELECTRIC` → « Électrique », `LPG` → « GPL », `OTHER` → « Autre », messages par code d'erreur)
- [X] T035 Créer la mise en page mobile-first `frontend/src/components/AppLayout.tsx` (en-tête, navigation adaptée à 360 px, zone de contenu) et le routage dans `frontend/src/App.tsx` avec `QueryClientProvider`

**Checkpoint**: l'application démarre sur une base vide migrée par Liquibase, `/api/**` répond 401 sans session, les tests d'intégration peuvent s'exécuter sur PostgreSQL.

---

## Phase 3: User Story 1 - Se connecter avec son compte Google (Priority: P1) 🎯 MVP

**Goal**: l'administrateur initial se connecte avec Google ; tout compte absent de la liste blanche ou non vérifié est refusé en 403 ; déconnexion possible ; nom, email et rôle visibles.

**Independent Test**: base vide + `INITIAL_ADMIN_EMAIL` → connexion de ce compte en ADMIN ; autre compte → page 403 ; sans session → 401 (quickstart scénarios 1 à 4, 15).

### Tests for User Story 1 ⚠️

> Écrire ces tests d'abord et vérifier qu'ils échouent avant l'implémentation.

- [X] T036 [P] [US1] Test d'intégration du démarrage dans `src/test/java/fr/carsupervision/administration/InitialAdminInitializerIT.java` : création de l'ADMIN depuis `INITIAL_ADMIN_EMAIL` (email normalisé), membre USER existant remis en ADMIN, ancien email initial conservé, échec du démarrage sans variable et sans ADMIN, échec avec un email mal formé
- [X] T037 [P] [US1] Test unitaire de `WhitelistOidcUserService` dans `src/test/java/fr/carsupervision/security/WhitelistOidcUserServiceTest.java` : email inconnu → `access_denied`, `email_verified=false` → `access_denied`, casse et espaces ignorés, `last_login_at` mis à jour, nom affiché repris de Google seulement s'il est vide
- [X] T038 [P] [US1] Test d'intégration de l'accès dans `src/test/java/fr/carsupervision/security/AuthenticationIT.java` : `GET /api/me` sans session → 401 `UNAUTHENTICATED` sans données ; avec ADMIN initial → 200 (email, nom, rôle ADMIN) ; échec de connexion OAuth2 → 403 et contenu de `access-denied.html` ; `POST /logout` → 204 puis `GET /api/me` → 401 ; `POST` sans jeton CSRF → 403 ; validation du contrat sur chaque réponse `/api`

### Implementation for User Story 1

- [X] T039 [US1] Implémenter `InitialAdminInitializer` (`ApplicationRunner`) dans `src/main/java/fr/carsupervision/administration/InitialAdminInitializer.java` selon research R8, avec le message d'arrêt « INITIAL_ADMIN_EMAIL doit être défini tant qu'aucun administrateur n'existe »
- [X] T040 [US1] Implémenter `WhitelistOidcUserService` dans `src/main/java/fr/carsupervision/security/WhitelistOidcUserService.java` (research R3) et le brancher dans `SecurityConfig` via `oauth2Login().userInfoEndpoint().oidcUserService(...)`
- [X] T041 [P] [US1] Créer la page statique de refus `src/main/resources/static/access-denied.html` (français, autonome, lisible à 360 px, sans donnée, lien « Réessayer avec un autre compte »)
- [X] T042 [US1] Implémenter `AccessDeniedLoginHandler` (`AuthenticationFailureHandler`) dans `src/main/java/fr/carsupervision/security/AccessDeniedLoginHandler.java` : statut 403 et rendu de `access-denied.html`, et le brancher dans `SecurityConfig`
- [X] T043 [P] [US1] Créer `MemberMapper` (MapStruct, `Member` → DTO `CurrentMember` et `Member` du contrat) dans `src/main/java/fr/carsupervision/administration/member/MemberMapper.java`
- [X] T044 [US1] Implémenter `SessionController` (implémente l'interface générée `SessionApi`, opération `getCurrentMember`) dans `src/main/java/fr/carsupervision/security/SessionController.java`
- [X] T045 [P] [US1] Créer le contexte du membre connecté dans `frontend/src/auth/AuthContext.tsx` (requête `GET /api/me`, état connecté / non connecté sur 401 / révoqué sur 403) et la garde de routes `frontend/src/auth/RequireAuth.tsx`
- [X] T046 [P] [US1] Créer `frontend/src/pages/LoginPage.tsx` : bouton « Se connecter avec Google » qui navigue vers `/oauth2/authorization/google`
- [X] T047 [US1] Afficher le nom, l'email et le rôle du membre et un bouton « Se déconnecter » (`POST /logout` avec jeton CSRF, puis retour à l'écran de connexion) dans `frontend/src/components/AppLayout.tsx`
- [X] T048 [US1] Créer la page d'accueil provisoire `frontend/src/pages/HomePage.tsx` et câbler les routes publiques / protégées dans `frontend/src/App.tsx`
- [X] T049 [P] [US1] Test de composant `frontend/tests/LoginPage.test.tsx` : affichage de l'écran de connexion quand `/api/me` répond 401

**Checkpoint**: l'ADMIN initial se connecte et se déconnecte, tout autre compte est refusé (MVP).

---

## Phase 4: User Story 2 - Gérer les membres et leurs rôles (Priority: P2)

**Goal**: un ADMIN ajoute, modifie et retire des membres ; les changements prennent effet à la requête suivante ; il reste toujours au moins un ADMIN.

**Independent Test**: en ADMIN, ajouter un email, vérifier la connexion avec le bon rôle, puis le retirer et vérifier le refus (quickstart scénarios 5 à 7, 14).

### Tests for User Story 2 ⚠️

- [X] T050 [P] [US2] Test d'intégration `backend/app/src/test/java/fr/carsupervision/member/MembersIT.java` : liste triée, ajout (email normalisé, `createdAt`), doublon quelle que soit la casse → 409 `EMAIL_ALREADY_EXISTS`, email mal formé → 400 avec `fieldErrors.email`, modification du nom et du rôle, retrait → 204, membre inconnu → 404, toutes les opérations refusées à un USER → 403 ; validation du contrat sur chaque appel
- [X] T051 [P] [US2] Test d'intégration `backend/app/src/test/java/fr/carsupervision/member/LastAdminIT.java` : rétrograder ou retirer le seul ADMIN → 409 `LAST_ADMIN` ; deux ADMIN qui se rétrogradent en parallèle (deux threads, deux transactions) → un seul réussit et il reste exactement un ADMIN
- [X] T052 [P] [US2] Test d'intégration `backend/app/src/test/java/fr/carsupervision/security/ImmediateRevocationIT.java` : un USER avec session ouverte retiré par l'ADMIN → requête suivante 403 `MEMBER_REVOKED` et session invalidée ; un USER promu ADMIN → accès à `/api/members` dès la requête suivante ; un ADMIN rétrogradé → 403 dès la requête suivante

### Implementation for User Story 2

- [X] T053 [US2] Implémenter `MemberService` dans `backend/member/src/main/java/fr/carsupervision/member/MemberService.java` : `list` (tri par nom affiché puis email), `create` (normalisation, `email` obligatoire, format email valide, 320 caractères max ; `displayName` 100 caractères max, `trim`, vide = absent ; doublon → 409 `EMAIL_ALREADY_EXISTS`), `get` (404 `NOT_FOUND`), `update` (champs absents inchangés, chaîne vide efface le nom), `delete` (cascade des attributions) ; rétrogradation et retrait d'un ADMIN dans une transaction qui verrouille d'abord les ADMIN via `findAllByRoleForUpdate` puis refuse avec 409 `LAST_ADMIN` s'il n'en resterait aucun
- [X] T054 [US2] Implémenter `MembersController` (implémente `MembersApi` : `listMembers`, `createMember`, `getMember`, `updateMember`, `deleteMember`, chaque méthode annotée `@PreAuthorize("hasRole('ADMIN')")`) dans `backend/member/src/main/java/fr/carsupervision/member/MembersController.java`
- [X] T055 [P] [US2] Créer les hooks TanStack Query des membres dans `frontend/src/api/members.ts` (liste, détail, création, modification, retrait, invalidation du cache après mutation)
- [X] T056 [P] [US2] Créer `frontend/src/components/FormErrors.tsx` : affichage du `detail` Problem Details et des `fieldErrors` sous chaque champ, sans réinitialiser la saisie
- [X] T057 [US2] Créer `frontend/src/pages/admin/MembersPage.tsx` : liste des membres (email, nom, rôle, dernière connexion) en cartes sur mobile, formulaire d'ajout (email, nom affiché facultatif, rôle) avec react-hook-form
- [X] T058 [US2] Créer `frontend/src/pages/admin/MemberEditPage.tsx` : modification du nom et du rôle, retrait avec confirmation, affichage des refus `LAST_ADMIN` et `EMAIL_ALREADY_EXISTS` en français
- [X] T059 [US2] Ajouter les entrées de navigation et routes ADMIN (`/admin/membres`, `/admin/membres/:id`) masquées pour un USER, et la page « Accès refusé » interne pour les 403 dans `frontend/src/App.tsx` et `frontend/src/components/AppLayout.tsx`
- [X] T060 [P] [US2] Test de composant `frontend/tests/MembersPage.test.tsx` : erreur de doublon affichée sous le champ email sans perte de la saisie

**Checkpoint**: la famille peut être ajoutée ; retraits et changements de rôle immédiats ; jamais zéro ADMIN.

---

## Phase 5: User Story 3 - Gérer le parc de véhicules (Priority: P3)

**Goal**: un ADMIN crée, modifie, archive et réactive des véhicules ; immatriculation unique.

**Independent Test**: en ADMIN, créer deux véhicules, en modifier un, en archiver un et vérifier les listes active et archivée (quickstart scénarios 8, 11, 12, 13).

### Tests for User Story 3 ⚠️

- [X] T061 [P] [US3] Test d'intégration `backend/app/src/test/java/fr/carsupervision/vehicle/VehiclesAdminIT.java` : création (statut `ACTIVE`, immatriculation affichée en majuscules), modification de tous les champs, doublon d'immatriculation actif ou archivé écrit autrement (`ab-123 cd` contre `AB123CD`) → 409 `REGISTRATION_ALREADY_EXISTS`, validations par champ (champs obligatoires vides, kilométrage négatif ou > 2 000 000, année < 1900 ou dans le futur, VIN invalide) → 400 avec `fieldErrors`, archivage et réactivation idempotents, liste `ACTIVE` par défaut et liste `ARCHIVED`, véhicule inconnu → 404 pour l'ADMIN, création / modification / archivage refusés à un USER → 403 ; validation du contrat sur chaque appel

### Implementation for User Story 3

- [X] T062 [P] [US3] Créer les énumérations `VehicleStatus` (`ACTIVE`, `ARCHIVED`) et `EnergyType` (`PETROL`, `DIESEL`, `HYBRID`, `ELECTRIC`, `LPG`, `OTHER`) et l'entité JPA `Vehicle` (mappée sur `vehicle`, `created_at` / `updated_at` gérés à la création et à la modification) dans `backend/vehicle/src/main/java/fr/carsupervision/vehicle/`
- [X] T063 [P] [US3] Créer `VehicleRepository` dans `backend/vehicle/src/main/java/fr/carsupervision/vehicle/VehicleRepository.java` avec `findAllByStatusOrderByNicknameAsc`, `existsByRegistrationKey` et `existsByRegistrationKeyAndIdNot`
- [X] T064 [P] [US3] Créer `VehicleMapper` (MapStruct, `VehicleInput` → `Vehicle`, `Vehicle` → DTO `Vehicle`) dans `backend/vehicle/src/main/java/fr/carsupervision/vehicle/VehicleMapper.java`
- [X] T065 [US3] Implémenter `VehicleService` (partie ADMIN) dans `backend/vehicle/src/main/java/fr/carsupervision/vehicle/VehicleService.java` : `create` et `update` avec `nickname`, `make`, `model` obligatoires non vides après `trim` (50 caractères max), `registration` obligatoire (20 caractères max, clé normalisée de 2 à 20 caractères alphanumériques), `firstRegistrationYear` entre 1900 et l'année courante incluse, `initialMileageKm` entier entre 0 et 2 000 000, `vin` facultatif de 17 caractères alphanumériques sans `I`, `O`, `Q` stocké en majuscules, `energyType` facultatif ; unicité de `registration_key` parmi tous les véhicules → 409 `REGISTRATION_ALREADY_EXISTS` ; `archive` / `reactivate` idempotents ; `list(status)` triée par surnom ; `get` → 404 `NOT_FOUND` pour l'ADMIN
- [X] T066 [US3] Implémenter `VehiclesController` (implémente `VehiclesApi` : `listVehicles`, `getVehicle`, `createVehicle`, `updateVehicle`, `archiveVehicle`, `reactivateVehicle` ; `@PreAuthorize("hasRole('ADMIN')")` sur création, modification, archivage et réactivation ; `listVehicles` et `getVehicle` délèguent au service, qui renvoie pour l'instant une liste vide et 403 `VEHICLE_FORBIDDEN` à un USER) dans `backend/vehicle/src/main/java/fr/carsupervision/vehicle/VehiclesController.java`
- [X] T067 [P] [US3] Créer les hooks TanStack Query des véhicules dans `frontend/src/api/vehicles.ts` (listes active et archivée, détail, création, modification, archivage, réactivation)
- [X] T068 [US3] Créer `frontend/src/pages/admin/FleetPage.tsx` : parc courant et onglet « Archivés » avec action « Réactiver », cartes adaptées à 360 px
- [X] T069 [US3] Créer `frontend/src/pages/admin/VehicleFormPage.tsx` (création et modification) : surnom, marque, modèle, immatriculation, année, kilométrage de départ, VIN, type d'énergie (libellés français), erreurs par champ via `FormErrors`, action « Archiver » avec confirmation
- [X] T070 [US3] Ajouter les routes ADMIN `/admin/parc`, `/admin/parc/nouveau`, `/admin/parc/:id` et l'entrée de navigation dans `frontend/src/App.tsx` et `frontend/src/components/AppLayout.tsx`
- [X] T071 [P] [US3] Test de composant `frontend/tests/VehicleFormPage.test.tsx` : erreur `REGISTRATION_ALREADY_EXISTS` affichée sous l'immatriculation, saisie conservée

**Checkpoint**: le parc est géré par l'ADMIN ; un USER ne peut rien modifier.

---

## Phase 6: User Story 4 - Donner à chaque membre accès à ses véhicules (Priority: P4)

**Goal**: l'ADMIN attribue des véhicules à chaque USER ; un USER ne voit et n'obtient que ses véhicules actifs attribués, sans fuite d'information.

**Independent Test**: deux véhicules, un USER autorisé sur un seul → il ne voit et n'obtient que celui-là (quickstart scénarios 9, 10, 12, 13).

### Tests for User Story 4 ⚠️

- [ ] T072 [P] [US4] Test d'intégration `backend/app/src/test/java/fr/carsupervision/vehicle/VehicleAccessIT.java` : `GET` et `PUT /api/members/{id}/vehicles` (remplacement complet, liste vide acceptée), véhicule inconnu dans la liste → 400 avec `fieldErrors.vehicleIds`, membre ADMIN → 409 `MEMBER_IS_ADMIN`, membre inconnu → 404, opérations refusées à un USER → 403 ; validation du contrat
- [ ] T073 [P] [US4] Test d'intégration `backend/app/src/test/java/fr/carsupervision/vehicle/UserVehicleVisibilityIT.java` couvrant la table de lecture du data-model : USER autorisé sur A seulement → liste = [A] ; `GET` sur B non attribué, sur un véhicule archivé attribué et sur un identifiant inexistant → même 403 `VEHICLE_FORBIDDEN` sans détail ; `status=ARCHIVED` demandé par un USER → 403 ; USER sans attribution → liste vide ; retrait de A par l'ADMIN → A invisible dès la requête suivante ; archivage puis réactivation de A → A de nouveau visible ; ADMIN voit tout sans attribution

### Implementation for User Story 4

- [ ] T074 [P] [US4] Créer l'entité `VehicleAccess` (clé composée `member_id` + `vehicle_id`, `granted_at`) et `VehicleAccessRepository` (`findVehicleIdsByMemberId`, `deleteByMemberId`, `existsByMemberIdAndVehicleId`) dans `backend/vehicle/src/main/java/fr/carsupervision/vehicle/VehicleAccess.java` et `backend/vehicle/src/main/java/fr/carsupervision/vehicle/VehicleAccessRepository.java`
- [ ] T075 [US4] Implémenter `VehicleAccessGuard` dans `backend/vehicle/src/main/java/fr/carsupervision/vehicle/VehicleAccessGuard.java` : `assertReadable(CurrentMember, UUID vehicleId)` (ADMIN : tout ; USER : véhicule actif attribué, sinon 403 `VEHICLE_FORBIDDEN` identique pour inexistant, archivé ou non attribué), réutilisable par les futurs modules
- [ ] T076 [US4] Étendre `VehicleService` dans `backend/vehicle/src/main/java/fr/carsupervision/vehicle/VehicleService.java` : pour un USER, `list` ne renvoie que ses véhicules `ACTIVE` attribués (requête dédiée dans `VehicleRepository`), `status=ARCHIVED` → 403 `VEHICLE_FORBIDDEN`, `get` passe par `VehicleAccessGuard`
- [ ] T077 [US4] Implémenter `VehicleAccessService` dans `backend/vehicle/src/main/java/fr/carsupervision/vehicle/VehicleAccessService.java` : lecture des attributions, remplacement complet en une transaction (`maxItems` 50, identifiants uniques, tous existants sinon 400 `VALIDATION_ERROR` sur `vehicleIds`), refus 409 `MEMBER_IS_ADMIN` pour un ADMIN, 404 pour un membre inconnu
- [ ] T078 [US4] Implémenter `VehicleAccessController` (implémente `VehicleAccessApi` : `getMemberVehicleAccess`, `replaceMemberVehicleAccess`, annotés `@PreAuthorize("hasRole('ADMIN')")`) dans `backend/vehicle/src/main/java/fr/carsupervision/vehicle/VehicleAccessController.java`
- [ ] T079 [P] [US4] Ajouter les hooks d'attribution (`GET` / `PUT /members/{id}/vehicles`) dans `frontend/src/api/members.ts`
- [ ] T080 [US4] Ajouter dans `frontend/src/pages/admin/MemberEditPage.tsx` la section « Véhicules accessibles » (cases à cocher des véhicules actifs, véhicules archivés attribués signalés), visible uniquement pour un membre USER, enregistrée en un seul `PUT`
- [ ] T081 [US4] Créer `frontend/src/pages/VehiclesPage.tsx` (« Mes véhicules » pour un USER, parc courant pour un ADMIN) avec le message « Aucun véhicule ne vous est encore attribué. Demandez à un administrateur. » quand la liste d'un USER est vide, et en faire la page d'accueil à la place de `frontend/src/pages/HomePage.tsx`
- [ ] T082 [US4] Créer `frontend/src/pages/VehicleDetailPage.tsx` (route `/vehicules/:id`) avec affichage de la page « Accès refusé » interne sur 403 `VEHICLE_FORBIDDEN`
- [ ] T083 [P] [US4] Test de composant `frontend/tests/VehiclesPage.test.tsx` : message « aucun véhicule attribué » pour un USER sans véhicule

**Checkpoint**: toutes les histoires fonctionnent ; un USER ne voit que ses véhicules.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: livrable, documentation et vérifications finales.

- [X] T084 Créer le `Dockerfile` multi-étapes à la racine : étape Node LTS (`npm ci`, `npm run build` dans `frontend/`), étape JDK 21 (`./gradlew bootJar` avec `frontend/dist` copié), image finale JRE 21 non root exposant 8080 ; ajouter `.dockerignore`
- [ ] T085 [P] Test d'intégration `backend/app/src/test/java/fr/carsupervision/common/StaticAndDocsIT.java` : `/` et une route SPA (`/admin/parc`) servent `index.html` sans session, `/openapi.yaml` et `/swagger-ui/index.html` exigent une session, `/actuator/health` est public
- [ ] T086 [P] Écrire le `README.md` à la racine (en français) : présentation, prérequis, création du client OAuth Google, variables d'environnement, lancement local et tests, renvoi vers `specs/001-acces-membres-flotte/quickstart.md`
- [ ] T087 Vérifier que chaque méthode des contrôleurs ADMIN porte `@PreAuthorize("hasRole('ADMIN')")` et qu'aucun `@RequestMapping` n'existe hors des interfaces générées (revue de `backend/*/src/main/java/fr/carsupervision/`) ; corriger les écarts
- [ ] T088 Vérifier l'affichage à 360 px de large de chaque écran (connexion, accès refusé, mes véhicules, détail, membres, édition membre, parc, formulaire véhicule) et corriger les débordements dans `frontend/src/`
- [ ] T089 Dérouler les 16 scénarios de `specs/001-acces-membres-flotte/quickstart.md` et corriger les écarts constatés
- [ ] T090 Vérifier que la CI `.github/workflows/ci.yml` passe sur la PR d'implémentation (génération OpenAPI, build, tests, image Docker)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)** : aucune dépendance.
- **Foundational (Phase 2)** : dépend du Setup ; bloque toutes les histoires.
- **US1 (Phase 3)** : dépend de la Phase 2. C'est le MVP.
- **US2 (Phase 4)** : dépend de la Phase 2 ; pour une validation manuelle de bout en bout, il faut US1 (connexion).
- **US3 (Phase 5)** : dépend de la Phase 2 ; indépendante de US2.
- **US4 (Phase 6)** : dépend de US3 (véhicules) ; l'écran d'attribution s'ajoute à `MemberEditPage` de US2 (T080 après T058).
- **Polish (Phase 7)** : après les histoires souhaitées.

### Within Each User Story

- Tests d'abord, en échec avant l'implémentation.
- Entités → repositories → services → contrôleurs → frontend.
- Les contrôleurs implémentent toutes les opérations de l'interface générée de leur tag (`skipDefaultInterface=true`).

### Dépendances notables entre tâches

- T003 → T004 → toute tâche qui implémente une interface générée (T044, T054, T066, T078).
- T012 → T033 → tous les hooks frontend.
- T015 à T018 → T014 (le master inclut les changesets existants).
- T024 → T026, T040, T042 (branchés dans la configuration de sécurité).
- T053 → T054 ; T065 → T066 ; T075 → T076 ; T077 → T078.
- T058 → T080 ; T048 → T081.

### Parallel Opportunities

- Phase 1 : T005, T006, T007, T009, T010 en parallèle.
- Phase 2 : T015 à T018 ; T019 à T022 ; T025, T027, T028 ; T031, T032 ; T033, T034.
- Après la Phase 2, US1 et US3 peuvent avancer en parallèle (packages différents) ; US2 en parallèle de US3 aussi.
- Dans chaque histoire, les tests marqués [P] et les fichiers frontend marqués [P].

---

## Parallel Example: User Story 1

```bash
# Tests de US1 en parallèle :
Task: "Test d'intégration du démarrage dans src/test/java/fr/carsupervision/administration/InitialAdminInitializerIT.java"
Task: "Test unitaire de WhitelistOidcUserService dans src/test/java/fr/carsupervision/security/WhitelistOidcUserServiceTest.java"
Task: "Test d'intégration de l'accès dans src/test/java/fr/carsupervision/security/AuthenticationIT.java"

# Puis, en parallèle de l'implémentation backend :
Task: "Page statique src/main/resources/static/access-denied.html"
Task: "Contexte du membre connecté frontend/src/auth/AuthContext.tsx"
Task: "Écran de connexion frontend/src/pages/LoginPage.tsx"
```

## Parallel Example: User Story 4

```bash
Task: "Test d'intégration src/test/java/fr/carsupervision/administration/VehicleAccessIT.java"
Task: "Test d'intégration src/test/java/fr/carsupervision/vehicle/UserVehicleVisibilityIT.java"
Task: "Entité VehicleAccess et VehicleAccessRepository dans src/main/java/fr/carsupervision/vehicle/"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 : Setup.
2. Phase 2 : Foundational (bloquante).
3. Phase 3 : US1.
4. **STOP and VALIDATE** : scénarios 1 à 4 et 15 du quickstart ; l'ADMIN initial se connecte, les autres sont refusés.

### Incremental Delivery

Une PR par étape, chacune avec CI verte :

1. Setup + Foundational → socle prêt.
2. + US1 → connexion sécurisée (MVP).
3. + US2 → ouverture à la famille.
4. + US3 → parc de véhicules.
5. + US4 → attributions et vue « mes véhicules ».
6. + Polish → image Docker, README, validation complète.

---

## Notes

- 90 tâches : Setup 13, Foundational 22, US1 14, US2 11, US3 11, US4 12, Polish 7.
- Le code généré (`build/generated/openapi`, `frontend/src/api/generated`) n'est jamais commité ni édité.
- Toute évolution d'API commence par `src/main/resources/openapi/openapi.yaml` (principe I).
- Committer après chaque tâche ou groupe logique ; s'arrêter à chaque checkpoint pour valider l'histoire.
