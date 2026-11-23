# Feature Specification: Accès sécurisé, membres et parc de véhicules

**Feature Branch**: `claude/project-thread-7eh5b8`

**Created**: 2026-11-19

**Status**: Draft

**Input**: User description: "Première tranche fondatrice de carSupervision : connexion avec un compte Google limitée à une liste blanche d'emails (premier administrateur initialisé par INITIAL_ADMIN_EMAIL, refus 403 pour tout compte non autorisé), deux rôles ADMIN et USER, et un espace d'administration permettant de gérer les membres de la famille, leurs rôles et le parc de véhicules, ainsi que les véhicules auxquels chaque membre a accès."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Se connecter avec son compte Google (Priority: P1)

Un membre de la famille ouvre l'application, choisit « Se connecter avec Google » et arrive sur
l'accueil de l'application s'il fait partie de la liste blanche. Toute personne dont l'email ne
figure pas dans la liste blanche est refusée avec un message clair, sans voir aucune donnée.
Au tout premier démarrage, l'administrateur désigné par la configuration de déploiement
(`INITIAL_ADMIN_EMAIL`) peut se connecter immédiatement avec le rôle ADMIN.

**Why this priority**: sans connexion contrôlée, aucune autre fonctionnalité ne peut être
ouverte en toute sécurité. C'est le socle exigé par la constitution (principe III).

**Independent Test**: déployer l'application vide avec `INITIAL_ADMIN_EMAIL` renseigné, se
connecter avec ce compte (accès accordé, rôle ADMIN), puis avec un autre compte Google
(accès refusé).

**Acceptance Scenarios**:

1. **Given** une installation neuve avec `INITIAL_ADMIN_EMAIL = said@example.com`, **When** ce
   compte se connecte avec Google, **Then** il accède à l'application avec le rôle ADMIN.
2. **Given** un compte Google dont l'email n'est pas dans la liste blanche, **When** il se
   connecte, **Then** l'accès est refusé (403 Forbidden) avec une page expliquant qu'il doit
   demander l'accès à un administrateur, et aucune donnée n'est affichée.
3. **Given** un membre autorisé connecté, **When** il se déconnecte, **Then** sa session est
   fermée et toute page protégée redemande une connexion.
4. **Given** une personne non connectée, **When** elle ouvre une page ou appelle un service de
   l'application, **Then** elle est invitée à se connecter et n'obtient aucune donnée.

---

### User Story 2 - Gérer les membres et leurs rôles (Priority: P2)

L'administrateur ajoute un membre de la famille en saisissant son email Google et en lui
attribuant le rôle USER ou ADMIN. Il peut ensuite changer son rôle ou retirer son accès. Le
membre ajouté peut se connecter dès son ajout ; un membre retiré perd l'accès immédiatement.

**Why this priority**: c'est ce qui permet d'ouvrir l'application à la famille au-delà du
premier administrateur.

**Independent Test**: en tant qu'ADMIN, ajouter un email, vérifier que ce compte se connecte
avec le bon rôle, puis le retirer et vérifier que l'accès est refusé.

**Acceptance Scenarios**:

1. **Given** un ADMIN connecté, **When** il ajoute `epouse@example.com` avec le rôle USER,
   **Then** ce membre apparaît dans la liste et peut se connecter avec le rôle USER.
2. **Given** un membre USER existant, **When** l'ADMIN le passe en ADMIN, **Then** ce membre a
   les droits d'administration dès sa requête suivante.
3. **Given** un membre existant, **When** l'ADMIN retire son accès, **Then** sa prochaine
   requête est refusée (403), y compris s'il avait une session ouverte.
4. **Given** un seul ADMIN dans la liste, **When** il tente de se retirer ou de passer en USER,
   **Then** l'action est refusée avec un message expliquant qu'il faut au moins un ADMIN.
5. **Given** un USER connecté, **When** il tente d'accéder à la gestion des membres (écran ou
   service), **Then** l'accès est refusé (403).
6. **Given** un email déjà présent dans la liste (quelle que soit la casse), **When** l'ADMIN
   l'ajoute à nouveau, **Then** l'ajout est refusé avec un message indiquant le doublon.

