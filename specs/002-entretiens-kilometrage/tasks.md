---
description: "Liste des tâches de la fonctionnalité 002-entretiens-kilometrage"
---

# Tasks: Kilométrage, entretiens, échéances et justificatifs

**Input**: Documents de conception de `/specs/002-entretiens-kilometrage/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/openapi.yaml](./contracts/openapi.yaml),
[quickstart.md](./quickstart.md)

**Tests**: OBLIGATOIRES. La constitution (principe V) impose des tests d'intégration sur le
calcul des échéances (par date et par kilométrage) et sur les règles RBAC, et la validation de
la conformité au contrat. Dans chaque histoire, les tests sont écrits d'abord et doivent
échouer avant l'implémentation. Chaque requête et réponse des IT est vérifiée avec
`ContractValidation.conformsToContract()`.

**Organization**: tâches regroupées par histoire utilisateur (US1 à US5), chacune livrable et
testable seule, dans l'ordre des priorités de la spec.

## Format: `[ID] [P?] [Story] Description`

- **[P]** : parallélisable (fichiers différents, aucune dépendance sur une tâche non terminée)
- **[Story]** : histoire concernée (US1 à US5)
- Chaque tâche indique le chemin exact du fichier

## Path Conventions

- Backend : un sous-projet Gradle par module dans `backend/<module>/`, paquets
  `fr.carsupervision.<module>`. Abréviations : `MNT` = `backend/maintenance/src/main/java/fr/carsupervision/maintenance`,
  `SCH` = `backend/schedule/src/main/java/fr/carsupervision/schedule`,
  `DOC` = `backend/document/src/main/java/fr/carsupervision/document`,
  `VEH` = `backend/vehicle/src/main/java/fr/carsupervision/vehicle`,
  `IT` = `backend/app/src/test/java/fr/carsupervision`.
- Changesets : `backend/<module>/src/main/resources/db/changelog/changes/`, inclus dans
  `backend/app/src/main/resources/db/changelog/db.changelog-master.yaml`.
- Code généré (jamais commité) : `backend/api-contract/build/generated/openapi/`,
  `frontend/src/api/generated/`.
- Frontend : `FE` = `frontend/src`, tests dans `frontend/tests/`. Avant toute tâche
  frontend, suivre la skill `charte-interface` (charte v2 « Lumineux ») ; libellés uniquement
  dans `FE/i18n/fr.ts`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: contrat 0.2.0, nouveaux modules branchés, configuration.

- [ ] T001 Copier `specs/002-entretiens-kilometrage/contracts/openapi.yaml` vers `backend/api-contract/src/main/resources/openapi/openapi.yaml` (contrat 0.2.0, source unique)
- [ ] T002 Lancer `./gradlew :backend:api-contract:compileJava` et vérifier la génération de `uploadRecordDocument` (paramètre `MultipartFile file`) et de `getDocumentContent` (retour `ResponseEntity<org.springframework.core.io.Resource>`) ; si le générateur produit autre chose, ajuster uniquement la configuration `openApiGenerate` de `backend/api-contract/build.gradle.kts` (par exemple `typeMappings`/`importMappings` pour `binary` → `Resource`), sans toucher au contrat ([research R15](./research.md))
- [ ] T003 Créer les sous-projets `backend/maintenance/build.gradle.kts` (`api(project(":backend:vehicle"))`), `backend/schedule/build.gradle.kts` (`api(project(":backend:maintenance"))`) et `backend/document/build.gradle.kts` (`api(project(":backend:maintenance"))`), tous avec le plugin `carsupervision.java-module`, et les déclarer dans `settings.gradle.kts` avec un commentaire d'une ligne chacun
- [ ] T004 Ajouter `implementation(project(":backend:schedule"))` et `implementation(project(":backend:document"))` dans `backend/app/build.gradle.kts`
- [ ] T005 [P] Compléter `backend/app/src/main/resources/application.yaml` : `spring.servlet.multipart.max-file-size: 10MB`, `max-request-size: 11MB`, `app.time-zone: ${APP_TIME_ZONE:Europe/Paris}`, `app.documents.storage: local`, `app.documents.storage-dir: ${DOCUMENTS_DIR:./data/documents}`
- [ ] T006 [P] Ajouter `DOCUMENTS_DIR` (commenté, avec la valeur par défaut) dans `.env.example` et `data/` dans `.gitignore`
- [ ] T007 [P] Dans `compose.yaml`, service `backend` : variable `DOCUMENTS_DIR: /var/lib/carsupervision/documents` et volume nommé `documents-data` monté sur ce chemin ; déclarer `documents-data` dans `volumes:` ; dans `backend/Dockerfile`, créer ce dossier avec l'utilisateur d'exécution comme propriétaire
- [ ] T008 [P] Ajouter `client_max_body_size 11m;` dans le bloc `server` de `frontend/nginx/default.conf.template` (commentaire : justificatifs de 10 Mo, FR-030)
- [ ] T009 [P] Lancer `cd frontend && npm run generate:api` et corriger la compilation TypeScript si `Vehicle.currentMileage` (désormais requis) casse des fixtures dans `frontend/tests/apiStub.ts`

**Checkpoint**: `./gradlew build` compile les trois modules vides et les interfaces générées ; `npm run build` passe.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: briques partagées par toutes les histoires. ⚠️ Aucune histoire ne commence avant la fin de cette phase.

### Socle commun

- [ ] T010 Ajouter à `backend/common/src/main/java/fr/carsupervision/common/error/ErrorCode.java` les codes `VEHICLE_ARCHIVED`, `NOT_AUTHOR`, `MILEAGE_INCONSISTENT`, `MILEAGE_JUMP_NOT_CONFIRMED`, `INITIAL_MILEAGE_ABOVE_RECORDS`, `RULE_ALREADY_EXISTS`, `DOCUMENT_TYPE_NOT_ALLOWED`, `DOCUMENT_TOO_LARGE`, `DOCUMENT_LIMIT_REACHED` (mêmes noms que l'énumération `Problem.code` du contrat)
- [ ] T011 Créer `backend/common/src/main/java/fr/carsupervision/common/time/Today.java` : composant qui renvoie `LocalDate.now(clock.withZone(zone))` avec `zone` lu dans `app.time-zone` (défaut `Europe/Paris`) et l'horloge partagée de `ClockConfig` ([R8](./research.md)), plus `year()`
- [ ] T012 [P] Test unitaire `backend/common/src/test/java/fr/carsupervision/common/time/TodayTest.java` : à 23 h 30 UTC le 31 décembre, `Today` renvoie le 1er janvier à Paris

### Véhicule : kilométrage actuel et écriture

- [ ] T013 Créer le changeset `backend/vehicle/src/main/resources/db/changelog/changes/005-add-vehicle-current-mileage.yaml` : colonnes `current_mileage_km integer` et `current_mileage_date date`, remplies par `UPDATE vehicle SET current_mileage_km = initial_mileage_km, current_mileage_date = created_at::date`, puis `NOT NULL` et `CHECK (current_mileage_km >= 0)` ; l'inclure dans `db.changelog-master.yaml`
- [ ] T014 Ajouter `currentMileageKm` et `currentMileageDate` à `VEH/Vehicle.java` ; à la création (`VehicleService.create`) les initialiser au kilométrage de départ et à `Today` ; à la modification, si le kilométrage de départ change et que le véhicule n'a aucune saisie (`VehicleMileageFloor` vide), le kilométrage actuel suit
- [ ] T015 Mapper `currentMileage { mileageKm, date }` dans `VEH/VehicleMapper.java` (schéma `CurrentMileage` du contrat)
- [ ] T016 Créer le port `VEH/VehicleMileageFloor.java` (`OptionalInt lowestRecordedMileage(UUID vehicleId)`) et l'utiliser dans `VehicleService.update` via `ObjectProvider` : refuser par 409 `INITIAL_MILEAGE_ABOVE_RECORDS` un `initialMileageKm` supérieur au plus petit kilométrage saisi (FR-009) ; message « Le kilométrage de départ ne peut pas dépasser le plus petit kilométrage déjà saisi (48 000 km). »
- [ ] T017 Ajouter `VehicleService.updateCurrentMileage(UUID vehicleId, int km, LocalDate date)` (public, appelé par `maintenance` dans sa transaction) dans `VEH/VehicleService.java`
- [ ] T018 Ajouter à `VEH/VehicleRepository.java` `findByIdForUpdate(UUID)` avec `@Lock(LockModeType.PESSIMISTIC_WRITE)`, et à `VEH/VehicleAccessGuard.java` `Vehicle lockForWrite(UUID vehicleId)` : règle de lecture d'`assertReadable` (403 `VEHICLE_FORBIDDEN` sans fuite, 404 pour un ADMIN si inexistant), puis 409 `VEHICLE_ARCHIVED` « Ce véhicule est archivé : aucune saisie n'est possible. » si archivé (ADMIN compris), puis verrou de la ligne ([R4](./research.md), [R5](./research.md)) ; ajouter aussi `List<Vehicle> activeAccessibleVehicles()` (ADMIN : tous les actifs ; USER : `findActiveAccessibleBy`)
- [ ] T019 Ajouter dans `VEH/VehicleAccessGuard.java` (ou une petite classe `VEH/AuthorPolicy.java`) `assertAuthorOrAdmin(UUID authorId)` (403 `NOT_AUTHOR` « Seul l'auteur de cette saisie ou un administrateur peut la modifier. ») et `canEdit(UUID authorId)`, plus `authorSnapshot()` qui renvoie l'identifiant et le nom affiché (ou l'email à défaut) du membre connecté ([R6](./research.md))

### Outillage de test

- [ ] T020 Ajouter à `IT/support/IntegrationTestBase.java` le nettoyage des nouvelles tables (`document, maintenance_record_operation, maintenance_record, mileage_reading, maintenance_rule, deadline` dans le `TRUNCATE`) et une horloge fixée (bean `Clock` de test au 2026-06-15T10:00:00Z, modifiable par test) dans `IT/support/TestSupportConfig.java`
- [ ] T021 [P] Créer `IT/support/TestVehicles.java` : fabriques `active(nickname, initialKm)`, `archived(...)`, `assign(member, vehicle)` et helpers JSON pour poster un relevé, un entretien et une règle
- [ ] T022 Mettre à jour les IT existants qui comparent un `Vehicle` complet (`IT/vehicle/VehiclesAdminIT.java`, `IT/vehicle/UserVehicleVisibilityIT.java`) pour `currentMileage`, et ajouter un test : modifier le kilométrage de départ d'un véhicule sans saisie met à jour `currentMileage`

### Socle frontend

- [ ] T023 [P] Créer `FE/lib/format.ts` : `formatKm(52000)` → « 52 000 km », `formatEuros(18950)` → « 189,50 € » (`Intl.NumberFormat('fr-FR')`), `parseEurosToCents("189,5")` → 18950 (virgule ou point, 2 décimales au plus, sinon erreur), `formatDate` en « 24/11/2026 » ; tests dans `frontend/tests/format.test.ts`
- [ ] T024 [P] Ajouter dans `FE/i18n/fr.ts` les libellés des 14 opérations (Révision, Vidange, Filtre à huile, Filtre à air, Filtre d'habitacle, Filtre à carburant, Pneus, Freins, Distribution, Batterie, Climatisation, Contrôle technique, Réparation, Autre), des états (« En retard », « Bientôt », « À jour ») et des nouveaux codes d'erreur
- [ ] T025 Afficher `currentMileage` (« 52 000 km · relevé le 24/11/2026 ») sur chaque carte de `FE/pages/VehiclesPage.tsx` et dans `FE/pages/admin/FleetPage.tsx` ; mettre à jour `frontend/tests/VehiclesPage.test.tsx`

**Checkpoint**: tous les tests existants passent ; un véhicule expose son kilométrage actuel ; `lockForWrite` refuse un véhicule archivé.

---

## Phase 3: User Story 1 - Relever le kilométrage d'un véhicule (Priority: P1) 🎯 MVP

**Goal**: saisir, corriger et supprimer un relevé, avec cohérence chronologique et kilométrage actuel à jour.

**Independent Test**: USER autorisé sur A (50 000 km) saisit 51 200 km sur téléphone ; A affiche 51 200 km ; une baisse est refusée avec le relevé bloquant ([quickstart](./quickstart.md) 1 à 3).

### Tests for User Story 1 ⚠️

- [ ] T026 [P] [US1] IT `IT/maintenance/MileageReadingsIT.java` : création (201, `currentMileage` à jour), baisse refusée 409 `MILEAGE_INCONSISTENT` avec le point bloquant dans `detail`, relevé rétroactif cohérent accepté sans changer le kilométrage actuel, deux relevés le même jour acceptés, inférieur au kilométrage de départ refusé, date future refusée 400 (date de Paris), écart > 100 000 km refusé 409 `MILEAGE_JUMP_NOT_CONFIRMED` puis accepté avec `confirmLargeJump: true`, correction et suppression qui recalculent le kilométrage actuel (US1-1 à US1-5, FR-005 à FR-008)
- [ ] T027 [P] [US1] IT `IT/maintenance/MileageAccessIT.java` : USER non attribué → 403 `VEHICLE_FORBIDDEN` sur création, correction, suppression (y compris identifiant inexistant) ; véhicule archivé → 409 `VEHICLE_ARCHIVED` même pour un ADMIN ; USER qui corrige le relevé d'un autre → 403 `NOT_AUTHOR` ; ADMIN corrige le relevé d'un USER → 200 ; auteur supprimé de la liste blanche → relevé conservé avec `authorName`
- [ ] T028 [P] [US1] IT `IT/vehicle/InitialMileageFloorIT.java` : kilométrage de départ supérieur au plus petit relevé → 409 `INITIAL_MILEAGE_ABOVE_RECORDS` ; égal → accepté (FR-009)
- [ ] T029 [P] [US1] IT `IT/maintenance/MileageConcurrencyIT.java` : deux créations simultanées incompatibles (même véhicule, dates croisées) → une seule réussit, l'autre reçoit 409 (verrou par véhicule, [R4](./research.md))

### Implementation for User Story 1

- [ ] T030 [US1] Changeset `backend/maintenance/src/main/resources/db/changelog/changes/006-create-mileage-reading.yaml` selon [data-model](./data-model.md) : `vehicle_id` FK `vehicle` `ON DELETE CASCADE`, `reading_date date` non nul, `mileage_km integer` non nul `CHECK (mileage_km >= 0)`, `note varchar(200)`, `author_id` FK `member` `ON DELETE SET NULL`, `author_name varchar(100)` non nul, `created_at`/`updated_at timestamptz` non nuls, index `(vehicle_id, reading_date)` ; l'inclure dans le changelog maître
- [ ] T031 [P] [US1] Entité `MNT/MileageReading.java` et `MNT/MileageReadingRepository.java` (lecture des relevés d'un véhicule triés par date puis kilométrage)
- [ ] T032 [US1] Créer `MNT/MileagePoints.java` : requête qui renvoie tous les points `(id, kind, date, km)` d'un véhicule (relevés pour l'instant ; les entretiens s'ajouteront en US2) et `MNT/MileageConsistency.java` (classe pure) : `check(points, candidate, excludedId, initialKm, confirmLargeJump)` appliquant [R4](./research.md) (≥ départ, ≥ max des points antérieurs, ≤ min des points postérieurs, aucune contrainte le même jour ; écart > 100 000 km avec le point antérieur le plus récent sans confirmation) ; messages en français citant le point bloquant (« Un kilométrage de 51 200 km a été saisi le 24/11/2026. »)
- [ ] T033 [P] [US1] Test unitaire `backend/maintenance/src/test/java/fr/carsupervision/maintenance/MileageConsistencyTest.java` : table de cas de T032 (bornes incluses, même jour, exclusion du point modifié, seuil de 100 000 km)
- [ ] T034 [US1] Créer `MNT/CurrentMileageUpdater.java` : recalcule le point le plus récent (date, puis km ; sinon kilométrage et date de création du véhicule) et appelle `VehicleService.updateCurrentMileage` ([R3](./research.md))
- [ ] T035 [US1] `MNT/MileageReadingService.java` : `create`, `update`, `delete` — `lockForWrite` d'abord, puis `assertAuthorOrAdmin` (modification, suppression), validation (date ≤ `Today`, note ≤ 200), `MileageConsistency`, enregistrement avec `authorSnapshot`, puis `CurrentMileageUpdater`
- [ ] T036 [P] [US1] Mapper MapStruct `MNT/MileageReadingMapper.java` (`canEdit` et `currentMileage` renseignés par le service)
- [ ] T037 [US1] `MNT/MileageReadingsController.java` implémentant l'interface générée du tag `mileage` (`createMileageReading` 201, `updateMileageReading`, `deleteMileageReading` 204)
- [ ] T038 [US1] Implémenter le port `VehicleMileageFloor` dans `MNT/MileageFloorProvider.java` (min des kilométrages de relevés ; les entretiens s'ajouteront en US2)
- [ ] T039 [P] [US1] Hooks `FE/api/mileage.ts` (`useCreateMileageReading`, `useUpdateMileageReading`, `useDeleteMileageReading`) qui invalident les clés `vehicles` et `history`
- [ ] T040 [US1] Composant `FE/components/MileageReadingDialog.tsx` : feuille plein écran sur mobile, champ kilométrage `inputMode="numeric"` pré-rempli avec le kilométrage actuel, date par défaut aujourd'hui, note facultative ; sur 409 `MILEAGE_JUMP_NOT_CONFIRMED`, boîte de confirmation puis renvoi avec `confirmLargeJump: true` ; sur 409 `MILEAGE_INCONSISTENT`, message sous le champ sans perdre la saisie ; toast de succès
- [ ] T041 [US1] Bouton « Relever le kilométrage » sur chaque carte de `FE/pages/VehiclesPage.tsx` (un geste, FR-017) et sur `FE/pages/VehicleDetailPage.tsx`, qui ouvrent `MileageReadingDialog`
- [ ] T042 [P] [US1] Test `frontend/tests/MileageReadingDialog.test.tsx` : envoi, message d'incohérence, confirmation d'un grand écart

**Checkpoint**: US1 livrable seule (scénarios 1 à 3 du quickstart).

---

## Phase 4: User Story 2 - Enregistrer un entretien réalisé (Priority: P2)

**Goal**: enregistrer, modifier et supprimer un entretien ; son kilométrage compte comme un relevé.

**Independent Test**: enregistrer une révision (Vidange + Filtre à huile, 189 €, « Garage du Centre ») sur un véhicule autorisé et la retrouver avec toutes ses informations ([quickstart](./quickstart.md) 4, 5, 18).

### Tests for User Story 2 ⚠️

- [ ] T043 [P] [US2] IT `IT/maintenance/MaintenanceRecordsIT.java` : création complète (201, `costCents: 18900`, `authorName`, `documentCount: 0`), kilométrage actuel mis à jour (US2-2), `OTHER` sans `otherDescription` → 400, aucune opération → 400, opération en double → 400, `costCents` négatif → 400, cohérence partagée avec les relevés (un entretien plus bas qu'un relevé antérieur → 409), entretien rétroactif cohérent rangé sans changer le kilométrage actuel (US2-6), modification et suppression par l'auteur et par un ADMIN (US2-4)
- [ ] T044 [P] [US2] IT `IT/maintenance/MaintenanceAccessIT.java` : USER non attribué → 403 sur `getMaintenanceRecord`, création, modification, suppression ; USER qui modifie l'entretien d'un autre → 403 `NOT_AUTHOR`, mais peut le lire (US2-5) ; véhicule archivé → 409 en écriture, lecture ADMIN autorisée ; `GET /operation-types` renvoie les 14 codes dans l'ordre avec les intervalles de [R9](./research.md)

### Implementation for User Story 2

- [ ] T045 [US2] Changeset `backend/maintenance/src/main/resources/db/changelog/changes/007-create-maintenance-record.yaml` selon [data-model](./data-model.md) : table `maintenance_record` (`record_date date` non nul, `mileage_km integer` non nul `>= 0`, `other_description varchar(200)`, `cost_cents integer CHECK (cost_cents >= 0)`, `garage varchar(100)`, `notes varchar(2000)`, auteur, audit, index `(vehicle_id, record_date)`) et table `maintenance_record_operation` (`record_id` FK `ON DELETE CASCADE`, `operation varchar(30)` avec `CHECK` sur les 14 codes, clé primaire `(record_id, operation)`, index `(operation)`) ; l'inclure dans le changelog maître
- [ ] T046 [P] [US2] `MNT/OperationType.java` (énumération alignée sur le contrat) et `MNT/OperationCatalog.java` (ordre d'affichage et intervalles par défaut de [R9](./research.md))
- [ ] T047 [P] [US2] `MNT/OperationTypesController.java` (`listOperationTypes`)
- [ ] T048 [P] [US2] Entité `MNT/MaintenanceRecord.java` (`@ElementCollection` d'`OperationType` sur `maintenance_record_operation`, `EnumType.STRING`) et `MNT/MaintenanceRecordRepository.java`
- [ ] T049 [US2] Étendre `MNT/MileagePoints.java` et `MNT/MileageFloorProvider.java` aux entretiens (un entretien est un point comme un relevé), et `MNT/CurrentMileageUpdater.java` en conséquence
- [ ] T050 [US2] Créer le port `MNT/RecordDocumentCounter.java` (`Map<UUID, Integer> countByRecord(Collection<UUID>)`, absent = 0) et l'événement `MNT/MaintenanceRecordDeleted.java` (`recordId`, `vehicleId`) ([R2](./research.md))
- [ ] T051 [US2] `MNT/MaintenanceRecordService.java` : `get` (lecture via `assertReadable`), `create`, `update`, `delete` — `lockForWrite`, auteur ou ADMIN, validation (`OTHER` ⇒ `otherDescription` non vide, opérations ≥ 1 sans doublon, date ≤ `Today`), `MileageConsistency`, `CurrentMileageUpdater` ; à la suppression, publier `MaintenanceRecordDeleted` **avant** de supprimer la ligne
- [ ] T052 [P] [US2] Mapper `MNT/MaintenanceRecordMapper.java`
- [ ] T053 [US2] `MNT/MaintenanceRecordsController.java` (`createMaintenanceRecord` 201, `getMaintenanceRecord`, `updateMaintenanceRecord`, `deleteMaintenanceRecord` 204)
- [ ] T054 [P] [US2] Hooks `FE/api/maintenance.ts` (`useOperationTypes`, `useMaintenanceRecord`, création, modification, suppression)
- [ ] T055 [US2] Page `FE/pages/MaintenanceRecordFormPage.tsx` (routes `vehicules/:vehicleId/entretiens/nouveau` et `entretiens/:recordId/modifier` dans `FE/App.tsx`) : date, kilométrage (`inputMode="numeric"`), opérations en cases à cocher (grandes zones tactiles), description obligatoire si « Autre », coût en euros (`inputMode="decimal"`, `parseEurosToCents`), garage, remarques ; même gestion des 409 que `MileageReadingDialog` ; erreurs par champ sans perte de saisie
- [ ] T056 [US2] Page `FE/pages/MaintenanceRecordPage.tsx` (route `entretiens/:recordId`) : détail, auteur, boutons Modifier/Supprimer seulement si `canEdit`, suppression avec confirmation
- [ ] T057 [P] [US2] Test `frontend/tests/MaintenanceRecordFormPage.test.tsx` : « Autre » exige une description, conversion du coût, envoi

**Checkpoint**: US1 et US2 fonctionnent ensemble ; un entretien met à jour le kilométrage.

---

## Phase 5: User Story 3 - Consulter l'historique d'un véhicule (Priority: P3)

**Goal**: fiche véhicule avec kilométrage actuel, totaux et historique filtrable.

**Independent Test**: véhicule avec 3 entretiens et 5 relevés ; ordre chronologique, totaux et filtre « Vidange » corrects ([quickstart](./quickstart.md) 5, 6).

### Tests for User Story 3 ⚠️

- [ ] T058 [P] [US3] IT `IT/maintenance/VehicleHistoryIT.java` : ordre (date décroissante, puis kilométrage décroissant), totaux 420 € cette année et 870 € au total avec des entretiens sans coût ignorés (US3-2, SC-006), filtre `operation=OIL_CHANGE` (relevés exclus), `recordsOnly=true`, véhicule sans saisie (kilométrage de départ, liste vide), `canEdit` selon l'auteur, `documentCount`, USER non attribué → 403, véhicule archivé lisible par l'ADMIN seulement (FR-018)

### Implementation for User Story 3

- [ ] T059 [US3] `MNT/VehicleHistoryService.java` : fusion relevés + entretiens triés, filtres, totaux en centimes par `sum` en base sur l'année `Today.year()` et sur tout l'historique, `documentCount` via `RecordDocumentCounter`
- [ ] T060 [US3] `MNT/VehicleHistoryController.java` (`getVehicleHistory`)
- [ ] T061 [P] [US3] Hook `FE/api/history.ts` (`useVehicleHistory(vehicleId, filters)`)
- [ ] T062 [US3] Refaire `FE/pages/VehicleDetailPage.tsx` : en-tête kilométrage actuel et date, tuiles « Dépenses {année} » et « Depuis le début », boutons « Relever le kilométrage » et « Ajouter un entretien », historique en liste (icône relevé ou entretien, opérations, coût, garage, nombre de justificatifs), filtre par opération et bascule « Entretiens seulement », état vide invitant à une première saisie (US3-4)
- [ ] T063 [P] [US3] Test `frontend/tests/VehicleDetailPage.test.tsx` : totaux formatés, filtre, état vide

**Checkpoint**: US1 à US3 forment un carnet d'entretien complet.

---

## Phase 6: User Story 4 - Être prévenu des prochaines échéances (Priority: P4)

**Goal**: règles d'échéance et échéances à date fixe, états calculés, rappels sur l'accueil, dont le rappel de relevé à 30 jours.

**Independent Test**: dernière vidange à 45 000 km il y a 9 mois, 59 200 km actuels, règle « 15 000 km ou 12 mois » → « Bientôt » sur la fiche et l'accueil ; une vidange relance l'échéance ([quickstart](./quickstart.md) 8 à 12, 20).

### Tests for User Story 4 ⚠️

- [ ] T064 [P] [US4] Test unitaire `backend/schedule/src/test/java/fr/carsupervision/schedule/DueDateCalculatorTest.java` (table de cas, principe V) : date seule, km seul, les deux (le plus défavorable l'emporte), bornes `remainingDays = 30` (OK) / `29` (SOON) / `0` (OVERDUE), `remainingKm = 1000` (OK) / `999` (SOON) / `0` (OVERDUE), restes négatifs, `plusMonths` depuis le 31 janvier (→ 28/29 février), tri OVERDUE → SOON → OK puis proximité
- [ ] T065 [P] [US4] IT `IT/schedule/MaintenanceRulesIT.java` : scénarios US4-1 à US4-5 de bout en bout (création avec point de départ par défaut = aujourd'hui et kilométrage actuel, « Bientôt » à 59 200 km, « En retard » à 60 400 km, relance par un entretien « Vidange », entretien rétroactif plus ancien sans effet, suppression de l'entretien de référence qui revient au précédent puis au point de départ), règle sans intervalle → 400, deuxième règle « Vidange » → 409 `RULE_ALREADY_EXISTS`, modification et suppression par tout membre ayant accès, USER non attribué → 403, archivé → 409
- [ ] T066 [P] [US4] IT `IT/schedule/DeadlinesIT.java` : création, état selon la date, `complete` sur une échéance annuelle au 1er avril 2027 → suivante au 1er avril 2028 (US4-6), `complete` idempotent, échéance unique sans suivante, accès et archivage comme T065
- [ ] T067 [P] [US4] IT `IT/schedule/RemindersIT.java` : `GET /reminders` ne contient que OVERDUE et SOON des véhicules actifs accessibles (USER : ses attributions, US4-7 ; ADMIN : tous les actifs ; archivés jamais) ; rappel de relevé pour un véhicule dont le dernier kilométrage date de 31 jours, absent à 30 jours et après un nouveau relevé (US4-8, FR-035, SC-011)

### Implementation for User Story 4

- [ ] T068 [US4] Changeset `backend/schedule/src/main/resources/db/changelog/changes/008-create-schedule.yaml` selon [data-model](./data-model.md) : `maintenance_rule` (`operation varchar(30)` non nul, `interval_km integer` 1 à 1 000 000, `interval_months smallint` 1 à 240, `baseline_date date` non nul, `baseline_mileage_km integer` non nul `>= 0`, audit, `UNIQUE (vehicle_id, operation)`, `CHECK (interval_km IS NOT NULL OR interval_months IS NOT NULL)`) et `deadline` (`label varchar(60)` non nul, `due_date date` non nul, `recurrence_months smallint` 1 à 120, `done_at timestamptz`, audit) ; l'inclure dans le changelog maître
- [ ] T069 [US4] Classe pure `SCH/DueDateCalculator.java` ([R10](./research.md)) : entrée (dernière réalisation, intervalles, kilométrage actuel, aujourd'hui) → `dueDate`, `dueMileageKm`, `remainingDays`, `remainingKm`, `status` ; seuils 30 jours et 1 000 km en constantes ; comparateur de tri
- [ ] T070 [P] [US4] Entités et dépôts `SCH/MaintenanceRule.java`, `SCH/MaintenanceRuleRepository.java`, `SCH/Deadline.java`, `SCH/DeadlineRepository.java`
- [ ] T071 [US4] Ajouter `MNT/MaintenanceQueries.java` (public) : `lastOccurrence(vehicleId, operation)` → entretien le plus récent comportant l'opération (date puis km), avec son identifiant
- [ ] T072 [US4] `SCH/MaintenanceRuleService.java` : création (opération unique par véhicule, intervalles, point de départ par défaut `Today` et kilométrage actuel, `baselineDate ≤ Today`), modification (l'opération ne change pas), suppression ; `lockForWrite` sur toute écriture, pas de contrôle d'auteur (FR-021)
- [ ] T073 [US4] `SCH/DeadlineService.java` : création, modification, suppression, `complete` (idempotent ; si `recurrenceMonths`, nouvelle échéance `dueDate.plusMonths(recurrenceMonths)` dans la même transaction)
- [ ] T074 [US4] `SCH/DueItemsService.java` : pour un véhicule, échéances des règles (dernière réalisation = `max(point de départ, lastOccurrence)` par date) et des échéances à date fixe à faire, via `DueDateCalculator`, triées
- [ ] T075 [US4] `SCH/RemindersService.java` : pour `activeAccessibleVehicles()`, échéances OVERDUE et SOON, et véhicules dont `currentMileageDate` est antérieure à `Today − 30 jours` (FR-035)
- [ ] T076 [P] [US4] Mappers `SCH/ScheduleMapper.java` (règle, échéance, `DueItem`, `Reminders`)
- [ ] T077 [US4] Contrôleurs `SCH/MaintenanceRulesController.java`, `SCH/DeadlinesController.java`, `SCH/DueItemsController.java`, `SCH/RemindersController.java` (interfaces du tag `schedule`)
- [ ] T078 [P] [US4] Hooks `FE/api/schedule.ts` (`useDueItems`, `useReminders`, règles, échéances, `useCompleteDeadline`) ; invalider `reminders` et `due-items` après toute saisie de relevé ou d'entretien (compléter `FE/api/mileage.ts` et `FE/api/maintenance.ts`)
- [ ] T079 [US4] Composant `FE/components/DueItemCard.tsx` : opération ou libellé, badge d'état (couleurs de la charte), « dans 700 km », « dans 12 jours », « en retard de 400 km », dernière réalisation
- [ ] T080 [US4] Section « Échéances » de `FE/pages/VehicleDetailPage.tsx` (en retard d'abord) et page `FE/pages/VehiclePlanPage.tsx` (route `vehicules/:vehicleId/plan`) : liste des règles avec intervalles proposés par `useOperationTypes`, formulaire de règle (opération, km, mois, dernière réalisation connue), échéances à date fixe (libellé, date, récurrence « Aucune / Tous les ans / Autre (mois) »), « Marquer comme faite »
- [ ] T081 [US4] Composant `FE/components/ReminderList.tsx` en tête de `FE/pages/VehiclesPage.tsx` (l'accueil) : échéances en retard et proches par véhicule, et rappels « Pense à relever le kilométrage » (dernier kilométrage et date, bouton qui ouvre `MileageReadingDialog`) ; pastille avec le nombre de rappels dans la navigation de `FE/components/AppLayout.tsx` (SC-008)
- [ ] T082 [P] [US4] Tests `frontend/tests/DueItemCard.test.tsx` et `frontend/tests/ReminderList.test.tsx` : libellés des états, restes positifs et négatifs, rappel de relevé

**Checkpoint**: les échéances apparaissent sur la fiche et l'accueil et se recalculent après chaque saisie.

---

## Phase 7: User Story 5 - Joindre les factures et justificatifs (Priority: P5)

**Goal**: joindre, consulter, télécharger et supprimer des justificatifs, servis uniquement après contrôle d'accès.

**Independent Test**: joindre une photo et un PDF, les rouvrir depuis un autre compte autorisé, vérifier qu'un compte non autorisé n'obtient rien même avec l'adresse directe ([quickstart](./quickstart.md) 13 à 17).

### Tests for User Story 5 ⚠️

- [ ] T083 [P] [US5] Test unitaire `backend/document/src/test/java/fr/carsupervision/document/FileSignatureTest.java` : PDF, JPEG, PNG, WebP reconnus ; texte renommé `.pdf`, GIF, fichier vide refusés
- [ ] T084 [P] [US5] IT `IT/document/DocumentsIT.java` (stockage dans un `@TempDir` via `app.documents.storage-dir`) : envoi d'un PDF et d'un JPEG (201, `contentType` déduit de la signature), contenu identique au téléchargement avec `Content-Disposition` `inline` puis `attachment` et le nom d'origine, `X-Content-Type-Options: nosniff`, `Cache-Control: private, no-store` ; > 10 Mo → 413 `DOCUMENT_TOO_LARGE` en Problem Details ; texte renommé → 415 ; onzième → 409 `DOCUMENT_LIMIT_REACHED` ; nom malveillant (`../../etc/passwd`) nettoyé et sans effet sur le chemin de stockage ; suppression par l'auteur et l'ADMIN, 403 `NOT_AUTHOR` sinon ; suppression de l'entretien → lignes et fichiers supprimés, `documentCount` correct avant (US5-1 à US5-6)
- [ ] T085 [P] [US5] IT `IT/document/DocumentAccessIT.java` : USER non attribué → 403 sans aucun octet sur `getDocumentContent`, liste, envoi, suppression (y compris identifiant inexistant) ; accès retiré à un USER → 403 dès la requête suivante (SC-010) ; véhicule archivé → 409 à l'envoi, lecture ADMIN autorisée ; requête non authentifiée → 401

### Implementation for User Story 5

- [ ] T086 [US5] Changeset `backend/document/src/main/resources/db/changelog/changes/009-create-document.yaml` selon [data-model](./data-model.md) : `record_id` FK `maintenance_record` (sans cascade), `vehicle_id` FK `vehicle`, `storage_key varchar(100)` non nul unique, `file_name varchar(120)` non nul, `content_type varchar(30)` non nul `CHECK IN ('application/pdf','image/jpeg','image/png','image/webp')`, `size_bytes bigint` non nul `CHECK (size_bytes BETWEEN 1 AND 10485760)`, auteur, `created_at`, index `(record_id)` ; l'inclure dans le changelog maître
- [ ] T087 [P] [US5] `DOC/DocumentStorage.java` (interface `store`, `open`, `delete`), `DOC/LocalDocumentStorage.java` (dossier `app.documents.storage-dir` créé au démarrage, clé `<vehicleId>/<documentId>`, écriture dans un fichier temporaire puis `Files.move(ATOMIC_MOVE)`, refus de toute clé qui sortirait du dossier) et `DOC/DocumentStorageConfig.java` (sélection par `app.documents.storage=local`) ([R13](./research.md))
- [ ] T088 [P] [US5] `DOC/FileSignature.java` : détection sur les premiers octets ([R14](./research.md)) ; `DOC/FileNames.java` : nettoyage du nom d'origine (caractères de contrôle et séparateurs retirés, 120 caractères, « justificatif » si vide)
- [ ] T089 [P] [US5] Entité `DOC/Document.java` et `DOC/DocumentRepository.java` (`countByRecordId`, comptage groupé par entretien)
- [ ] T090 [US5] `DOC/DocumentService.java` : `upload` (lecture de l'entretien, `lockForWrite` du véhicule, limite de 10, signature, écriture du fichier puis de la ligne, effacement du fichier si la transaction échoue), `list`, `open` (`assertReadable` sur `vehicle_id` avant toute lecture du fichier), `delete` (auteur ou ADMIN, ligne puis fichier après validation)
- [ ] T091 [US5] `DOC/DocumentsController.java` : `listRecordDocuments`, `uploadRecordDocument` (201), `getDocumentContent` (flux `Resource`, `Content-Type`, `Content-Disposition` avec `filename*` UTF-8, `nosniff`, `no-store`), `deleteDocument` (204)
- [ ] T092 [US5] `DOC/RecordDocumentCounterImpl.java` (port de `maintenance`) et `DOC/MaintenanceRecordDeletedListener.java` (`@EventListener` : supprimer les lignes dans la transaction ; `@TransactionalEventListener(AFTER_COMMIT)` : effacer les fichiers) ([R2](./research.md))
- [ ] T093 [US5] Dans `backend/common/src/main/java/fr/carsupervision/common/error/ApiExceptionHandler.java`, traduire `MaxUploadSizeExceededException` en 413 `DOCUMENT_TOO_LARGE` « Le fichier dépasse 10 Mo. »
- [ ] T094 [P] [US5] Hooks `FE/api/documents.ts` (liste, envoi `multipart` via `FormData` avec l'en-tête CSRF, suppression) et URL de contenu `/api/documents/{id}/content` (`?download=true` pour télécharger)
- [ ] T095 [US5] Composant `FE/components/DocumentList.tsx` dans `FE/pages/MaintenanceRecordPage.tsx` : bouton « Joindre une facture » (`<input type="file" accept="application/pdf,image/*">`, appareil photo proposé sur mobile), vérification de taille côté client avant envoi, progression, vignette pour les images (l'image elle-même) et icône pour les PDF, nom, taille, date, ouvrir, télécharger, supprimer si `canDelete` ; messages des erreurs 409/413/415
- [ ] T096 [US5] Confirmation de suppression d'un entretien dans `FE/pages/MaintenanceRecordPage.tsx` qui annonce le nombre de justificatifs (`documentCount`, US5-6) ; icône trombone et nombre dans l'historique de `FE/pages/VehicleDetailPage.tsx`
- [ ] T097 [P] [US5] Test `frontend/tests/DocumentList.test.tsx` : refus côté client au-delà de 10 Mo, affichage d'une image et d'un PDF, bouton supprimer selon `canDelete`

**Checkpoint**: toutes les histoires fonctionnent ; les justificatifs ne sont lisibles que par les membres ayant accès au véhicule.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [ ] T098 [P] Mettre à jour `CLAUDE.md` : chaîne de dépendances (`app → document/schedule → maintenance → vehicle → …`), repères des modules `maintenance`, `schedule`, `document`, variable `DOCUMENTS_DIR`
- [ ] T099 [P] Ajouter au `README.md` la consigne de sauvegarde conjointe de la base et du dossier des justificatifs (volume `documents-data`) ([quickstart §4](./quickstart.md))
- [ ] T100 Vérifier tous les nouveaux écrans à 360 px (aucun débordement, zones tactiles, clavier numérique) en suivant la skill `charte-interface`, et corriger
- [ ] T101 Dérouler les scénarios 1 à 20 du [quickstart](./quickstart.md) ; noter dans ce fichier ceux qui demandent une vérification manuelle de Said (téléphone réel, appareil photo)
- [ ] T102 Lancer `./gradlew test`, `cd frontend && npm test -- --run && npm run lint && npm run build` et vérifier la CI verte sur la PR

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)** : aucune dépendance ; T002 conditionne toute la suite (code généré).
- **Foundational (Phase 2)** : après le Setup ; bloque toutes les histoires.
- **US1 (Phase 3)** : après la Phase 2. MVP.
- **US2 (Phase 4)** : après US1 (réutilise `MileagePoints`, `MileageConsistency`, `CurrentMileageUpdater`).
- **US3 (Phase 5)** : après US2 (historique des deux types de saisie).
- **US4 (Phase 6)** : après US2 (dernière réalisation d'une opération) ; peut avancer en parallèle de US3.
- **US5 (Phase 7)** : après US2 (rattachement aux entretiens) ; peut avancer en parallèle de US3 et US4.
- **Polish (Phase 8)** : après les histoires retenues.

### Within Each User Story

- Tests d'abord, en échec, puis changeset → entités → services → contrôleurs → frontend.
- Un changeset ne se modifie jamais une fois fusionné : toute correction passe par un nouveau changeset.

### Dépendances notables entre tâches

- T013 (changeset 005) avant T014 ; T016 (port) avant T038 (implémentation).
- T018 (`lockForWrite`) et T019 (auteur) avant tout service d'écriture (T035, T051, T072, T073, T090).
- T032 (cohérence) avant T035 et T049 ; T050 (port et événement) avant T059 et T092.
- T071 (`lastOccurrence`) avant T074 ; T069 (calculateur) avant T074 et T075.
- T093 (413) avant de faire passer T084.

### Parallel Opportunities

- Phase 1 : T005 à T009.
- Phase 2 : T012, T021, T023, T024 en parallèle des tâches backend.
- Dans chaque histoire, les tests marqués [P] s'écrivent ensemble ; entités, mappers et hooks frontend [P] en parallèle.
- Après US2 : US3, US4 et US5 dans des branches ou par des personnes différentes.

---

## Parallel Example: User Story 4

```bash
# Tests d'abord, ensemble :
Task: "T064 DueDateCalculatorTest (table de cas)"
Task: "T065 MaintenanceRulesIT"
Task: "T066 DeadlinesIT"
Task: "T067 RemindersIT"

