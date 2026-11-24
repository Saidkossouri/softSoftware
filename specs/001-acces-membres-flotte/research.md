# Recherche (Phase 0) : Accès sécurisé, membres et parc de véhicules

**Feature**: `001-acces-membres-flotte` | **Date**: 2026-11-23 | **Plan**: [plan.md](./plan.md)

Ce document tranche les choix techniques laissés ouverts par la spécification et la
constitution. Chaque décision suit le format : Décision / Justification / Alternatives écartées.
Aucun point « NEEDS CLARIFICATION » ne reste ouvert.

---

## R1. Organisation du dépôt et du livrable

> **Révision du 2026-11-24 (décision du mainteneur)** : le dépôt est désormais un monolithe
> multi-modules Gradle (`backend/<module>/`, voir [plan.md](./plan.md)) et chaque brique a sa
> propre image : `backend/Dockerfile` (jar Spring Boot, API seule) et `frontend/Dockerfile`
> (application construite servie par nginx, qui relaie `/api`, `/oauth2`, `/login`, `/logout`
> vers le backend). L'origine reste unique côté navigateur (nginx en reverse proxy) : pas de
> CORS, même cookie de session. Coût accepté : deux conteneurs à déployer au lieu d'un (sur
> Cloud Run, deux services). La décision d'origine est conservée ci-dessous pour mémoire.

**Décision** : projet Gradle (Kotlin DSL) à la racine du dépôt pour le backend
(`src/main/java`, `src/main/resources/openapi/openapi.yaml`), application React dans
`frontend/`. Un seul déployable : le build Docker compile le frontend puis l'embarque dans les
ressources statiques du jar Spring Boot (`static/`). Une seule image Docker, une seule URL.

**Justification** : la constitution impose le chemin `src/main/resources/openapi/openapi.yaml`
(donc le projet Gradle à la racine) et un monolithe modulaire. Servir le frontend depuis le
backend donne une origine unique : pas de CORS, cookie de session simple, une seule ressource
à déployer sur VPS ou Cloud Run.

**Alternatives écartées** :
- Deux images (nginx + backend) : un conteneur de plus à exploiter, CORS ou reverse proxy à
  configurer, sans bénéfice pour 2 à 5 utilisateurs.
- Plugin Gradle Node (`com.github.node-gradle`) pour piloter npm depuis Gradle : ajoute une
  couche ; le Dockerfile multi-étapes et la CI enchaînent déjà les deux builds simplement.

## R2. Modèle d'authentification Google

**Décision** : Spring Security `oauth2Login()` côté serveur (flux « authorization code » OpenID
Connect avec Google), session HTTP côté serveur identifiée par un cookie `HttpOnly`,
`Secure`, `SameSite=Lax`. Le frontend ne manipule aucun jeton : il déclenche la connexion en
naviguant vers `/oauth2/authorization/google` et appelle l'API avec le cookie de session.

**Justification** : c'est le schéma « Backend for Frontend » recommandé pour une SPA servie par
le même backend ; les jetons Google ne transitent jamais par le navigateur, la constitution
(principe III) est respectée sans code d'authentification maison, et aucun mot de passe n'est
stocké (FR-001).

**Alternatives écartées** :
- SPA qui obtient un ID token Google et l'envoie en `Bearer` (backend en resource server) :
  jetons exposés au JavaScript, renouvellement à gérer côté navigateur, plus de code.
- Émettre nos propres JWT après la connexion Google : réinvente une gestion de session et
  complique la révocation immédiate exigée par FR-007.

## R3. Liste blanche, compte non vérifié et refus 403

**Décision** :
1. Un `OidcUserService` personnalisé s'exécute à la fin du flux Google. Il normalise l'email
   (`trim` + minuscules), vérifie le claim `email_verified = true`, cherche le membre en base.
   Absent ou non vérifié : il lève une `OAuth2AuthenticationException` avec le code
   `access_denied`.
2. Le `AuthenticationFailureHandler` répond **HTTP 403** avec une page HTML statique en
   français (`access-denied.html`) qui explique de demander l'accès à un administrateur. Aucune
   session authentifiée n'est créée.