---

### User Story 3 - Gérer le parc de véhicules (Priority: P3)

L'administrateur enregistre les véhicules de la famille (surnom, marque, modèle,
immatriculation, année, kilométrage de départ), les modifie, et archive ceux qui ne sont plus
utilisés. Les véhicules archivés n'apparaissent plus dans la liste courante mais restent
consultables par l'ADMIN.

**Why this priority**: le véhicule est l'objet central auquel s'accrocheront kilométrages,
entretiens, échéances et justificatifs dans les fonctionnalités suivantes.

**Independent Test**: en tant qu'ADMIN, créer deux véhicules, en modifier un, en archiver un,
et vérifier la liste obtenue.

**Acceptance Scenarios**:

1. **Given** un ADMIN connecté, **When** il crée un véhicule avec un surnom, une marque, un
   modèle, une immatriculation et un kilométrage de départ, **Then** le véhicule apparaît dans
   le parc.
2. **Given** un véhicule existant, **When** l'ADMIN modifie son immatriculation ou son surnom,
   **Then** les nouvelles valeurs sont affichées partout.
3. **Given** une immatriculation déjà utilisée par un autre véhicule, **When** l'ADMIN tente de
   la réutiliser, **Then** l'enregistrement est refusé avec un message indiquant le doublon.
4. **Given** un véhicule actif, **When** l'ADMIN l'archive, **Then** il disparaît de la liste
   courante, reste visible dans la vue « archivés » de l'ADMIN et peut être réactivé.
5. **Given** un USER connecté, **When** il tente de créer, modifier ou archiver un véhicule,
   **Then** l'action est refusée (403).

---

### User Story 4 - Donner à chaque membre accès à ses véhicules (Priority: P4)

L'administrateur choisit, pour chaque membre USER, les véhicules auxquels il a accès. Un USER
connecté ne voit que ces véhicules ; un ADMIN voit tout le parc.

**Why this priority**: la constitution (principe IV) impose qu'un USER ne voie que les
véhicules qui lui sont autorisés ; cette règle doit exister avant que l'on saisisse des
entretiens ou des kilométrages.

**Independent Test**: créer deux véhicules, autoriser un USER sur un seul, se connecter avec
ce USER et vérifier qu'il ne voit et n'obtient que celui-là.

**Acceptance Scenarios**:

1. **Given** un USER autorisé sur le véhicule A seulement, **When** il consulte la liste des
   véhicules, **Then** il ne voit que le véhicule A.
2. **Given** ce même USER, **When** il demande directement le véhicule B (par exemple via son
   adresse ou son identifiant), **Then** l'accès est refusé (403) et aucune information sur B
   n'est renvoyée.
3. **Given** un USER sans aucun véhicule autorisé, **When** il se connecte, **Then** il voit un
   message lui indiquant qu'aucun véhicule ne lui est encore attribué.
4. **Given** un USER autorisé sur A, **When** l'ADMIN lui retire A, **Then** dès sa requête
   suivante A n'est plus visible ni accessible.
5. **Given** un véhicule archivé, **When** un USER autorisé sur ce véhicule consulte sa liste,
   **Then** le véhicule n'apparaît pas dans sa liste courante.

---

### Edge Cases

- `INITIAL_ADMIN_EMAIL` absent au démarrage alors qu'aucun ADMIN n'existe en base : l'application
  refuse de démarrer avec un message d'erreur explicite, plutôt que de rester inaccessible.
