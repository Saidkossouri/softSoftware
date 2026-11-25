# Research : kilométrage, entretiens, échéances et justificatifs

**Feature**: `002-entretiens-kilometrage` | **Plan**: [plan.md](./plan.md) | **Date**: 2026-11-25

Décisions techniques de la Phase 0. Chaque décision suit le format Décision / Justification /
Alternatives écartées. Le socle (Spring Boot, Spring Security, Liquibase, MapStruct,
Testcontainers, React, shadcn/ui) est celui de la fonctionnalité 001
([research 001](../001-acces-membres-flotte/research.md)) et n'est pas rediscuté ici.

## R1. Découpage en modules Gradle

- **Décision** : trois nouveaux sous-projets dans `backend/`, en plus de `vehicle` :
  - `maintenance` : relevés kilométriques, entretiens, liste des opérations, historique et
    totaux, règles de cohérence du kilométrage ;
  - `schedule` : règles d'échéance, échéances à date fixe, calcul des échéances, rappels de
    l'accueil (dont le rappel de relevé) ;
  - `document` : justificatifs et abstraction de stockage des fichiers.

  Dépendances, toujours dans un seul sens, sans cycle :

  ```text
  app ──> document ──┐
  app ──> schedule ──┴──> maintenance ──> vehicle ──> security ──> member ──> common ──> api-contract
  ```