# Puis, en parallèle :
Task: "T070 Entités et dépôts MaintenanceRule / Deadline"
Task: "T076 ScheduleMapper"
Task: "T078 Hooks FE/api/schedule.ts"
```

## Parallel Example: User Story 5

```bash
Task: "T083 FileSignatureTest"
Task: "T084 DocumentsIT"
Task: "T085 DocumentAccessIT"
Task: "T087 DocumentStorage + LocalDocumentStorage"
Task: "T088 FileSignature + FileNames"
Task: "T089 Document + DocumentRepository"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phases 1 et 2 (contrat, modules, kilométrage actuel, verrou d'écriture).
2. Phase 3 (relevés) : la famille peut déjà relever les compteurs depuis le téléphone.
3. **Arrêt et validation** : scénarios 1 à 3 du quickstart, puis fusion.

### Incremental Delivery

Une PR par étape, chacune avec une CI verte :

1. Setup + Foundational + US1 → relevés (MVP).
2. US2 → entretiens.
3. US3 → historique et dépenses.
4. US4 → échéances et rappels (règles critiques du principe V).
5. US5 → justificatifs.
6. Polish.

## Notes

- [P] = fichiers différents, aucune dépendance sur une tâche non terminée.
- Aucun type d'API n'est écrit à la main : backend sur les interfaces générées, frontend sur
  `FE/api/generated/schema.d.ts`.
- Cocher chaque tâche (`[X]`) dans ce fichier au fil de l'implémentation.