3. Si le membre est connu, le service met à jour `last_login_at` et, si le nom affiché est vide,
   le reprend du claim `name` de Google.

**Justification** : le refus a lieu avant qu'une session authentifiée n'existe ; la réponse
est un vrai 403 (FR-002, SC-002), et la page ne contient aucune donnée.

**Alternatives écartées** :
- Rediriger vers une route de la SPA : la réponse serait un 302 puis 200, pas un 403.
- Accepter la connexion puis bloquer sur chaque appel d'API : une session authentifiée
  existerait pour un compte non autorisé, ce qui est fragile.

## R4. Réévaluation des droits à chaque requête (FR-007, SC-006)

**Décision** : la session ne contient que l'identité Google (email). Un filtre
`CurrentMemberFilter`, placé après l'authentification, recharge le membre par email à **chaque
requête** vers `/api/**` :
- membre introuvable (retiré) : session invalidée, réponse 403 (Problem Details,
  code `MEMBER_REVOKED`) ;
- membre trouvé : l'`Authentication` de la requête est remplacée par une
  `MemberAuthentication` portant l'autorité `ROLE_ADMIN` ou `ROLE_USER` lue en base et
  l'identifiant du membre.

Les contrôles `@PreAuthorize("hasRole('ADMIN')")` et les filtres d'accès véhicule utilisent
donc toujours le rôle et les attributions actuels.

**Justification** : avec 2 à 5 membres, une lecture par clé primaire indexée par requête est
négligeable, et c'est le moyen le plus simple de garantir un effet immédiat sans cache à
invalider.

**Alternatives écartées** :
- Rôles figés dans la session au login + invalidation des sessions du membre modifié :
  nécessite d'indexer les sessions par utilisateur, plus complexe et plus fragile.
- Cache local avec expiration courte : ne garantit pas « dès la requête suivante ».

## R5. Persistance des sessions

**Décision** : Spring Session JDBC dans la base PostgreSQL existante (tables créées par un
changeset Liquibase). Durée d'inactivité : 30 jours. La déconnexion (`POST /logout`) supprime
la session en base.

**Justification** : sur Cloud Run, les instances s'arrêtent (mise à l'échelle à zéro) et
changent à chaque déploiement ; des sessions en mémoire obligeraient la famille à se
reconnecter très souvent, surtout sur téléphone. PostgreSQL est déjà là : aucun composant
d'infrastructure ajouté (principe VI).

**Alternatives écartées** :
- Sessions en mémoire Tomcat : perdues à chaque redémarrage ou mise à l'échelle.
- Redis : composant d'infrastructure supplémentaire non justifié.

## R6. Protection CSRF et réponses non authentifiées

**Décision** :
- CSRF activé avec `CookieCsrfTokenRepository.withHttpOnlyFalse()` et le gestionnaire SPA
  recommandé par Spring Security 6 ; le client frontend renvoie le cookie `XSRF-TOKEN` dans
  l'en-tête `X-XSRF-TOKEN` pour toute requête modifiante.
- `/api/**` sans session : **401** (Problem Details, code `UNAUTHENTICATED`), jamais de
  redirection HTML. La SPA affiche alors l'écran « Se connecter avec Google » (FR-006,
  scénario US1-4).
- Session valide mais droits insuffisants : **403** (Problem Details).

**Justification** : l'authentification par cookie impose une protection CSRF ; le
401/403 JSON sur l'API permet à la SPA de réagir proprement, et un appel direct au service
n'obtient aucune donnée (SC-003).

**Alternatives écartées** : désactiver CSRF en s'appuyant uniquement sur `SameSite=Lax` :
protection jugée insuffisante pour des requêtes modifiantes.

## R7. Pages publiques et SPA

**Décision** : sont accessibles sans session : la coquille de la SPA (`index.html`, fichiers
JS/CSS), `access-denied.html`, les points d'entrée OAuth2 de Spring Security et
`/actuator/health`. Toute donnée passe par `/api/**`, qui exige une session et un membre
autorisé. Le repli des routes SPA vers `index.html` est configuré par un `WebMvcConfigurer`
(résolution de ressources statiques), pas par un contrôleur REST.