- **Justification** : la constitution (principe II) cite ces modules métier ; relevés et
  entretiens partagent une seule règle de cohérence (FR-006), donc un seul module ; échéances
  et justificatifs dépendent des entretiens mais pas l'un de l'autre. Le graphe reste un arbre
  orienté vers `vehicle`, ce que la règle « dépendances dans un seul sens » de `CLAUDE.md`
  autorise (la chaîne linéaire décrite aujourd'hui sera mise à jour).
- **Alternatives écartées** : un seul module `maintenance` pour tout (plus simple au départ,
  mais mélange stockage de fichiers et calcul d'échéances, et grossit vite) ; quatre modules
  (`mileage` séparé de `maintenance`) : la règle de cohérence porte sur les deux à la fois, la
  séparer créerait des allers-retours entre modules.

## R2. Communication entre modules : appels descendants et « ports »

- **Décision** : un module appelle librement les services publics des modules dont il dépend.
  Quand un module du bas a besoin d'une information d'un module du haut, il déclare une petite
  interface (« port ») que le module du haut implémente, injectée en `ObjectProvider` (absente
  = comportement neutre). Trois ports :
  - `vehicle.VehicleMileageFloor` (implémenté par `maintenance`) : plus petit kilométrage déjà
    saisi, pour refuser un kilométrage de départ trop élevé (FR-009) ;
  - `maintenance.RecordDocumentCounter` (implémenté par `document`) : nombre de justificatifs
    par entretien, affiché dans l'historique et dans la confirmation de suppression ;
  - suppression d'un entretien : `maintenance` publie l'événement `MaintenanceRecordDeleted`
    dans la transaction, avant de supprimer la ligne ; `document` l'écoute (`@EventListener`, même transaction) pour
    supprimer les lignes, puis efface les fichiers après validation
    (`@TransactionalEventListener(AFTER_COMMIT)`).
- **Justification** : respecte le sens unique des dépendances sans dupliquer de données ; tout
  reste dans le même processus et la même transaction (aucune infrastructure).
- **Alternatives écartées** : dépendance inverse (crée un cycle Gradle, interdit) ; clés
  étrangères `ON DELETE CASCADE` seules pour les justificatifs (les lignes partiraient, mais
  pas les fichiers sur disque).

## R3. Kilométrage actuel : stocké sur le véhicule, recalculé à chaque saisie

- **Décision** : deux colonnes ajoutées à `vehicle` (changeset du module `vehicle`) :
  `current_mileage_km` et `current_mileage_date`, initialisées avec le kilométrage de départ
  et la date de création. À chaque création, modification ou suppression de relevé ou
  d'entretien, `maintenance` recalcule « le point le plus récent » (date la plus récente, puis
  plus grand kilométrage ; à défaut, le kilométrage de départ) et appelle
  `VehicleService.updateCurrentMileage(...)` dans la même transaction. Le schéma `Vehicle` du
  contrat gagne `currentMileage { mileageKm, date }`.
- **Justification** : la liste « Mes véhicules » (FR-017) et l'accueil lisent le kilométrage
  sans requête supplémentaire par véhicule ; le recalcul complet (et non un « max » incrémental)
  reste juste après une correction ou une suppression. Volumétrie : quelques centaines de
  points par véhicule, recalcul instantané.
- **Alternatives écartées** : calcul à la lecture via un port (requête par véhicule à chaque
  liste, et le module `vehicle` dépendrait d'un port pour son propre affichage) ; vue SQL
  (logique cachée hors des services, contraire au principe II).

## R4. Cohérence chronologique du kilométrage (FR-006, FR-007, FR-009)

- **Décision** : un « point » est un relevé ou un entretien `(date, km)`. Pour une saisie
  `(d, k)` sur un véhicule, en excluant la saisie elle-même lors d'une modification :
  - `k ≥ initialMileageKm` ;
  - `k ≥` le plus grand kilométrage des points datés **avant** `d` ;
  - `k ≤` le plus petit kilométrage des points datés **après** `d` ;
  - aucune contrainte entre points du même jour (tri par kilométrage décroissant à
    l'affichage).

  En cas de violation : 409 `MILEAGE_INCONSISTENT`, avec un `fieldError` sur `mileageKm` et un
  message qui cite le point bloquant (« Un kilométrage de 51 200 km a été saisi le
  24/11/2026 »). Si `k` dépasse de plus de 100 000 km le point précédent et que
  `confirmLargeJump` n'est pas vrai : 409 `MILEAGE_JUMP_NOT_CONFIRMED` ; le frontend affiche
  une confirmation puis renvoie la saisie. Une suppression ne peut jamais créer d'incohérence.
  FR-009 : `VehicleService.update` refuse (409 `INITIAL_MILEAGE_ABOVE_RECORDS`) un kilométrage
  de départ supérieur à `VehicleMileageFloor.lowest(vehicleId)`.
- **Concurrence** : toute écriture commence par verrouiller la ligne du véhicule
  (`SELECT … FOR UPDATE` via `@Lock(PESSIMISTIC_WRITE)`), ce qui sérialise les saisies d'un
  même véhicule ; deux membres au garage en même temps ne peuvent pas créer d'incohérence.
- **Justification** : règle simple, compréhensible et testable ; le verrou par véhicule est la
  solution la plus légère pour une règle qui porte sur plusieurs lignes.
- **Alternatives écartées** : contrainte en base (impossible à exprimer simplement entre
  lignes) ; isolation `SERIALIZABLE` (relances à gérer, plus complexe) ; interdire toute saisie
  antérieure au dernier relevé (empêche la reprise d'un vieux carnet, scénario US2-6).

## R5. Droits d'écriture

- **Décision** : `VehicleAccessGuard` (module `vehicle`) gagne
  `lockForWrite(vehicleId)` : mêmes règles de lecture que `assertReadable`, puis 409
  `VEHICLE_ARCHIVED` si le véhicule est archivé (ADMIN compris), puis verrou (R4). Pour
  modifier ou supprimer un relevé, un entretien ou un justificatif : auteur ou ADMIN, sinon
  403 `NOT_AUTHOR`. Les règles et échéances à date fixe sont modifiables par tout membre ayant
  accès (FR-021). Les réponses portent `canEdit` / `canDelete` pour que l'interface n'affiche
  que les actions permises, mais le serveur reste seul juge.
- **Ordre des contrôles** : accès au véhicule (403 `VEHICLE_FORBIDDEN`, sans fuite) avant
  tout le reste ; une ressource inexistante donne 403 à un USER et 404 à un ADMIN, comme en
  001.
- **Justification** : réutilise la règle d'accès de 001 sans la dupliquer (principe IV).

## R6. Auteur des saisies

- **Décision** : chaque relevé, entretien et justificatif stocke `author_id` (clé étrangère
  vers `member`, `ON DELETE SET NULL`) et `author_name` (nom affiché, ou email à défaut, figé à
  la création).
- **Justification** : en 001 un membre retiré est supprimé ; ses saisies doivent rester et
  afficher son nom (cas limite de la spec). `author_id` sert au contrôle « auteur ou ADMIN ».
- **Alternatives écartées** : suppression logique des membres (change la fonctionnalité 001) ;
  nom seul sans identifiant (contrôle d'auteur fragile en cas d'homonymes).

## R7. Montants

- **Décision** : coût en centimes (`cost_cents integer`, `costCents` dans le contrat) ; totaux
  calculés en base (`sum`) sur des entiers. Le frontend convertit la saisie « 189,50 » en 18950
  et affiche avec `Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' })`.
- **Justification** : aucun arrondi possible (SC-006) ; les entiers passent sans perte en JSON
  et en TypeScript.
- **Alternatives écartées** : `number` décimal en JSON (risque d'arrondi en JavaScript) ;
  chaîne décimale (validation et conversion des deux côtés).

## R8. Dates « du jour » et fuseau horaire

- **Décision** : nouveau composant `common.time.Today` qui donne la date du jour dans le fuseau
  `app.time-zone` (par défaut `Europe/Paris`) à partir de l'horloge partagée. Il sert aux
  contrôles « pas dans le futur », à l'année des totaux et au calcul des échéances.
- **Justification** : l'horloge est en UTC ; un relevé saisi à 00 h 30 à Paris ne doit pas être
  refusé comme « dans le futur ». Les tests fixent l'horloge.

## R9. Liste des opérations et intervalles par défaut

- **Décision** : énumération `OperationType` dans le contrat ; les intervalles par défaut sont
  une table en code dans `maintenance` (`OperationCatalog`), exposée par
  `GET /api/operation-types`. Libellés uniquement dans `frontend/src/i18n/fr.ts`.

  | Code | Libellé | Défaut |
  |---|---|---|
  | `SERVICE` | Révision | 15 000 km ou 12 mois |
  | `OIL_CHANGE` | Vidange | 15 000 km ou 12 mois |
  | `OIL_FILTER` | Filtre à huile | 15 000 km ou 12 mois |
  | `AIR_FILTER` | Filtre à air | 30 000 km ou 24 mois |
  | `CABIN_FILTER` | Filtre d'habitacle | 15 000 km ou 12 mois |
  | `FUEL_FILTER` | Filtre à carburant | 60 000 km ou 48 mois |
  | `TIRES` | Pneus | 40 000 km |
  | `BRAKES` | Freins | 30 000 km |
  | `TIMING_BELT` | Distribution | 120 000 km ou 60 mois |
  | `BATTERY` | Batterie | 60 mois |
  | `AIR_CONDITIONING` | Climatisation | 24 mois |
  | `TECHNICAL_INSPECTION` | Contrôle technique | 24 mois |
  | `REPAIR` | Réparation | aucun |
  | `OTHER` | Autre (description obligatoire) | aucun |

  Les opérations d'un entretien sont stockées dans une table d'association
  (`maintenance_record_operation`), indexée par opération pour le filtre (FR-016) et la
  recherche de la dernière réalisation (R10).
- **Justification** : la spec fixe la liste ; un référentiel en base géré par l'ADMIN n'est pas
  demandé (Assumptions). Ces valeurs ne sont que des propositions modifiables règle par règle.
- **Alternatives écartées** : table de référence en base (écrans ADMIN en plus, non demandés) ;
  colonne tableau PostgreSQL (filtre moins lisible en JPA).

## R10. Calcul des échéances (FR-023, FR-024, principe V)

- **Décision** : calcul à la lecture, jamais stocké. Pour une règle `(opération, intervalKm?,
  intervalMonths?, baselineDate, baselineKm)` :
  1. **Dernière réalisation** : l'entretien le plus récent du véhicule comportant l'opération
     (date, puis kilométrage) s'il est daté au plus tôt du point de départ ; sinon le point de
     départ de la règle. Un entretien rétroactif plus ancien que la dernière réalisation ne
     change donc rien ; la suppression de l'entretien de référence fait revenir à la
     réalisation précédente ou au point de départ.
  2. **Termes** : `dueDate = lastDate.plusMonths(intervalMonths)` (fin de mois ramenée au
     dernier jour, règle de `java.time`) ; `dueKm = lastKm + intervalKm`.
  3. **Restes** : `remainingDays = dueDate − aujourd'hui` ; `remainingKm = dueKm −
     kilométrage actuel`.
  4. **État par terme** : `OVERDUE` si reste ≤ 0, `SOON` si `remainingDays < 30` ou
     `remainingKm < 1000`, sinon `OK` ; l'état de l'échéance est le plus défavorable des deux
     termes.

  Échéances à date fixe : même règle sur la seule date. « Faite » : `done = true`, et si
  `recurrenceMonths` existe, nouvelle échéance à `dueDate.plusMonths(recurrenceMonths)`
  (à partir de l'échéance prévue, pas du jour où on la coche : l'assurance du 1er avril reste
  au 1er avril). Tri : `OVERDUE`, `SOON`, `OK`, puis reste croissant (jours et kilomètres
  comparés via le plus proche des deux).

  Le calcul est une classe pure `DueDateCalculator` (aucune dépendance Spring), testée
  unitairement sur une table de cas (date seule, km seul, les deux, bornes 30 jours / 1 000 km,
  fins de mois, retard), puis en intégration de bout en bout (saisie → échéance recalculée).
- **Justification** : FR-026 (« immédiatement ») est garanti sans tâche planifiée ni cache ;
  la volumétrie (≤ 5 véhicules × ≤ 15 règles) rend le calcul négligeable.
- **Alternatives écartées** : stocker l'échéance et la recalculer par événement (risque de
  désynchronisation) ; tâche planifiée nocturne (pas d'immédiateté, infrastructure de plus) ;
  estimation du kilométrage futur (hors périmètre, Assumptions).

## R11. Rappels de l'accueil (FR-028, FR-035)

- **Décision** : `GET /api/reminders` agrège, pour les véhicules **actifs** accessibles au
  membre (ADMIN : tous les actifs ; USER : ses attributions), les échéances `OVERDUE` et `SOON`
  et la liste des véhicules dont `current_mileage_date` est antérieure à aujourd'hui − 30 jours.
  Le frontend affiche une pastille sur l'accueil et dans la navigation dès que la liste n'est
  pas vide. Aucun email ni notification push (Assumptions).
- **Justification** : un seul appel à l'ouverture de l'application (SC-008) ; réutilise R3
  pour le rappel de relevé.

## R12. Justificatifs : transit par le backend, sans URL présignée

- **Décision** (choisie par le mainteneur le 2026-11-25) : envoi par
  `POST /api/maintenance-records/{id}/documents` en `multipart/form-data` et lecture par
  `GET /api/documents/{id}/content`, toujours via le backend, qui contrôle l'accès au véhicule
  à chaque requête (FR-032). Le contenu est copié en flux (sans charger le fichier en mémoire)
  vers ou depuis le stockage. Pas d'URL présignée.
- **Justification** : contrôle d'accès refait à chaque téléchargement, alors qu'une URL
  présignée est utilisable par quiconque la détient jusqu'à expiration ; un seul chemin de code
  pour le disque local et un bucket (les URL présignées n'existent pas pour le disque) ; pas de
  CORS sur le bucket ni d'envoi en deux temps laissant des fichiers orphelins ; le format et la
  taille sont vérifiés avant stockage. Volume : 10 Mo maximum, 5 utilisateurs, sous la limite de
  32 Mo par requête de Cloud Run.
- **Alternatives écartées** : URL présignées pour l'envoi et/ou la lecture (gardées en option
  future : une redirection 302 vers une URL de courte durée après contrôle d'accès, sans
  changer le contrat de l'envoi) ; fichiers en base `bytea` (base et sauvegardes alourdies,
  contraire à la stack de la constitution).

## R13. Abstraction de stockage

- **Décision** : interface `DocumentStorage` dans le module `document` :
  `store(key, InputStream, size, contentType)`, `open(key)`, `delete(key)`. Implémentation
  livrée : `LocalDocumentStorage`, dans le dossier `app.documents.storage-dir` (variable
  `DOCUMENTS_DIR`, par défaut `./data/documents` en natif, `/var/lib/carsupervision/documents`
  en Docker sur un volume nommé `documents-data`). Choix de l'implémentation par
  `app.documents.storage=local`. La clé est `<vehicleId>/<documentId>` : jamais le nom fourni
  par l'utilisateur (aucune traversée de chemin possible) ; écriture dans un fichier temporaire
  puis renommage atomique. Une implémentation GCS ou S3 s'ajoutera au déploiement (fonctionnalité
  dédiée), sans toucher au code métier, comme l'exige la constitution.
- **Cohérence fichier / base** : envoi = écrire le fichier puis insérer la ligne, et effacer le
  fichier si la transaction échoue ; suppression = supprimer la ligne puis le fichier après
  validation. Un arrêt brutal peut laisser un fichier sans ligne, sans effet visible ; un
  nettoyage n'est pas nécessaire à cette échelle.
- **Sauvegarde** : le dossier des justificatifs se sauvegarde avec la base (consigne ajoutée au
  quickstart et au README).
- **Alternatives écartées** : MinIO en local pour imiter S3 (service de plus, non justifié,
  principe VI) ; GCS dès maintenant (pas encore de déploiement cible).

## R14. Contrôle des fichiers envoyés (FR-030)

- **Décision** : taille limitée à 10 Mo par `spring.servlet.multipart.max-file-size=10MB`
  (`max-request-size=11MB`) avec une réponse 413 `DOCUMENT_TOO_LARGE` en Problem Details, et
  `client_max_body_size 11m` dans le nginx du frontend (sinon nginx refuse au-delà de 1 Mo).
  Format déterminé par la signature du contenu (`%PDF-`, `FF D8 FF`, `89 50 4E 47`,
  `RIFF….WEBP`), sinon 415 `DOCUMENT_TYPE_NOT_ALLOWED` ; le type annoncé par le navigateur est
  ignoré. 10 justificatifs au plus par entretien (409 `DOCUMENT_LIMIT_REACHED`). Nom d'origine
  nettoyé (caractères de contrôle et séparateurs retirés, 120 caractères) et renvoyé dans
  `Content-Disposition` (`filename*` UTF-8). Réponses de contenu avec
  `X-Content-Type-Options: nosniff` et `Cache-Control: private, no-store`.
- **Justification** : un fichier renommé (`.exe` en `.pdf`) ne peut pas être servi comme PDF ;
  aucun contenu n'est exécuté ni interprété par le serveur. Pas d'antivirus (usage familial,
  fichiers servis uniquement aux membres de la liste blanche).
- **Photos de téléphone** : `<input type="file" accept="application/pdf,image/*">` propose
  l'appareil photo sur mobile sans l'imposer ; Safari iOS convertit les photos HEIC en JPEG à
  l'envoi. Pas de redimensionnement côté client dans cette fonctionnalité.
- **Alternatives écartées** : Apache Tika (dépendance lourde pour quatre signatures) ;
  vignettes générées côté serveur (le navigateur affiche l'image elle-même en petit ; un PDF a
  une icône).

## R15. Contrat d'API

- **Décision** : contrat 0.2.0 dans [contracts/openapi.yaml](./contracts/openapi.yaml), à
  recopier dans `backend/api-contract/src/main/resources/openapi/openapi.yaml` en première
  tâche. Ajouts : 18 chemins, 9 codes d'erreur, `Vehicle.currentMileage`. Choix notables :
  historique « à plat » (`HistoryEntry` avec `kind`) plutôt qu'un `oneOf`, mal géré par
  openapi-generator ; `DueItem` à plat pour la même raison ; `canEdit` / `canDelete` calculés
  pour le membre connecté ; envoi `multipart/form-data` (paramètre `MultipartFile` généré) et
  contenu binaire typé par format.
- **Point à vérifier en première tâche** : la génération Spring du `multipart` et des réponses
  binaires (`Resource`) avec openapi-generator 7.14 ; repli prévu si besoin :
  `useResponseEntity` et type de retour `Resource`, sans changer le contrat. (La génération n'a
  pas pu être lancée pendant la rédaction du plan : dépôt Maven indisponible, code 429.)

## R16. Stratégie de test (principe V)

Tests d'intégration `*IT` dans `backend/app` (Testcontainers PostgreSQL, validation de chaque
requête et réponse contre le contrat, horloge fixée) :

- **RBAC** : pour chaque nouveau chemin, USER non attribué → 403 sans fuite (y compris
  ressource inexistante), USER attribué → succès, ADMIN → succès ; véhicule archivé → 409 en
  écriture, lecture ADMIN seulement ; modification de la saisie d'un autre → 403 `NOT_AUTHOR` ;
  contenu d'un justificatif demandé par un USER non attribué → 403 sans octet renvoyé.
- **Kilométrage** : baisse refusée avec le point bloquant, rétroactif cohérent accepté,
  confirmation au-delà de 100 000 km, recalcul après modification et suppression, kilométrage
  de départ trop élevé refusé, deux saisies concurrentes sur un même véhicule.
- **Échéances** : table de cas de `DueDateCalculator` (unitaire, module `schedule`), puis IT de
  bout en bout : scénarios US4-1 à US4-8 de la spec, dont l'entretien qui relance une
  échéance, la suppression de l'entretien de référence, l'échéance récurrente et le rappel de
  relevé à 30 jours.
- **Justificatifs** : formats acceptés et refusés (signature), 10 Mo, limite de 10, nom
  d'origine dans `Content-Disposition`, suppression d'un entretien qui efface ses fichiers
  (stockage local dans un dossier temporaire de test).
- **Frontend** (Vitest + Testing Library) : formats français des montants et kilométrages,
  formulaire d'entretien (opération « Autre », confirmation de grand écart), affichage des
  états d'échéance.
