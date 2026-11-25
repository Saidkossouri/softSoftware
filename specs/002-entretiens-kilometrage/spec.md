# Feature Specification: Suivi du kilométrage et historique des entretiens

**Feature Branch**: `feature/002-entretiens-kilometrage`

**Created**: 2026-11-25

**Status**: Draft

**Input**: User description: "Fonctionnalité 002 : suivi du kilométrage et historique des entretiens par véhicule. Les membres relèvent le kilométrage de leurs véhicules et enregistrent les entretiens réalisés (type, date, kilométrage, coût, garage), sur les seuls véhicules auxquels ils ont accès. Les échéances et rappels, ainsi que les justificatifs (factures), font l'objet de fonctionnalités suivantes."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Relever le kilométrage d'un véhicule (Priority: P1)

Un membre, à la station ou en garant la voiture, ouvre l'application sur son téléphone, choisit
un de ses véhicules et saisit le kilométrage affiché au compteur. Le kilométrage actuel du
véhicule est mis à jour et l'historique des relevés est conservé.

**Why this priority**: le kilométrage est la donnée la plus fréquente à saisir et la base de
tout le reste (kilométrage d'un entretien, futures échéances au kilométrage). C'est aussi la
tranche la plus petite qui apporte déjà de la valeur.

**Independent Test**: avec un USER autorisé sur un véhicule de kilométrage de départ 50 000 km,
saisir 51 200 km depuis un téléphone, vérifier que le véhicule affiche 51 200 km et que le relevé
apparaît dans l'historique.

**Acceptance Scenarios**:

1. **Given** un USER autorisé sur le véhicule A (kilométrage actuel 50 000 km), **When** il
   saisit un relevé de 51 200 km daté d'aujourd'hui, **Then** le véhicule A affiche un
   kilométrage actuel de 51 200 km et la date du relevé.
2. **Given** le véhicule A à 51 200 km relevés hier, **When** un membre saisit 50 900 km daté
   d'aujourd'hui, **Then** la saisie est refusée avec un message indiquant qu'un kilométrage ne
   peut pas diminuer et rappelant le dernier relevé connu.
3. **Given** un relevé saisi par erreur, **When** son auteur (ou un ADMIN) le corrige ou le
   supprime, **Then** le kilométrage actuel du véhicule est recalculé.
4. **Given** un USER non autorisé sur le véhicule B, **When** il tente de consulter ou de
   saisir un relevé sur B (y compris en appelant directement le service), **Then** l'accès est
   refusé (403) sans révéler d'information sur B.
5. **Given** un relevé daté dans le futur, **When** un membre l'enregistre, **Then** la saisie
   est refusée avec un message sur le champ date.

---

### User Story 2 - Enregistrer un entretien réalisé (Priority: P2)

Après un passage au garage, un membre enregistre l'entretien : date, kilométrage, nature des
opérations (par exemple vidange et filtre à huile), coût, garage et remarques. Le kilométrage
saisi compte aussi comme un relevé du véhicule.

**Why this priority**: c'est le cœur de l'application (historique des entretiens), et la
donnée dont les futures échéances auront besoin (« dernière vidange à telle date et tel
kilométrage »).

**Independent Test**: enregistrer une révision (vidange + filtres, 189 €, garage « Garage du
Centre ») sur un véhicule autorisé, puis la retrouver dans l'historique du véhicule avec toutes
ses informations.

**Acceptance Scenarios**:

1. **Given** un USER autorisé sur le véhicule A, **When** il enregistre un entretien daté du
   jour, à 52 000 km, avec les opérations « Vidange » et « Filtre à huile », un coût de
   189,00 € et le garage « Garage du Centre », **Then** l'entretien apparaît en tête de
   l'historique de A avec ces informations et l'auteur de la saisie.
2. **Given** ce même entretien à 52 000 km plus récent que le dernier relevé, **When** il est
   enregistré, **Then** le kilométrage actuel de A passe à 52 000 km.
3. **Given** un entretien de type « Autre », **When** le membre ne précise pas de description,
   **Then** la saisie est refusée avec un message demandant de décrire l'opération.
4. **Given** un entretien saisi par un membre, **When** son auteur ou un ADMIN le modifie ou le
   supprime (après confirmation), **Then** l'historique et le kilométrage actuel sont mis à
   jour.
5. **Given** un entretien saisi par un autre membre, **When** un USER tente de le modifier ou
   de le supprimer, **Then** l'action est refusée (403) ; il peut seulement le consulter.
6. **Given** un entretien ancien à saisir après coup (par exemple d'après un vieux carnet),
   **When** le membre saisit une date et un kilométrage passés cohérents avec les relevés
   existants, **Then** l'entretien est rangé à sa place chronologique sans changer le
   kilométrage actuel.

---

### User Story 3 - Consulter l'historique d'un véhicule (Priority: P3)

Un membre ouvre la fiche d'un de ses véhicules et voit en un coup d'œil son kilométrage actuel,
la date du dernier relevé, et un historique chronologique des entretiens et relevés, avec le
total des dépenses d'entretien de l'année en cours et depuis le début du suivi. Il peut filtrer
l'historique par type d'opération (par exemple pour retrouver la dernière vidange).

**Why this priority**: la consultation donne son sens à la saisie ; elle est livrable dès que
les deux premières histoires existent.

**Independent Test**: sur un véhicule ayant 3 entretiens et 5 relevés, ouvrir la fiche et
vérifier l'ordre chronologique, les totaux et le filtre « Vidange ».

**Acceptance Scenarios**:

1. **Given** un véhicule avec des entretiens et des relevés, **When** un membre autorisé ouvre
   sa fiche, **Then** il voit le kilométrage actuel, la date du dernier relevé et l'historique
   du plus récent au plus ancien.
2. **Given** des entretiens de 120 € et 300 € cette année et de 450 € l'année dernière, **When**
   le membre consulte la fiche, **Then** il voit un total de 420 € pour l'année en cours et
   870 € depuis le début du suivi ; les entretiens sans coût ne sont pas comptés.
3. **Given** l'historique d'un véhicule, **When** le membre filtre sur « Vidange », **Then**
   seuls les entretiens comportant une vidange sont affichés, le plus récent en premier.
4. **Given** un véhicule sans aucune saisie, **When** un membre ouvre sa fiche, **Then** il
   voit le kilométrage de départ et un message l'invitant à saisir un premier relevé ou
   entretien.
5. **Given** la liste « Mes véhicules », **When** un membre la consulte, **Then** chaque
   véhicule affiche son kilométrage actuel.

---

### Edge Cases

- Deux relevés le même jour : autorisés s'ils sont croissants dans l'ordre de saisie ; le plus
  élevé fait foi pour ce jour.
- Relevé ou entretien rétroactif incohérent : un kilométrage daté entre deux relevés existants
  doit être compris entre ces deux valeurs, sinon la saisie est refusée avec un message citant
  le relevé qui bloque.
- Kilométrage inférieur au kilométrage de départ du véhicule : refusé.
- Modification par l'ADMIN du kilométrage de départ d'un véhicule déjà suivi : refusée si la
  nouvelle valeur dépasse le plus petit kilométrage déjà saisi.
- Saut de kilométrage invraisemblable (plus de 100 000 km d'écart avec le relevé précédent) :
  la saisie demande une confirmation (faute de frappe probable, par exemple un zéro en trop),
  sans être interdite.
- Véhicule archivé : son historique reste consultable par l'ADMIN, mais aucune nouvelle saisie
  n'est possible tant qu'il n'est pas réactivé ; un USER ne le voit plus (règle de la
  fonctionnalité 001).
- Retrait de l'accès d'un USER à un véhicule : il ne voit plus ce véhicule ni son historique,
  mais ses saisies passées restent dans l'historique, avec son nom comme auteur.
- Membre retiré de la liste blanche : ses saisies passées sont conservées et affichent son nom.
- Coût négatif, montant avec plus de 2 décimales, date dans le futur, kilométrage non entier ou
  négatif, aucune opération cochée : refus avec un message par champ, sans perte de la saisie.
- Saisie interrompue (réseau perdu au garage) : un message clair indique que l'enregistrement
  n'a pas abouti et la saisie reste dans le formulaire pour un nouvel essai.

## Requirements *(mandatory)*

### Functional Requirements

**Accès**

- **FR-001**: Tout membre ayant accès à un véhicule (USER autorisé sur ce véhicule, ou ADMIN)
  DOIT pouvoir consulter son kilométrage et son historique, et y saisir des relevés et des
  entretiens.
- **FR-002**: Toute demande portant sur le kilométrage ou les entretiens d'un véhicule auquel le
  membre n'a pas accès DOIT être refusée (403) sans révéler d'information, le contrôle étant
  fait côté serveur et réévalué à chaque requête.
- **FR-003**: Seul l'auteur d'une saisie ou un ADMIN DOIT pouvoir la modifier ou la supprimer ;
  les autres membres ayant accès au véhicule la consultent seulement.
- **FR-004**: Aucune saisie (création, modification, suppression) NE DOIT être possible sur un
  véhicule archivé.

**Kilométrage**

- **FR-005**: Un membre DOIT pouvoir enregistrer un relevé kilométrique : date (obligatoire, par
  défaut aujourd'hui, pas dans le futur), kilométrage (obligatoire, entier), remarque
  (facultative).
- **FR-006**: Le système DOIT garantir la cohérence chronologique : en tenant compte de tous les
  relevés et entretiens du véhicule, un kilométrage NE DOIT PAS être inférieur à un kilométrage
  daté avant lui ni supérieur à un kilométrage daté après lui, ni inférieur au kilométrage de
  départ du véhicule.
- **FR-007**: Le système DOIT demander une confirmation explicite avant d'enregistrer un
  kilométrage qui dépasse de plus de 100 000 km le kilométrage précédent.
- **FR-008**: Le kilométrage actuel d'un véhicule DOIT être la valeur la plus récente parmi son
  kilométrage de départ, ses relevés et les kilométrages de ses entretiens ; il DOIT être
  affiché avec sa date et recalculé après toute création, modification ou suppression.
- **FR-009**: Le système DOIT refuser une modification du kilométrage de départ d'un véhicule
  qui le rendrait supérieur au plus petit kilométrage déjà saisi pour ce véhicule.

**Entretiens**

- **FR-010**: Un membre DOIT pouvoir enregistrer un entretien avec : date (obligatoire, pas dans
  le futur), kilométrage (obligatoire, soumis aux mêmes règles de cohérence que les relevés),
  une ou plusieurs opérations (obligatoire), coût total TTC en euros (facultatif, positif ou
  nul, 2 décimales au plus), garage ou intervenant (facultatif, texte libre, « Moi-même »
  possible) et remarques (facultatives).
- **FR-011**: Les opérations DOIVENT être choisies dans une liste fixe : Révision, Vidange,
  Filtre à huile, Filtre à air, Filtre d'habitacle, Filtre à carburant, Pneus, Freins,
  Distribution, Batterie, Climatisation, Contrôle technique, Réparation, Autre. L'opération
  « Autre » DOIT être accompagnée d'une description.
- **FR-012**: Chaque relevé et chaque entretien DOIT conserver son auteur et ses dates de
  création et de dernière modification.
- **FR-013**: La suppression d'un entretien ou d'un relevé DOIT être confirmée par
  l'utilisateur ; elle est définitive.

**Consultation**

- **FR-014**: La fiche d'un véhicule DOIT afficher son kilométrage actuel et la date
  correspondante, puis l'historique des entretiens et des relevés du plus récent au plus
  ancien (à date égale, le plus grand kilométrage en premier).
- **FR-015**: La fiche DOIT afficher le total des coûts d'entretien de l'année civile en cours
  et le total depuis le début du suivi.
- **FR-016**: L'historique DOIT pouvoir être filtré par opération et pouvoir masquer les
  relevés simples pour n'afficher que les entretiens.
- **FR-017**: La liste des véhicules d'un membre DOIT afficher le kilométrage actuel de chaque
  véhicule et donner accès en un geste à la saisie d'un relevé.
- **FR-018**: Un ADMIN DOIT pouvoir consulter l'historique d'un véhicule archivé.

**Interface**

- **FR-019**: Les saisies de relevé et d'entretien DOIVENT être utilisables d'une main sur
  téléphone (largeur 360 px), avec un clavier numérique pour les kilométrages et montants.
- **FR-020**: Les montants DOIVENT être affichés au format français (`1 234,50 €`) et les
  kilométrages avec séparateur de milliers (`52 000 km`) ; les messages d'erreur sont en
  français et indiquent quoi faire.

### Key Entities

- **Relevé kilométrique** : un kilométrage constaté sur un véhicule à une date. Véhicule, date,
  kilométrage, remarque, auteur, dates de création et de modification.
- **Entretien** : une intervention réalisée sur un véhicule. Véhicule, date, kilométrage,
  opérations (une ou plusieurs), description (obligatoire pour « Autre »), coût total,
  garage ou intervenant, remarques, auteur, dates de création et de modification. Son
  kilométrage participe au calcul du kilométrage actuel au même titre qu'un relevé.
- **Opération** : un élément de la liste fixe des natures d'entretien (vidange, pneus,
  contrôle technique…). Elle servira de point d'accroche aux échéances de la fonctionnalité
  suivante.
- **Véhicule** (existant, fonctionnalité 001) : gagne un kilométrage actuel calculé et la date
  de ce kilométrage.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un membre saisit un relevé kilométrique en moins de 20 secondes depuis
  l'ouverture de l'application sur un téléphone.
- **SC-002**: Un membre enregistre un entretien complet (opérations, coût, garage) en moins de
  1 minute 30 depuis un téléphone.
- **SC-003**: 100 % des saisies qui feraient diminuer le kilométrage dans le temps sont
  refusées avec un message indiquant le relevé en conflit.
- **SC-004**: 100 % des demandes d'un USER portant sur l'historique d'un véhicule qui ne lui est
  pas attribué, et 100 % de ses tentatives de modifier la saisie d'un autre membre, sont
  refusées, y compris en appelant directement les services.
- **SC-005**: Un membre retrouve la date et le kilométrage de la dernière vidange d'un véhicule
  en moins de 10 secondes depuis sa fiche.
- **SC-006**: Les totaux de dépenses affichés correspondent exactement à la somme des coûts
  saisis (aucun écart d'arrondi).

## Assumptions

- **Périmètre** : cette fonctionnalité couvre les relevés kilométriques et l'historique des
  entretiens. Les échéances et rappels (par date ou par kilométrage, y compris assurance et
  contrôle technique à venir) font l'objet de la fonctionnalité 003 ; les justificatifs et
  factures (PDF, photos) de la fonctionnalité 004.
- La liste des opérations est fixe dans cette fonctionnalité. Sa gestion par l'ADMIN
  (référentiel prévu par la constitution), avec les intervalles par défaut (par exemple
  vidange tous les 15 000 km ou 1 an), arrivera avec les échéances, où elle prend tout son
  sens.
- L'assurance n'est pas un entretien (pas de kilométrage, c'est une échéance) : elle est
  traitée en 003.
- Un entretien a un seul coût total ; la ventilation du coût par opération n'est pas
  nécessaire pour un usage familial.
- Le garage est un texte libre ; il n'y a pas de carnet d'adresses de garages.
- Les montants sont en euros uniquement.
- La consommation de carburant (pleins) n'est pas suivie.
- Pas d'export (CSV, PDF) ni de graphique dans cette fonctionnalité.
- Volumétrie conforme à la constitution : quelques centaines de saisies par véhicule au plus ;
  ni pagination ni recherche avancée ne sont nécessaires.
- Les règles d'accès aux véhicules (attributions, archivage) sont celles de la fonctionnalité
  001 et ne changent pas.