**Justification** : la coquille de l'application ne contient aucune donnée ; FR-006 est
satisfait car toute page affichant des données dépend d'appels `/api/**` protégés.

**Alternatives écartées** : protéger aussi `index.html` par redirection vers Google : la page
de refus et l'écran de connexion deviendraient difficiles à servir proprement.

## R8. Initialisation de l'administrateur (FR-004)

**Décision** : un `ApplicationRunner` (`InitialAdminInitializer`) s'exécute après les
migrations Liquibase :
- `INITIAL_ADMIN_EMAIL` renseigné et valide : l'email normalisé est créé en ADMIN, ou passé en
  ADMIN s'il existait en USER. Les autres membres ne sont pas modifiés (un ancien email
  initial reste en place).
- variable absente ou vide et aucun ADMIN en base : exception, l'application s'arrête avec le
  message « INITIAL_ADMIN_EMAIL doit être défini tant qu'aucun administrateur n'existe ».
- variable présente mais email mal formé : arrêt avec un message explicite.

**Justification** : couvre les trois cas limites de la spec et garantit SC-001 (aucune
manipulation en base).

**Alternatives écartées** : un changeset Liquibase paramétré : moins lisible, et la règle
« remettre en ADMIN » relève de la logique métier, pas du schéma.

## R9. Règle « au moins un ADMIN » sous concurrence (FR-012, SC-007)

**Décision** : toute opération qui retire ou rétrograde un ADMIN s'exécute dans une
transaction qui verrouille d'abord les lignes ADMIN
(`SELECT … FROM member WHERE role = 'ADMIN' FOR UPDATE`, via `@Lock(PESSIMISTIC_WRITE)`),
puis compte les ADMIN restants. S'il n'en resterait aucun : 409 `LAST_ADMIN`.

**Justification** : deux transactions concurrentes qui rétrogradent chacune un ADMIN différent
sont sérialisées par le verrou ; PostgreSQL réévalue le `WHERE` après l'attente, donc la
seconde voit le nombre réel d'ADMIN et échoue. Simple, sans niveau d'isolation particulier.

**Alternatives écartées** :
- Isolation `SERIALIZABLE` : impose une logique de reprise sur erreur de sérialisation.
- Trigger PostgreSQL : règle métier cachée dans la base, moins lisible et moins testable.

## R10. Normalisation et unicité

**Décision** :
- Email membre : stocké normalisé (`trim`, minuscules) ; contrainte `UNIQUE` sur la colonne.
- Immatriculation : la saisie (`registration`) est conservée pour l'affichage, nettoyée des
  espaces de bord et mise en majuscules ; une colonne `registration_key` (majuscules, sans
  espaces ni tirets) porte la contrainte `UNIQUE`, actifs et archivés confondus (FR-016).
- Le service vérifie l'unicité pour renvoyer un 409 lisible ; la contrainte en base reste le
  garde-fou en cas de course (violation traduite aussi en 409).

**Justification** : unicité garantie par la base, message clair par le service.

**Alternatives écartées** : index fonctionnel `UNIQUE (upper(replace(...)))` : moins lisible
et plus difficile à refléter dans le mapping JPA.

## R11. Retrait d'un membre

**Décision** : retirer un membre supprime sa ligne `member` et, en cascade, ses lignes
`vehicle_access`. Aucun historique n'est conservé (l'audit est hors périmètre).

**Justification** : correspond à « retirer son accès » ; un nouvel ajout du même email repart
de zéro. Quand la fonctionnalité d'entretiens enregistrera l'auteur d'une saisie, elle
conservera une copie du nom de l'auteur ou réévaluera ce choix (archivage des membres).

**Alternatives écartées** : suppression logique (`removed_at`) : complexifie toutes les
requêtes et l'unicité de l'email, sans besoin exprimé dans cette fonctionnalité.

