# carSupervision Constitution

Application web privée de suivi de l'entretien et des révisions des véhicules de la famille.
Ce document fixe les règles non négociables que chaque spécification, plan, liste de tâches
et implémentation Spec Kit doit respecter.

## Core Principles

### I. API Contract-First (NON NÉGOCIABLE)

- Toute fonctionnalité qui expose ou consomme une API DOIT commencer par la définition du contrat
  dans `src/main/resources/openapi/openapi.yaml` (OpenAPI 3.x, format YAML) : endpoints,
  paramètres, schémas de DTOs et codes d'erreur.
- Les interfaces serveur (`*Api.java`) et les DTOs DOIVENT être générés automatiquement lors du
  build Gradle par la tâche `openApiGenerate` (plugin `org.openapi.generator`), vers
  `build/generated`.
- Le code généré NE DOIT JAMAIS être édité à la main ni commité dans `src/`.
- Les contrôleurs Spring DOIVENT uniquement implémenter ces interfaces générées ; aucun endpoint
  REST ne peut être déclaré hors du contrat.
- Le frontend DOIT consommer les types et clients générés depuis ce même `openapi.yaml` ; aucun
  type d'API n'est redéfini à la main côté frontend.
- La documentation interactive (Swagger UI / Springdoc) DOIT être exposée à partir du contrat.

**Justification** : un contrat unique et versionné garantit la cohérence entre backend,
frontend et documentation, et rend chaque changement d'API visible en revue.

### II. Monolithe modulaire en couches

- Le backend est un monolithe modulaire : un seul déployable, découpé en modules métier
  (véhicules, entretiens, kilométrages, échéances, justificatifs, administration).
- Chaque module respecte une séparation claire Controller / Service / Repository / Mappers.
- Les entités JPA NE DOIVENT JAMAIS être exposées directement par l'API : la conversion
  entité ↔ DTO passe obligatoirement par un mapper.
- La logique métier vit dans les services, pas dans les contrôleurs ni dans les repositories.

**Justification** : une architecture simple à déployer, mais structurée pour rester lisible et
évolutive.

### III. Sécurité et authentification déléguée à Google

- L'authentification DOIT être déléguée à Google (OAuth2 / OpenID Connect) via Spring Security ;
  l'application ne stocke aucun mot de passe.
- Seuls les comptes Google dont l'email figure dans la liste blanche peuvent accéder à
  l'application. Tout compte Google non autorisé DOIT recevoir une erreur `403 Forbidden`.
- La liste blanche des emails autorisés DOIT être persistée en base PostgreSQL.
- Au démarrage, la liste blanche DOIT être initialisée à partir de la variable d'environnement
  `INITIAL_ADMIN_EMAIL`, qui garantit l'accès du premier administrateur avec le rôle `ADMIN`.
- Les secrets (identifiants OAuth, accès base, accès bucket) sont fournis par variables
  d'environnement et ne sont jamais commités.

**Justification** : déléguer l'authentification à un fournisseur éprouvé évite de gérer des
identifiants, et la liste blanche restreint l'accès au seul cercle familial.

### IV. Gestion des rôles et administration (RBAC)

- L'application distingue exactement deux rôles :
  - `ADMIN` : gestion de la liste blanche des utilisateurs autorisés, attribution des rôles,
    gestion globale du parc de véhicules et des référentiels.
  - `USER` : consultation et saisie des entretiens et des kilométrages sur les véhicules
    qui lui sont autorisés.
- Les endpoints d'administration DOIVENT être strictement protégés côté backend via Spring
  Security (`@PreAuthorize("hasRole('ADMIN')")`). Un contrôle côté frontend seul ne suffit jamais.
- Un `USER` NE DOIT PAS pouvoir lire ni modifier les données d'un véhicule qui ne lui est pas
  autorisé.

**Justification** : les droits sont simples mais doivent être garantis par le serveur, seule
source de vérité.

### V. Tests ciblés sur les règles critiques

- Les tests (JUnit 5) DOIVENT couvrir par des tests d'intégration les règles métier critiques :
  - le calcul des prochaines échéances d'entretien (par date et par kilométrage) ;
  - les règles d'accès RBAC (liste blanche, `403 Forbidden`, endpoints `ADMIN`, accès d'un
    `USER` limité à ses véhicules autorisés).
- La conformité de l'implémentation avec le contrat OpenAPI DOIT être validée par des tests.
- Une fonctionnalité touchant ces règles n'est pas terminée tant que ses tests ne passent pas
  dans la CI.

