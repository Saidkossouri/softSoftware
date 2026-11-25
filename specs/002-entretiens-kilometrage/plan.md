# Implementation Plan: Kilométrage, entretiens, échéances et justificatifs

**Branch**: `feature/002-plan` | **Date**: 2026-11-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-entretiens-kilometrage/spec.md`

## Summary

Deuxième fonctionnalité de carSupervision : sur les véhicules auxquels il a accès, un membre
relève le kilométrage, enregistre les entretiens (types d'entretien, coût, garage), consulte
l'historique et les dépenses, définit les échéances (par kilométrage et/ou par durée, ou à
date fixe, dont assurance, vignette et visite technique) que l'accueil signale, et joint les
factures (PDF, photos). L'ADMIN gère le catalogue des types d'entretien et leurs fréquences
recommandées, livré pré-rempli avec des valeurs adaptées au Maroc.

Approche technique : trois nouveaux modules Gradle (`maintenance`, `schedule`, `document`)
au-dessus de `vehicle`, qui réutilisent la règle d'accès de 001 (`VehicleAccessGuard`). Le
kilométrage actuel est recalculé à chaque saisie et stocké sur le véhicule ; la cohérence
chronologique est vérifiée sous un verrou par véhicule. Les échéances sont calculées à la
lecture par une classe pure, testée sur une table de cas puis de bout en bout. Les
justificatifs transitent par le backend, qui contrôle l'accès à chaque requête, et sont
stockés derrière une abstraction (disque local sur volume Docker maintenant, bucket au
déploiement). Contrat d'abord : [contracts/openapi.yaml](./contracts/openapi.yaml) (0.2.0).
Détails et alternatives : [research.md](./research.md).

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5 (frontend)

**Primary Dependencies**: inchangées depuis 001 (Spring Boot 3.5, Spring Security, Data JPA,
Liquibase, MapStruct, springdoc, openapi-generator 7.14 ; React, TanStack Query,
react-hook-form, shadcn/ui, openapi-fetch). **Aucune nouvelle dépendance** : multipart et
flux de fichiers viennent de Spring Web, la détection de format est faite sur quatre
signatures ([R14](./research.md)).

**Storage**: PostgreSQL 17 (6 nouveaux changesets, [data-model.md](./data-model.md)) ;
fichiers des justificatifs sur disque local derrière `DocumentStorage` (dossier
`DOCUMENTS_DIR`, volume Docker `documents-data`), bucket GCS ou S3 au déploiement
([R13](./research.md)).

**Testing**: JUnit 5, Spring Boot Test + MockMvc, Testcontainers PostgreSQL, validation de
contrat (`swagger-request-validator-mockmvc`), horloge fixée ; tests unitaires de
`DueDateCalculator` ; Vitest + Testing Library ([R16](./research.md)).

**Target Platform**: inchangée (images backend et frontend nginx ; VPS Docker Compose ou
Cloud Run) ; navigateurs mobiles et desktop récents.

**Project Type**: application web (API REST + SPA).

**Performance Goals**: réponses API < 300 ms au 95e centile ; envoi d'une photo de 5 Mo en
moins de 10 s en 4G ; accueil avec rappels en moins de 2 s sur mobile.

**Constraints**: droits réévalués à chaque requête ; aucun justificatif accessible sans
session autorisée ; fichiers de 10 Mo au plus ; montants sans arrondi ; dates au jour de
Paris ; interface en français, utilisable d'une main à 360 px ; charte v2 « Lumineux »
(skill `charte-interface`).

**Scale/Scope**: 2 à 5 membres, 2 à 5 véhicules, quelques centaines de saisies et de
justificatifs par véhicule ; 23 nouveaux chemins d'API ; écrans : catalogue des types (ADMIN), fiche véhicule enrichie,
saisie d'un relevé, formulaire d'entretien, détail d'entretien avec justificatifs, plan
d'entretien (règles et échéances à date fixe), rappels sur l'accueil.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principe | Exigence | Comment le plan la respecte | Statut |
|---|---|---|---|
| I. Contract-First | Contrat d'abord, code généré au build, jamais commité, frontend sur types générés | [contracts/openapi.yaml](./contracts/openapi.yaml) recopié dans `backend/api-contract/…/openapi.yaml` en première tâche ; contrôleurs des trois modules implémentant les `*Api` générés ; `npm run generate:api` côté frontend ; envoi et contenu des fichiers décrits dans le contrat (`multipart/form-data`, réponses binaires). | ✅ |
| II. Monolithe modulaire en couches | Un déployable, modules métier, Controller / Service / Repository / Mapper, pas d'entité exposée | Modules `maintenance`, `schedule`, `document` ([R1](./research.md)), dépendances dans un seul sens, ports et événements pour remonter l'information ([R2](./research.md)) ; mappers MapStruct ; règles dans les services (cohérence du kilométrage, calcul des échéances). | ✅ |
| III. Auth Google | Inchangée | Aucun changement d'authentification ; nouveaux chemins protégés par la configuration existante (`/api/**` authentifié, CSRF sur l'envoi de fichiers compris). | ✅ |
| IV. RBAC | USER limité à ses véhicules, contrôle serveur | `VehicleAccessGuard.assertReadable` / `lockForWrite` en tête de chaque service ; contenu des justificatifs servi uniquement après ce contrôle ([R5](./research.md), [R12](./research.md)) ; « auteur ou ADMIN » pour modifier une saisie. ; catalogue : écritures réservées aux ADMIN par `@PreAuthorize("hasRole('ADMIN')")`. | ✅ |
| V. Tests ciblés | IT du calcul des échéances (date et kilométrage) et du RBAC, conformité au contrat, en CI | `DueDateCalculator` testé sur table de cas + IT des scénarios US4 ; IT RBAC sur les 18 chemins (dont le contenu d'un justificatif) ; validation de contrat de chaque requête et réponse ([R16](./research.md)). | ✅ |
| VI. Simplicité | Pas d'infrastructure non justifiée | Aucune nouvelle dépendance ni service : pas de MinIO, pas de tâche planifiée, pas de cache ; échéances calculées à la lecture ; un volume Docker pour les fichiers. Ajouts justifiés en Complexity Tracking. | ✅ (justifié) |
| Stack : stockage de fichiers | Abstraction disque local en dev, bucket en prod, sans changement du code métier | Interface `DocumentStorage` + `LocalDocumentStorage` ; l'implémentation bucket est une classe de plus, choisie par configuration, ajoutée avec le déploiement ([R13](./research.md)). | ✅ |
| Dev / CI/CD | Compose, natif, pipeline inchangé | `compose.yaml` gagne le volume `documents-data` ; nginx du frontend accepte 11 Mo ; CI inchangée. | ✅ |

**Point d'interprétation (stockage)** : la constitution demande le bucket « en production ».
Aucune cible de production n'est encore déployée ; ce plan livre l'abstraction et
l'implémentation locale, et l'implémentation bucket sera ajoutée par la fonctionnalité de
déploiement, sans toucher aux modules métier. Sur un VPS en Docker Compose, le volume local
convient aussi en production ; sur Cloud Run il est obligatoire de passer au bucket (disque du
conteneur éphémère).

**Résultat** : porte franchie avant la Phase 0 et confirmée après la Phase 1 (le design
n'ajoute aucun écart).

## Project Structure

### Documentation (this feature)

```text
specs/002-entretiens-kilometrage/
├── spec.md              # Spécification (fusionnée, PR #16 et #17)
├── plan.md              # Ce fichier
├── research.md          # Phase 0 : décisions techniques
├── data-model.md        # Phase 1 : tables, contraintes, valeurs calculées
├── quickstart.md        # Phase 1 : guide de validation
├── contracts/
│   └── openapi.yaml     # Phase 1 : contrat complet 0.2.0 (devient la source dans api-contract)
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks, pas encore créé)
```

### Source Code (repository root)

Seuls les éléments nouveaux ou modifiés sont listés.

```text
settings.gradle.kts                  # + backend:maintenance, backend:schedule, backend:document
compose.yaml                         # + volume documents-data monté sur le backend, DOCUMENTS_DIR
.env.example                         # + DOCUMENTS_DIR
frontend/nginx/default.conf.template # + client_max_body_size 11m

backend/
├── api-contract/src/main/resources/openapi/openapi.yaml   # contrat 0.2.0
├── common/
│   ├── error/ErrorCode              # + 10 codes (VEHICLE_ARCHIVED, NOT_AUTHOR, MILEAGE_…, DOCUMENT_…, TYPE_NAME_ALREADY_EXISTS)
│   ├── error/ApiExceptionHandler    # + 413 multipart en Problem Details
│   └── time/Today                   # date du jour à Paris (app.time-zone)
├── vehicle/
│   ├── Vehicle, VehicleMapper       # + currentMileageKm, currentMileageDate
│   ├── VehicleService               # + updateCurrentMileage ; contrôle FR-009 via VehicleMileageFloor
│   ├── VehicleAccessGuard           # + lockForWrite (archivé → 409, verrou de la ligne)
│   ├── VehicleMileageFloor          # port implémenté par maintenance
│   └── changeset 005-add-vehicle-current-mileage
├── maintenance/                     # fr.carsupervision.maintenance (US1, US2, US3)
│   ├── MaintenanceType, MaintenanceTypeCategory, MaintenanceTypeRepository, MaintenanceTypeService
│   ├── MaintenanceTypesController   # catalogue : lecture pour tous, écriture ADMIN (US6)
│   ├── MileageReading, MileageReadingRepository, MileageReadingService, MileageReadingsController
│   ├── MaintenanceRecord, MaintenanceRecordRepository, MaintenanceRecordService, MaintenanceRecordsController
│   ├── MileageConsistency           # règle FR-006 / FR-007 sur l'ensemble des points d'un véhicule
│   ├── VehicleHistoryService, VehicleHistoryController   # historique, filtres, totaux
│   ├── MaintenanceQueries           # dernière réalisation d'un type (pour schedule)
│   ├── RecordDocumentCounter        # port implémenté par document
│   ├── MaintenanceRecordDeleted     # événement
│   ├── mappers MapStruct
│   └── changesets 006-create-mileage-reading, 007-create-maintenance-type (+ catalogue pré-rempli), 008-create-maintenance-record
├── schedule/                        # fr.carsupervision.schedule (US4)
│   ├── MaintenanceRule, Deadline, repositories, services, contrôleurs
│   ├── DueDateCalculator            # calcul pur des échéances (R10), tests unitaires
│   ├── DueItemsService              # échéances d'un véhicule
│   ├── RemindersService, RemindersController   # accueil : échéances proches et rappel de relevé
│   ├── DeadlineSuggestionService, DeadlineSuggestionsController   # assurance, vignette, visite technique (FR-040)
│   └── changeset 009-create-schedule
├── document/                        # fr.carsupervision.document (US5)
│   ├── Document, DocumentRepository, DocumentService, DocumentsController
│   ├── DocumentStorage, LocalDocumentStorage, DocumentStorageConfig
│   ├── FileSignature                # PDF, JPEG, PNG, WebP
│   ├── RecordDocumentCounterImpl, MaintenanceRecordDeletedListener
│   └── changeset 010-create-document
└── app/
    ├── build.gradle.kts             # + dépendances document, schedule
    ├── application.yaml             # multipart 10 Mo, app.documents.*, app.time-zone
    ├── db/changelog/db.changelog-master.yaml   # + changesets 005 à 010
    └── src/test/java/fr/carsupervision/
        ├── maintenance/             # MileageReadingsIT, MaintenanceRecordsIT, VehicleHistoryIT, MileageConcurrencyIT
        ├── schedule/                # MaintenanceRulesIT, DeadlinesIT, RemindersIT
        └── document/                # DocumentsIT (formats, taille, accès, cascade)

frontend/src/
├── i18n/fr.ts                       # catégories, états, messages (les noms de types viennent du catalogue)
├── lib/format.ts                    # montants (€), kilométrages, dates en français
├── pages/
│   ├── VehiclesPage.tsx             # + kilométrage actuel, saisie rapide d'un relevé, rappels
│   ├── VehicleDetailPage.tsx        # + kilométrage, échéances, totaux, historique filtrable
│   ├── MaintenanceRecordFormPage.tsx
│   ├── MaintenanceRecordPage.tsx    # détail, justificatifs (envoi, aperçu, suppression)
│   ├── VehiclePlanPage.tsx          # règles, échéances à date fixe, suggestions
│   └── admin/MaintenanceTypesPage.tsx   # catalogue des types (US6)
└── components/
    ├── MileageReadingDialog.tsx     # saisie d'un relevé (clavier numérique, confirmation d'écart)
    ├── DueItemCard.tsx, ReminderList.tsx
    └── DocumentList.tsx
```

**Structure Decision** : on garde le monolithe multi-modules de 001. Le module `vehicle` ne
connaît aucun des nouveaux modules ; il expose `VehicleAccessGuard`, `VehicleService` et le
port `VehicleMileageFloor`. `maintenance` porte la règle de cohérence commune aux relevés et
aux entretiens ; `schedule` et `document` en dépendent sans dépendre l'un de l'autre. `CLAUDE.md`
(chaîne de dépendances et repères dans le code) sera mis à jour dans la PR d'implémentation.

## Découpage de livraison suggéré

Pour `/speckit-tasks`, dans l'ordre des priorités de la spec, chaque étape restant
démontrable :

1. **Socle** : contrat 0.2.0, trois modules vides branchés, `Today`, codes d'erreur,
   changeset 005, `lockForWrite`, `currentMileage` dans `Vehicle`.
2. **US1 Relevés** : relevés, cohérence et confirmation d'écart, kilométrage actuel, FR-009,
   saisie rapide depuis « Mes véhicules ».
3. **US2 Entretiens** : catalogue pré-rempli et sa lecture, entretiens, « auteur ou ADMIN », formulaire mobile.
4. **US3 Historique** : historique filtrable, totaux, fiche véhicule.
5. **US4 Échéances** : `DueDateCalculator` et ses tests, règles, échéances à date fixe,
   rappels de l'accueil dont le rappel de relevé.
6. **US5 Justificatifs** : stockage local, envoi et contrôle des fichiers, contenu servi après
   contrôle d'accès, cascade à la suppression, volume Docker et nginx.
7. **US6 Catalogue (écran ADMIN)** : création, modification, désactivation des types ; le
   catalogue pré-rempli et sa lecture arrivent dès US2.
8. **Finition** : vérification à 360 px, quickstart de bout en bout, `CLAUDE.md`, consigne de
   sauvegarde.

## Complexity Tracking

| Écart ou ajout | Pourquoi c'est nécessaire | Alternative plus simple écartée parce que |
|---|---|---|
| Trois modules Gradle au lieu d'un | Constitution (principe II) et séparation claire : cohérence du kilométrage, calcul des échéances et stockage de fichiers évoluent séparément | Un seul module : mélangerait fichiers et règles métier, et grossirait vite. |
| Ports et événement entre modules ([R2](./research.md)) | Respecter le sens unique des dépendances (pas de cycle) tout en affichant le nombre de justificatifs et en refusant un kilométrage de départ trop élevé | Dépendance inverse : cycle Gradle interdit. Duplication de données : risque d'incohérence. |
| Kilométrage actuel stocké sur `vehicle` | Liste des véhicules et accueil sans requête par véhicule | Calcul à la lecture : plus de requêtes et un port de plus pour l'affichage de `vehicle`. |
| Verrou pessimiste par véhicule | Règle de cohérence sur plusieurs lignes, saisies simultanées possibles | Isolation `SERIALIZABLE` : relances à gérer. Aucun contrôle : incohérences possibles. |
| Catalogue des types en base géré par l'ADMIN | Choix du mainteneur : fréquences adaptées au pays et aux véhicules, sans changer le code ; référentiel prévu par la constitution (principe IV) | Liste fixe en code : impossible à adapter sans livraison. |
| Volume Docker pour les justificatifs | Exigé par la constitution (stockage de fichiers) ; disque local en dev | Fichiers en base : base et sauvegardes alourdies. MinIO : service de plus. |
