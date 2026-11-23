# Modèle de données (Phase 1) : Accès sécurisé, membres et parc de véhicules

**Feature**: `001-acces-membres-flotte` | **Date**: 2026-11-23 | **Plan**: [plan.md](./plan.md)

Base PostgreSQL, schéma géré exclusivement par Liquibase
(`src/main/resources/db/changelog/`). Les entités JPA ci-dessous ne sont jamais exposées par
l'API : la conversion vers les DTOs générés passe par un mapper (principe II).

## Vue d'ensemble

```text
member 1 ──── * vehicle_access * ──── 1 vehicle
```

Un membre USER peut avoir plusieurs véhicules ; un véhicule peut être attribué à plusieurs
membres. Les ADMIN voient tout le parc sans attribution.

## Entité `Member` (table `member`)

Une entrée de la liste blanche.

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | `uuid` | PK | Identifiant généré par l'application |
| `email` | `varchar(320)` | NOT NULL, UNIQUE | Email Google normalisé (`trim` + minuscules) |
| `display_name` | `varchar(100)` | NULL | Nom affiché ; repris de Google à la 1re connexion s'il est vide |
| `role` | `varchar(10)` | NOT NULL, CHECK (`role` IN (`ADMIN`, `USER`)) | Rôle |
| `created_at` | `timestamptz` | NOT NULL | Date d'ajout à la liste blanche |
| `last_login_at` | `timestamptz` | NULL | Date de dernière connexion réussie |

Index : `UNIQUE (email)`, index sur `role` (comptage et verrou des ADMIN).

**Règles de validation**

- `email` : obligatoire, format email valide, 320 caractères max, normalisé avant toute
  comparaison ou écriture (FR-005).
- `display_name` : facultatif, 100 caractères max, espaces de bord supprimés ; chaîne vide
  traitée comme absente.
- `role` : `ADMIN` ou `USER` uniquement (FR-009).
- Ajout d'un email déjà présent : refus 409 `EMAIL_ALREADY_EXISTS` (FR-011).
- Toute opération qui laisserait zéro ADMIN : refus 409 `LAST_ADMIN` (FR-012), avec verrou
  `FOR UPDATE` sur les lignes ADMIN (voir [research.md R9](./research.md)).

**Cycle de vie**

```text
(absent) ──ajout par ADMIN──▶ USER ⇄ ADMIN ──retrait par ADMIN──▶ (supprimé)
(absent) ──INITIAL_ADMIN_EMAIL au démarrage──▶ ADMIN
USER ──INITIAL_ADMIN_EMAIL au démarrage──▶ ADMIN
```

