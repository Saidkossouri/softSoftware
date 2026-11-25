# Data model : kilométrage, entretiens, échéances et justificatifs

**Feature**: `002-entretiens-kilometrage` | **Plan**: [plan.md](./plan.md)

Tables PostgreSQL créées par des changesets Liquibase **nouveaux**, chacun dans son module et
inclus dans le changelog maître de `backend/app` (aucun changeset existant n'est modifié).
Identifiants `uuid` générés par l'application, dates d'audit en `timestamptz`, dates métier en
`date` (jour au Maroc, fuseau `Africa/Casablanca`, [research R8](./research.md)).

## Vue d'ensemble

```text
member 1 ──< mileage_reading >── 1 vehicle 1 ──< maintenance_record >── 1 member
                                     │                │
                                     │                ├──< maintenance_record_type >── 1 maintenance_type
                                     │                └──< document >── 1 member
                                     ├──< maintenance_rule >──────────── 1 maintenance_type
                                     ├──< deadline >──────────────────── 0..1 maintenance_type
                                     └──< deadline_suggestion_dismissal >── 1 maintenance_type
```

## Modifications de `vehicle` (module `vehicle`, changeset `005-add-vehicle-current-mileage`)

| Colonne | Type | Règle |
|---|---|---|
| `current_mileage_km` | integer, non nul | ≥ 0 ; initialisé à `initial_mileage_km` |
| `current_mileage_date` | date, non nul | initialisé à `created_at::date` |

Mises à jour uniquement par `VehicleService.updateCurrentMileage`, appelé par `maintenance`
après chaque écriture (FR-008, [R3](./research.md)). À la création d'un véhicule : kilométrage
de départ et date du jour. À la modification du kilométrage de départ d'un véhicule sans
aucune saisie, le kilométrage actuel suit.

## `mileage_reading` (module `maintenance`, changeset `006-create-mileage-reading`)

| Colonne | Type | Règle |
|---|---|---|
| `id` | uuid | clé primaire |
| `vehicle_id` | uuid, non nul | FK `vehicle`, `ON DELETE CASCADE` (aucun véhicule n'est supprimé aujourd'hui) |
| `reading_date` | date, non nul | ≤ aujourd'hui |
| `mileage_km` | integer, non nul | `CHECK (mileage_km >= 0)` ; cohérence FR-006 vérifiée par le service |
| `note` | varchar(200) | facultatif |
| `author_id` | uuid | FK `member`, `ON DELETE SET NULL` |
| `author_name` | varchar(100), non nul | nom affiché (ou email) figé à la création |
| `created_at`, `updated_at` | timestamptz, non nul | |

Index : `(vehicle_id, reading_date)`.

## `maintenance_type` (module `maintenance`, changeset `007-create-maintenance-type`)

Catalogue géré par l'ADMIN (US6, FR-036 à FR-040).

| Colonne | Type | Règle |
|---|---|---|
| `id` | uuid | clé primaire ; identifiants fixes pour les types pré-remplis |
| `name` | varchar(60), non nul | espaces retirés aux extrémités, non vide |
| `name_key` | varchar(60), non nul, unique | nom en minuscules, espaces internes réduits (409 `TYPE_NAME_ALREADY_EXISTS`) |
| `category` | varchar(15), non nul | `CHECK IN ('MAINTENANCE','ADMINISTRATIVE')` ; non modifiable |
| `default_interval_km` | integer | 1 à 1 000 000 ; `MAINTENANCE` seulement (`CHECK`) |
| `default_interval_months` | smallint | 1 à 240 |
| `requires_description` | boolean, non nul, défaut faux | vrai seulement pour `MAINTENANCE` (`CHECK`) |
| `suggest_from_age_years` | smallint | 0 à 50 ; `ADMINISTRATIVE` seulement (`CHECK`) |
| `display_order` | smallint, non nul | ordre dans la catégorie |
| `active` | boolean, non nul, défaut vrai | |
| `created_at`, `updated_at` | timestamptz, non nul | |

Données initiales, insérées par le même changeset (valeurs indicatives pour le Maroc, voir
[spec, Assumptions](./spec.md)) :

| Ordre | Nom | Catégorie | km | mois | Autre |
|---|---|---|---|---|---|
| 1 | Vidange | MAINTENANCE | 10 000 | 12 | |
| 2 | Filtre à huile | MAINTENANCE | 10 000 | 12 | |
| 3 | Filtre à air | MAINTENANCE | 20 000 | 24 | |
| 4 | Filtre d'habitacle | MAINTENANCE | 15 000 | 12 | |
| 5 | Filtre à carburant | MAINTENANCE | 40 000 | 24 | |
| 6 | Révision | MAINTENANCE | 20 000 | 12 | |
| 7 | Pneus | MAINTENANCE | 40 000 | | |
| 8 | Freins | MAINTENANCE | 30 000 | | |
| 9 | Distribution | MAINTENANCE | 80 000 | 60 | |
| 10 | Batterie | MAINTENANCE | | 48 | |
| 11 | Climatisation | MAINTENANCE | | 24 | |
| 12 | Réparation | MAINTENANCE | | | |
| 13 | Autre | MAINTENANCE | | | description obligatoire |
| 1 | Assurance | ADMINISTRATIVE | | 12 | suggérée dès 0 an |
| 2 | Vignette | ADMINISTRATIVE | | 12 | suggérée dès 0 an |
| 3 | Visite technique | ADMINISTRATIVE | | 12 | suggérée dès 5 ans |

## `maintenance_record` (module `maintenance`, changeset `008-create-maintenance-record`)

| Colonne | Type | Règle |
|---|---|---|
| `id` | uuid | clé primaire |
| `vehicle_id` | uuid, non nul | FK `vehicle`, `ON DELETE CASCADE` |
| `record_date` | date, non nul | ≤ aujourd'hui |
| `mileage_km` | integer, non nul | ≥ 0 ; mêmes règles de cohérence que les relevés |
| `description` | varchar(200) | obligatoire si l'un des types a `requires_description` |
| `cost_cents` | integer | facultatif, `CHECK (cost_cents >= 0)` |
| `garage` | varchar(100) | facultatif, texte libre (« Moi-même » possible) |
| `notes` | varchar(2000) | facultatif |
| `author_id` | uuid | FK `member`, `ON DELETE SET NULL` |
| `author_name` | varchar(100), non nul | |
| `created_at`, `updated_at` | timestamptz, non nul | |

Index : `(vehicle_id, record_date)`.

### `maintenance_record_type`

| Colonne | Type | Règle |
|---|---|---|
| `record_id` | uuid, non nul | FK `maintenance_record`, `ON DELETE CASCADE` |
| `type_id` | uuid, non nul | FK `maintenance_type` ; type `MAINTENANCE` actif à la saisie (contrôle du service) |

Clé primaire `(record_id, type_id)` (un type au plus une fois par entretien) ; index
`(type_id)` pour le filtre de l'historique et la dernière réalisation d'une règle. Un type
désactivé plus tard reste lié et affiché.

## `maintenance_rule` (module `schedule`, changeset `009-create-schedule`)

| Colonne | Type | Règle |
|---|---|---|
| `id` | uuid | clé primaire |
| `vehicle_id` | uuid, non nul | FK `vehicle`, `ON DELETE CASCADE` |
| `type_id` | uuid, non nul | FK `maintenance_type` ; type `MAINTENANCE` actif à la création ; non modifiable |
| `interval_km` | integer | 1 à 1 000 000 |
| `interval_months` | smallint | 1 à 240 |
| `baseline_date` | date, non nul | ≤ aujourd'hui ; par défaut aujourd'hui |
| `baseline_mileage_km` | integer, non nul | ≥ 0 ; par défaut le kilométrage actuel |
| `created_at`, `updated_at` | timestamptz, non nul | |

Contraintes : `UNIQUE (vehicle_id, type_id)` (409 `RULE_ALREADY_EXISTS`) ;
`CHECK (interval_km IS NOT NULL OR interval_months IS NOT NULL)`.

## `deadline` (module `schedule`, même changeset)

| Colonne | Type | Règle |
|---|---|---|
| `id` | uuid | clé primaire |
| `vehicle_id` | uuid, non nul | FK `vehicle`, `ON DELETE CASCADE` |
| `type_id` | uuid | FK `maintenance_type`, facultatif ; type `ADMINISTRATIVE` actif à la saisie |
| `label` | varchar(60), non nul | non vide ; nom du type si non fourni |
| `due_date` | date, non nul | peut être passée (saisie d'une échéance déjà en retard) |
| `recurrence_months` | smallint | 1 à 120 ; absent = échéance unique |
| `done_at` | timestamptz | nul tant que l'échéance est à faire |
| `created_at`, `updated_at` | timestamptz, non nul | |

Transition : `à faire → faite` par `POST /deadlines/{id}/complete` (idempotent). Si
`recurrence_months` est renseigné, création dans la même transaction d'une nouvelle échéance
`due_date + recurrence_months`, même libellé et même récurrence. Une échéance faite ne revient
pas à « à faire » (on la supprime ou on en crée une nouvelle) ; seules les échéances à faire
apparaissent dans les échéances calculées.

## `deadline_suggestion_dismissal` (module `schedule`, même changeset)

| Colonne | Type | Règle |
|---|---|---|
| `vehicle_id` | uuid, non nul | FK `vehicle`, `ON DELETE CASCADE` |
| `type_id` | uuid, non nul | FK `maintenance_type` |
| `created_at` | timestamptz, non nul | |

Clé primaire `(vehicle_id, type_id)`. Une suggestion ignorée ne revient pas pour ce véhicule
(FR-040) ; créer ensuite une échéance de ce type reste possible.

## `document` (module `document`, changeset `010-create-document`)

| Colonne | Type | Règle |
|---|---|---|
| `id` | uuid | clé primaire |
| `record_id` | uuid, non nul | FK `maintenance_record` (suppression gérée par le service, [R2](./research.md)) |
| `vehicle_id` | uuid, non nul | copie du véhicule de l'entretien : contrôle d'accès sans jointure, clé de stockage |
| `storage_key` | varchar(100), non nul, unique | `<vehicleId>/<documentId>` |
| `file_name` | varchar(120), non nul | nom d'origine nettoyé |
| `content_type` | varchar(30), non nul | `CHECK` : `application/pdf`, `image/jpeg`, `image/png`, `image/webp` |
| `size_bytes` | bigint, non nul | 1 à 10 485 760 |
| `author_id` | uuid | FK `member`, `ON DELETE SET NULL` |
| `author_name` | varchar(100), non nul | |
| `created_at` | timestamptz, non nul | |

Index : `(record_id)`. Au plus 10 lignes par `record_id` (contrôle du service sous le verrou du
véhicule).

## Valeurs calculées (jamais stockées)

| Valeur | Calcul | Exposée par |
|---|---|---|
| Kilométrage actuel | point le plus récent (date puis km) parmi relevés et entretiens, sinon kilométrage de départ | `Vehicle.currentMileage`, réponses d'écriture |
| Totaux de dépenses | somme de `cost_cents` sur l'année civile en cours (Maroc) et sur tout l'historique | `VehicleHistory.costTotals` |
| Échéance d'une règle | [research R10](./research.md) | `DueItem` |
| Suggestions d'échéances | types `ADMINISTRATIVE` actifs avec `suggest_from_age_years ≤ année − first_registration_year` (ou = 0 sans année), sans échéance à faire de ce type, non ignorés | `GET /vehicles/{id}/deadline-suggestions` |
| Rappel de relevé | véhicule actif dont `current_mileage_date < aujourd'hui − 30 jours` | `Reminders.mileageReadings` |
| `canEdit` / `canDelete` | membre connecté auteur, ou ADMIN | réponses de lecture |

## Validation et codes d'erreur

| Cas | Réponse |
|---|---|
| Champ manquant, hors bornes, date future, description manquante, aucun type, type désactivé ou de la mauvaise catégorie, règle sans intervalle, champ de catalogue incompatible avec la catégorie | 400 `VALIDATION_ERROR`, un message par champ |
| Modification du catalogue par un USER, ou `includeInactive` demandé par un USER | 403 `ADMIN_ONLY` |
| Nom de type déjà présent (casse ignorée) | 409 `TYPE_NAME_ALREADY_EXISTS` |
| Véhicule non accessible (ou ressource inexistante pour un USER) | 403 `VEHICLE_FORBIDDEN` |
| Modification ou suppression par un autre que l'auteur, hors ADMIN | 403 `NOT_AUTHOR` |
| Écriture sur un véhicule archivé | 409 `VEHICLE_ARCHIVED` |
| Kilométrage incohérent | 409 `MILEAGE_INCONSISTENT` (message citant le point bloquant) |
| Écart > 100 000 km non confirmé | 409 `MILEAGE_JUMP_NOT_CONFIRMED` |
| Kilométrage de départ supérieur au plus petit kilométrage saisi | 409 `INITIAL_MILEAGE_ABOVE_RECORDS` |
| Deuxième règle pour le même type | 409 `RULE_ALREADY_EXISTS` |
| Fichier > 10 Mo | 413 `DOCUMENT_TOO_LARGE` |
| Format non accepté | 415 `DOCUMENT_TYPE_NOT_ALLOWED` |
| Onzième justificatif | 409 `DOCUMENT_LIMIT_REACHED` |

## Suppressions en cascade

- Entretien supprimé → ses liens de types (FK), puis ses justificatifs : lignes dans la même
  transaction, fichiers après validation ([R2](./research.md)). Le kilométrage actuel et les
  échéances sont recalculés (ces dernières à la lecture suivante).
- Membre supprimé (fonctionnalité 001) → `author_id` mis à nul, `author_name` conservé.
- Type d'entretien : jamais supprimé (désactivation) ; les FK vers `maintenance_type` sont sans cascade.
- Véhicule : jamais supprimé (archivage) ; les `ON DELETE CASCADE` ne servent qu'en test.