- `INITIAL_ADMIN_EMAIL` désigne un email déjà présent avec le rôle USER : au démarrage, ce membre
  est remis en ADMIN (l'email de configuration garantit toujours un accès administrateur).
- `INITIAL_ADMIN_EMAIL` change entre deux démarrages : le nouvel email est ajouté comme ADMIN,
  l'ancien n'est ni supprimé ni modifié.
- Email saisi avec une casse ou des espaces différents (`Said@Example.com `) : il est normalisé
  (minuscules, sans espaces) et comparé de façon insensible à la casse.
- Compte Google dont l'email n'est pas vérifié par Google : l'accès est refusé (403).
- Membre retiré pendant qu'il a une session ouverte : sa requête suivante est refusée.
- Deux ADMIN qui se rétrogradent ou se retirent mutuellement en même temps : la règle « au moins
  un ADMIN » reste garantie.
- Archivage d'un véhicule : ses autorisations d'accès sont conservées et redeviennent effectives
  s'il est réactivé.
- Saisie invalide (email mal formé, kilométrage négatif, année dans le futur, champs
  obligatoires vides) : refus avec un message par champ, sans perte de la saisie.

## Requirements *(mandatory)*

### Functional Requirements

**Authentification et liste blanche**

- **FR-001**: Le système DOIT permettre la connexion uniquement via un compte Google ; il ne
  DOIT gérer ni stocker aucun mot de passe.
- **FR-002**: Le système DOIT n'accorder l'accès qu'aux comptes Google dont l'email (vérifié par
  Google) figure dans la liste blanche ; tout autre compte DOIT recevoir un refus 403 Forbidden
  et une page expliquant comment demander l'accès.
- **FR-003**: Le système DOIT conserver la liste blanche de façon persistante.
- **FR-004**: Au démarrage, le système DOIT garantir que l'email défini par `INITIAL_ADMIN_EMAIL`
  est présent dans la liste blanche avec le rôle ADMIN ; si cette variable est absente et
  qu'aucun ADMIN n'existe, le système DOIT refuser de démarrer avec un message explicite.
- **FR-005**: Le système DOIT comparer les emails sans tenir compte de la casse ni des espaces
  en début ou fin.
- **FR-006**: Toute page et tout service, hors page de connexion et page de refus, DOIT exiger
  une session authentifiée et autorisée.
- **FR-007**: Les droits d'un membre (présence dans la liste blanche, rôle, véhicules autorisés)
  DOIVENT être réévalués à chaque requête, de sorte qu'un retrait ou un changement prenne
  effet immédiatement, même pour une session déjà ouverte.
- **FR-008**: Un membre connecté DOIT pouvoir se déconnecter.

**Rôles et membres**

- **FR-009**: Le système DOIT gérer exactement deux rôles : ADMIN et USER.
- **FR-010**: Un ADMIN DOIT pouvoir lister les membres, ajouter un membre (email, nom affiché
  facultatif, rôle), modifier son rôle et retirer son accès.
- **FR-011**: Le système DOIT refuser l'ajout d'un email déjà présent dans la liste blanche.
- **FR-012**: Le système DOIT refuser toute action qui laisserait la liste blanche sans aucun
  ADMIN.
- **FR-013**: Toutes les fonctions d'administration (membres, rôles, parc, attributions) DOIVENT
  être refusées (403) à un USER, le contrôle étant fait côté serveur et pas seulement masqué
  dans l'interface.
- **FR-014**: Le système DOIT afficher à chaque membre connecté son nom, son email et son rôle.

**Parc de véhicules**

- **FR-015**: Un ADMIN DOIT pouvoir créer un véhicule avec : surnom (obligatoire), marque
  (obligatoire), modèle (obligatoire), immatriculation (obligatoire, unique), année de mise en
  circulation (facultative), kilométrage de départ (obligatoire, entier positif ou nul), numéro
  VIN (facultatif) et type d'énergie (facultatif : essence, diesel, hybride, électrique, GPL,
  autre).
- **FR-016**: Le système DOIT refuser une immatriculation déjà utilisée par un autre véhicule,
  actif ou archivé (comparaison sans tenir compte de la casse, des espaces ni des tirets).
- **FR-017**: Un ADMIN DOIT pouvoir modifier toutes les informations d'un véhicule.
- **FR-018**: Un ADMIN DOIT pouvoir archiver et réactiver un véhicule ; aucun véhicule n'est
  supprimé définitivement dans cette fonctionnalité.
- **FR-019**: La liste courante des véhicules DOIT exclure les véhicules archivés ; un ADMIN
  DOIT pouvoir afficher les véhicules archivés séparément.