## R12. Contrôle d'accès aux véhicules (FR-020 à FR-022)

**Décision** :
- Table d'association `vehicle_access(member_id, vehicle_id)` ; l'attribution est remplacée en
  bloc par `PUT /api/members/{id}/vehicles` (la liste de cases à cocher de l'écran).
- Les attributions ne s'appliquent qu'aux membres USER : les définir pour un ADMIN renvoie 409
  `MEMBER_IS_ADMIN`. Elles sont conservées si le membre
  devient ADMIN (sans effet) et redeviennent effectives s'il redevient USER, comme pour un
  véhicule archivé puis réactivé.
- Le `VehicleService` applique la règle dans la couche service : un ADMIN voit tout ; un USER
  ne reçoit que les véhicules actifs attribués. Pour un USER, un véhicule inexistant, archivé
  ou non attribué donne la même réponse **403 `VEHICLE_FORBIDDEN`**, sans aucun détail, afin de
  ne pas révéler son existence. Un ADMIN reçoit 404 pour un véhicule inexistant.

**Justification** : règle concentrée en un seul endroit, réutilisable par les futurs modules
(kilométrages, entretiens) via `VehicleAccessGuard.assertReadable(member, vehicleId)`.

**Alternatives écartées** : filtres Hibernate ou sécurité au niveau des lignes PostgreSQL :
plus puissants mais disproportionnés pour cette volumétrie.

## R13. Format des erreurs et messages en français (FR-025)

**Décision** : toutes les erreurs de `/api/**` suivent RFC 9457 (Problem Details,
`application/problem+json`) avec les extensions `code` (code stable, ex. `EMAIL_ALREADY_EXISTS`)
et `fieldErrors` (liste `{field, message}`) pour les erreurs de validation. Le champ `detail`
est un message en français destiné à l'utilisateur. Un `@RestControllerAdvice` unique produit
ces réponses ; le schéma `Problem` est défini dans le contrat OpenAPI.

**Justification** : format standard supporté nativement par Spring 6 (`ProblemDetail`) ; le
frontend affiche `detail` et place `fieldErrors` sous chaque champ sans perdre la saisie.

## R14. Génération du code depuis le contrat

**Décision** :
- Backend : plugin `org.openapi.generator`, générateur `spring`, options `interfaceOnly=true`,
  `useSpringBoot3=true`, `useTags=true`, `skipDefaultInterface=true`,
  `useBeanValidation=true`, `openApiNullable=false`, sortie `build/generated/openapi`,
  packages `fr.carsupervision.api` et `fr.carsupervision.api.model`. `compileJava` dépend de
  `openApiGenerate`.
- Frontend : `openapi-typescript` génère `frontend/src/api/generated/schema.d.ts` depuis le même
  fichier (script `npm run generate:api`, lancé avant `dev` et `build`), consommé par le client
  typé `openapi-fetch`. Le fichier généré est ignoré par git.
- Documentation : Springdoc sert Swagger UI à partir du fichier `openapi.yaml` exposé tel quel
  (`springdoc.swagger-ui.url=/openapi.yaml`), réservé aux membres connectés.

**Justification** : respecte le principe I à la lettre ; `openapi-fetch` est un client très
léger (pas de classes générées à maintenir) dont les types viennent exclusivement du contrat.

**Alternatives écartées** : `openapi-generator` `typescript-fetch` pour le frontend : génère
beaucoup de code et de classes ; `orval` : utile avec TanStack Query mais plus de configuration.

## R15. Stratégie de test (principe V)

**Décision** :
- Tests d'intégration JUnit 5 avec `@SpringBootTest`, MockMvc et **Testcontainers PostgreSQL**
  (vraie base : contraintes `UNIQUE`, `FOR UPDATE`, Liquibase réellement exécuté).
- Simulation des comptes Google avec le post-processeur MockMvc `oidcLogin()` de
  `spring-security-test` (email, `email_verified`), plus un test unitaire de
  l'`OidcUserService` pour le refus des comptes non autorisés ou non vérifiés.
- Conformité au contrat : `swagger-request-validator-mockmvc` (Atlassian) valide chaque
  requête et réponse des tests d'intégration contre `openapi.yaml`.
- Couverture obligatoire : liste blanche et 403, 401 sans session, endpoints ADMIN refusés au
  USER, accès véhicule limité, effet immédiat d'un retrait, règle du dernier ADMIN y compris en
  concurrence (deux threads), démarrage sans `INITIAL_ADMIN_EMAIL`.
- Frontend : Vitest + Testing Library pour quelques tests de composants (formulaires et
  affichage des erreurs). Pas de tests de bout en bout navigateur dans cette fonctionnalité.

**Justification** : effort concentré sur les règles RBAC critiques, comme l'exige la
constitution ; Testcontainers fonctionne sur les runners GitHub Actions et avec Docker ou
Rancher Desktop en local.

**Alternatives écartées** : H2 en mémoire : comportements différents de PostgreSQL (verrous,
contraintes), donc tests trompeurs sur les règles critiques.

## R16. Frontend

**Décision** : React + TypeScript avec Vite, React Router, TanStack Query (cache et
invalidation après mutation), Tailwind CSS + shadcn/ui, `react-hook-form` pour les
formulaires. Interface en français, conçue d'abord pour 360 px de large (FR-024).

**Justification** : pile demandée par la constitution, outils standard et légers. La
validation de référence reste côté serveur ; le frontend ne fait que des contrôles de confort.

## R17. Environnement local, CI et déploiement

**Décision** :
- `compose.yaml` : PostgreSQL 17 uniquement. Backend lancé par `./gradlew bootRun`, frontend
  par `npm run dev` (Vite, port 5173) avec un proxy vers le backend (port 8080) pour `/api`,
  `/oauth2`, `/login`, `/logout` et `/openapi.yaml`, en conservant l'en-tête `Host` : l'URI de
  redirection Google en développement est `http://localhost:5173/login/oauth2/code/google`.
- `Dockerfile` multi-étapes : build frontend (Node LTS), build Gradle (JDK 21), image finale
  JRE 21. En production, `server.forward-headers-strategy=framework` pour construire la bonne
  URI de redirection derrière le proxy (Cloud Run ou reverse proxy du VPS).
- CI GitHub Actions (`.github/workflows/ci.yml`) : génération OpenAPI + build + tests Gradle
  (Testcontainers), génération des types + lint + tests + build frontend, puis construction de
  l'image Docker (sans publication à ce stade).
- Variables d'environnement : `INITIAL_ADMIN_EMAIL`, `GOOGLE_CLIENT_ID`,
  `GOOGLE_CLIENT_SECRET`, `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`. Un fichier
  `.env.example` documente les noms ; `.env` est ignoré par git.

**Justification** : conforme à la section « Environnement de développement, hébergement et
CI/CD » de la constitution ; le choix entre VPS et Cloud Run n'est pas nécessaire pour cette
fonctionnalité car l'image est la même.

## R18. Identifiants

**Décision** : clés primaires `UUID` générées par l'application, exposées dans l'API.

**Justification** : identifiants non devinables et non séquentiels, ce qui évite de révéler le
nombre de véhicules ou de membres ; aucun coût notable à cette volumétrie.

**Alternatives écartées** : `bigint` auto-incrémenté : suffisant fonctionnellement mais
énumérable ; l'accès reste de toute façon contrôlé côté serveur.

## R19. Mappers entité ↔ DTO

**Décision** : MapStruct (processeur d'annotations à la compilation) pour les conversions entre
entités JPA et DTOs générés, un mapper par module.

**Justification** : le principe II impose un mapper ; MapStruct produit du code simple,
vérifié à la compilation (un champ du contrat non mappé fait échouer le build), sans
réflexion à l'exécution.

**Alternatives écartées** : mappers écrits à la main : acceptables à cette taille, mais
répétitifs et sans alerte quand le contrat ajoute un champ ; ModelMapper : réflexion à
l'exécution, erreurs découvertes tardivement.