**Justification** : l'effort de test est concentré là où une erreur coûte cher (échéance
manquée, fuite d'accès, rupture de contrat).

### VI. Pragmatisme et simplicité

- Éviter l'over-engineering : pas de Kafka, pas de Kubernetes, pas de cache distribué, ni
  d'autre composant d'infrastructure non justifié par un besoin réel du projet.
- Aucune logique multi-tenant complexe : l'application sert une seule famille.
- Privilégier la lisibilité et la maintenabilité du code.
- Chaque étape Spec Kit (spec, plan, tâches) DOIT produire une documentation claire.
- Toute complexité supplémentaire DOIT être justifiée dans le plan (section « Constitution
  Check » / suivi de complexité).

**Justification** : un projet familial doit rester simple à comprendre, à faire tourner et à
faire évoluer par une seule personne.

## Périmètre et utilisateurs

- **Cible** : application web responsive, mobile-first pour la saisie rapide au garage ou à la
  station, et confortable sur desktop.
- **Volumétrie** : usage strictement privé et familial, 2 à 5 utilisateurs maximum et 2 à 5
  véhicules. Aucune logique multi-tenant complexe n'est requise.
- **Fonctionnalités clés** :
  - suivi du kilométrage ;
  - historique des entretiens et révisions (vidanges, filtres, pneus, contrôle technique,
    assurance) ;
  - alertes d'échéances, par date ou par kilométrage ;
  - stockage des justificatifs et factures (PDF, images) ;
  - espace d'administration pour gérer les membres de la famille, leurs rôles et le parc de
    véhicules.

## Stack technique

- **Backend** : Java 21 + Spring Boot 3 (API REST), Spring Data JPA / Hibernate, Liquibase pour
  les migrations de schéma, Jackson. Toute évolution du schéma passe par un changeset Liquibase.
- **Build** : Gradle avec Kotlin DSL (`build.gradle.kts`).
- **Contrat d'API** : OpenAPI 3.x en YAML (`src/main/resources/openapi/openapi.yaml`), génération
  des interfaces `*Api.java` et des DTOs au build (`org.openapi.generator` vers
  `build/generated`), documentation interactive Swagger UI / Springdoc.
- **Frontend** : ReactJS en TypeScript, UI moderne, épurée et rapide (par exemple Tailwind CSS +
  shadcn/ui), consommant les types et clients générés depuis `openapi.yaml`.
- **Base de données** : PostgreSQL.
- **Stockage de fichiers** : abstraction de stockage permettant le stockage local sur disque en
  développement et un bucket GCP Cloud Storage / S3 en production, sans changement du code
  métier.

## Environnement de développement, hébergement et CI/CD

- **Développement local** : Docker Compose pour les services d'infrastructure (PostgreSQL),
  compatible Linux et Rancher Desktop. Le backend et le frontend s'exécutent nativement pour un
  cycle de rechargement rapide.
- **Hébergement et déploiement** : déploiement simple et économique, soit en auto-hébergement
  via Docker Compose sur un petit VPS, soit en déploiement conteneurisé sur GCP Cloud Run avec
  une base managée.
- **CI/CD** : pipeline léger qui enchaîne génération OpenAPI, build Gradle, tests unitaires et
  d'intégration, puis construction de l'image Docker. Une PR ne peut être fusionnée que si ce
  pipeline passe.

## Governance

- Cette constitution prévaut sur toute autre pratique du projet. Chaque spec, plan et liste de
  tâches Spec Kit DOIT vérifier sa conformité (« Constitution Check » du plan) et toute
  dérogation DOIT être justifiée explicitement.
- Toute PR et toute revue DOIVENT vérifier le respect des principes, en particulier
  Contract-First (I), la protection des endpoints `ADMIN` (IV) et la présence des tests
  critiques (V).
- Modification de la constitution : via `/speckit-constitution` dans une PR dédiée, approuvée
  par le mainteneur du projet (Said), avec mise à jour de la version et de la date d'amendement.
- Versionnement sémantique :
  - MAJOR : suppression ou redéfinition incompatible d'un principe ou d'une règle de gouvernance ;
  - MINOR : ajout d'un principe ou d'une section, ou extension notable des règles ;
  - PATCH : clarifications, reformulations, corrections sans effet sur le fond.

**Version**: 1.0.0 | **Ratified**: 2026-11-19 | **Last Amended**: 2026-11-19