**Accès aux véhicules**

- **FR-020**: Un ADMIN DOIT pouvoir attribuer et retirer à chaque membre USER l'accès à un ou
  plusieurs véhicules.
- **FR-021**: Un ADMIN DOIT avoir accès à tous les véhicules sans attribution explicite.
- **FR-022**: Un USER NE DOIT voir et obtenir que les véhicules actifs qui lui sont attribués ;
  toute demande portant sur un autre véhicule DOIT être refusée (403) sans révéler
  d'information sur ce véhicule.
- **FR-023**: Un USER sans véhicule attribué DOIT voir un message l'indiquant au lieu d'une
  liste vide sans explication.

**Interface**

- **FR-024**: Tous les écrans de cette fonctionnalité DOIVENT être utilisables sur téléphone
  (largeur 360 px) comme sur ordinateur.
- **FR-025**: Les messages d'erreur DOIVENT être en français et indiquer à l'utilisateur quoi
  faire.

### Key Entities

- **Membre autorisé** : une entrée de la liste blanche. Email (unique, normalisé), nom affiché,
  rôle (ADMIN ou USER), date d'ajout, date de dernière connexion. Le nom affiché peut être
  repris du compte Google à la première connexion s'il n'a pas été saisi.
- **Véhicule** : un véhicule du parc familial. Surnom, marque, modèle, immatriculation
  (unique), année, kilométrage de départ, VIN, type d'énergie, statut (actif ou archivé),
  dates de création et de modification.
- **Accès véhicule** : le lien entre un membre USER et un véhicule qu'il peut consulter et sur
  lequel il pourra saisir des entretiens et kilométrages. Un membre peut avoir plusieurs
  véhicules et un véhicule plusieurs membres.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Sur une installation neuve, l'administrateur initial accède à l'application en
  moins de 1 minute après le premier démarrage, sans aucune manipulation en base.
- **SC-002**: 100 % des tentatives de connexion par un compte Google absent de la liste blanche
  sont refusées sans qu'aucune donnée ne soit affichée ni renvoyée.
- **SC-003**: 100 % des actions d'administration tentées par un USER, et 100 % des demandes
  d'un USER portant sur un véhicule qui ne lui est pas attribué, sont refusées, y compris en
  appelant directement les services sans passer par l'interface.
- **SC-004**: Un ADMIN ajoute un nouveau membre et lui attribue un véhicule en moins de
  2 minutes, depuis un téléphone.
- **SC-005**: Un ADMIN enregistre un nouveau véhicule en moins de 2 minutes, depuis un
  téléphone.
- **SC-006**: Le retrait d'un membre ou d'une attribution prend effet dès la requête suivante
  de ce membre (aucun délai d'attente).
- **SC-007**: Il est impossible d'aboutir à une liste blanche sans ADMIN, quelle que soit la
  suite d'actions effectuée.

## Assumptions

- L'accès d'un USER à un véhicule est attribué explicitement par un ADMIN, véhicule par
  véhicule ; il n'existe pas de notion de « propriétaire » distincte dans cette fonctionnalité.
- Il n'y a pas de demande d'accès en libre-service ni d'invitation par email : l'ADMIN prévient
  lui-même le membre ajouté, qui se connecte avec son compte Google.
- Les véhicules sont archivés plutôt que supprimés, pour préserver l'historique d'entretien que
  les fonctionnalités suivantes y rattacheront.
- La saisie des kilométrages, des entretiens, des échéances et des justificatifs est hors
  périmètre ; cette fonctionnalité n'enregistre que le kilométrage de départ du véhicule.
- Les référentiels (types d'entretien, etc.) mentionnés dans la constitution seront traités
  avec la fonctionnalité d'historique des entretiens.
- La journalisation détaillée des actions d'administration (audit) est hors périmètre ; seules
  les dates d'ajout et de dernière connexion des membres sont conservées.
- Volumétrie conforme à la constitution : 2 à 5 membres et 2 à 5 véhicules ; ni pagination ni
  recherche avancée ne sont nécessaires.
- L'interface est en français uniquement.