Le retrait supprime la ligne et ses attributions (`ON DELETE CASCADE`). Les dates sont gérées
par le service (`created_at` à l'ajout, `last_login_at` à chaque connexion réussie).

## Entité `Vehicle` (table `vehicle`)

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `id` | `uuid` | PK | Identifiant |
| `nickname` | `varchar(50)` | NOT NULL | Surnom (« La Clio de Said ») |
| `make` | `varchar(50)` | NOT NULL | Marque |
| `model` | `varchar(50)` | NOT NULL | Modèle |
| `registration` | `varchar(20)` | NOT NULL | Immatriculation telle qu'affichée (majuscules, espaces de bord retirés) |
| `registration_key` | `varchar(20)` | NOT NULL, UNIQUE | Immatriculation normalisée : majuscules, sans espaces ni tirets |
| `first_registration_year` | `smallint` | NULL, CHECK (≥ 1900) | Année de mise en circulation |
| `initial_mileage_km` | `integer` | NOT NULL, CHECK (≥ 0) | Kilométrage de départ |
| `vin` | `varchar(17)` | NULL | Numéro VIN |
| `energy_type` | `varchar(12)` | NULL, CHECK (valeurs ci-dessous) | Type d'énergie |
| `status` | `varchar(10)` | NOT NULL, CHECK (`ACTIVE`, `ARCHIVED`), défaut `ACTIVE` | Statut |
| `created_at` | `timestamptz` | NOT NULL | Création |
| `updated_at` | `timestamptz` | NOT NULL | Dernière modification |

Valeurs de `energy_type` : `PETROL` (essence), `DIESEL`, `HYBRID` (hybride), `ELECTRIC`
(électrique), `LPG` (GPL), `OTHER` (autre). Les libellés français sont portés par le frontend.

Index : `UNIQUE (registration_key)`, index sur `status`.

**Règles de validation** (FR-015 à FR-017, message par champ en cas d'erreur)

- `nickname`, `make`, `model`, `registration` : obligatoires, non vides après `trim`,
  longueurs max ci-dessus.
- `registration` : la clé normalisée doit contenir de 2 à 20 caractères alphanumériques.
  Aucun format national n'est imposé (véhicules étrangers ou anciens possibles).
- `registration_key` unique parmi tous les véhicules, actifs et archivés : sinon 409
  `REGISTRATION_ALREADY_EXISTS` (FR-016).
- `first_registration_year` : entre 1900 et l'année courante incluse (« année dans le futur »
  refusée).
- `initial_mileage_km` : entier ≥ 0, borne haute 2 000 000.
- `vin` : facultatif ; s'il est fourni, 17 caractères alphanumériques sans `I`, `O`, `Q`,
  stocké en majuscules. Pas d'unicité imposée.
- `energy_type` : facultatif, une des valeurs listées.

**Cycle de vie** (FR-018)

```text
création ──▶ ACTIVE ──archiver──▶ ARCHIVED ──réactiver──▶ ACTIVE
```

Aucune suppression définitive. Archiver un véhicule déjà archivé (ou réactiver un véhicule
actif) est sans effet et renvoie l'état courant (opérations idempotentes). Les attributions
sont conservées pendant l'archivage. Un véhicule archivé reste modifiable par un ADMIN.

## Entité `VehicleAccess` (table `vehicle_access`)

| Colonne | Type | Contraintes | Description |
|---|---|---|---|
| `member_id` | `uuid` | PK, FK → `member.id` ON DELETE CASCADE | Membre |
| `vehicle_id` | `uuid` | PK, FK → `vehicle.id` | Véhicule (jamais supprimé) |
| `granted_at` | `timestamptz` | NOT NULL | Date d'attribution |

**Règles**

- Attribuable à un membre de rôle USER uniquement (FR-020) : `PUT` sur un ADMIN renvoie 409
  `MEMBER_IS_ADMIN`. Les lignes existantes d'un membre promu ADMIN sont conservées, sans effet
  tant qu'il est ADMIN.
- Tous les `vehicleIds` fournis doivent exister (actifs ou archivés), sinon 400 avec erreur de
  champ.
- Le remplacement de l'ensemble des attributions d'un membre se fait dans une seule
  transaction.

## Règle de lecture d'un véhicule (FR-021, FR-022)

| Appelant | Véhicule actif attribué | Actif non attribué | Archivé | Inexistant |
|---|---|---|---|---|
| ADMIN | 200 | 200 | 200 | 404 |
| USER | 200 | 403 | 403 | 403 |

Pour un USER, les trois refus sont identiques (code `VEHICLE_FORBIDDEN`, aucun détail) afin
de ne rien révéler sur le véhicule.

## Tables techniques

- `spring_session` et `spring_session_attributes` : sessions persistées (Spring Session JDBC,
  voir [research.md R5](./research.md)), créées par un changeset Liquibase reprenant le script
  PostgreSQL fourni par Spring Session.
- `databasechangelog` et `databasechangeloglock` : tables internes de Liquibase.

## Changesets Liquibase prévus

1. `001-create-member.yaml` : table `member`.
2. `002-create-vehicle.yaml` : table `vehicle`.
3. `003-create-vehicle-access.yaml` : table `vehicle_access`.
4. `004-create-spring-session.yaml` : tables de session.

Inclus depuis `db/changelog/db.changelog-master.yaml`.
