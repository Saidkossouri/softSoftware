# Feature Specification: Kilométrage, entretiens, échéances et justificatifs

**Feature Branch**: `feature/002-entretiens-kilometrage`

**Created**: 2026-11-25

**Status**: Draft

**Input**: User description: "Fonctionnalité 002 : suivi du kilométrage et historique des entretiens par véhicule. Les membres relèvent le kilométrage de leurs véhicules et enregistrent les entretiens réalisés (type, date, kilométrage, coût, garage), sur les seuls véhicules auxquels ils ont accès. Périmètre étendu par le mainteneur (2026-11-25) : la fonctionnalité inclut aussi les échéances et rappels (par date ou par kilométrage) et les justificatifs (factures en PDF, photos), puis un catalogue des types d'entretien et des échéances administratives (assurance, vignette, visite technique) géré par l'ADMIN."

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

**Independent Test**: enregistrer une révision (vidange + filtres, 450 DH, garage « Garage du
Centre ») sur un véhicule autorisé, puis la retrouver dans l'historique du véhicule avec toutes
ses informations.

**Acceptance Scenarios**:

1. **Given** un USER autorisé sur le véhicule A, **When** il enregistre un entretien daté du
   jour, à 52 000 km, avec les opérations « Vidange » et « Filtre à huile », un coût de
   450,00 DH et le garage « Garage du Centre », **Then** l'entretien apparaît en tête de
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
2. **Given** des entretiens de 300 DH et 800 DH cette année et de 1 200 DH l'année dernière, **When**
   le membre consulte la fiche, **Then** il voit un total de 1 100 DH pour l'année en cours et
   2 300 DH depuis le début du suivi ; les entretiens sans coût ne sont pas comptés.
3. **Given** l'historique d'un véhicule, **When** le membre filtre sur « Vidange », **Then**
   seuls les entretiens comportant une vidange sont affichés, le plus récent en premier.
4. **Given** un véhicule sans aucune saisie, **When** un membre ouvre sa fiche, **Then** il
   voit le kilométrage de départ et un message l'invitant à saisir un premier relevé ou
   entretien.
5. **Given** la liste « Mes véhicules », **When** un membre la consulte, **Then** chaque
   véhicule affiche son kilométrage actuel.

---

### User Story 4 - Être prévenu des prochaines échéances (Priority: P4)

Pour chaque véhicule, un membre définit le plan d'entretien : quelles opérations reviennent et
à quel rythme (par exemple vidange tous les 10 000 km ou tous les 12 mois, au premier des deux
termes atteint). Il crée aussi les échéances administratives à date fixe (assurance, vignette,
visite technique), proposées d'après le catalogue de l'ADMIN. L'application calcule la prochaine échéance de
chaque élément à partir du dernier entretien correspondant, et l'accueil signale ce qui arrive
bientôt ou est en retard sur ses véhicules.

**Why this priority**: c'est ce qui évite d'oublier une vidange ou un contrôle technique, la
raison d'être de l'application. Elle s'appuie sur l'historique des histoires 1 à 3.

**Independent Test**: sur un véhicule à 59 200 km dont la dernière vidange date de 9 mois à
45 000 km, créer la règle « Vidange tous les 15 000 km ou 12 mois » et vérifier qu'elle est
annoncée « Bientôt » (échéance à 60 000 km) sur la fiche et l'accueil ; enregistrer une vidange
et vérifier que l'échéance repart.

**Acceptance Scenarios**:

1. **Given** une règle « Vidange tous les 15 000 km ou 12 mois » et une dernière vidange le
   1er mars 2026 à 45 000 km, **When** un membre consulte le véhicule, **Then** la prochaine
   vidange est annoncée pour le 1er mars 2027 ou à 60 000 km, au premier des deux termes atteint.
2. **Given** cette même règle et un kilométrage actuel de 59 200 km, **When** le membre ouvre
   l'accueil, **Then** l'échéance apparaît comme « Bientôt » (moins de 1 000 km restants), avec
   le véhicule et ce qui reste (800 km).
3. **Given** un kilométrage actuel de 60 400 km sans nouvelle vidange, **When** le membre
   consulte le véhicule, **Then** l'échéance apparaît « En retard » de 400 km, en tête de liste.
4. **Given** une échéance en retard, **When** un membre enregistre un entretien comportant
   l'opération « Vidange », **Then** l'échéance est recalculée à partir de ce nouvel entretien.
5. **Given** un véhicule sans aucune vidange enregistrée, **When** un membre crée la règle,
   **Then** il indique la date et le kilométrage de la dernière réalisation connue (par défaut :
   aujourd'hui et le kilométrage actuel), qui servent de point de départ.
6. **Given** une échéance à date fixe « Assurance » au 1er avril 2027, renouvelable chaque
   année, **When** le membre la marque comme faite, **Then** la suivante est créée au 1er avril
   2028.
7. **Given** un USER sans accès au véhicule B, **When** il ouvre l'accueil, **Then** aucune
   échéance de B n'apparaît, et toute demande directe sur les échéances de B est refusée (403).
8. **Given** un véhicule actif dont le dernier kilométrage connu date de plus de 30 jours,
   **When** un membre ayant accès à ce véhicule ouvre l'accueil, **Then** il voit un rappel
   « Pense à relever le kilométrage » pour ce véhicule, avec la date et la valeur du dernier
   relevé et un accès direct à la saisie ; le rappel disparaît dès qu'un relevé ou un entretien
   plus récent est enregistré.
9. **Given** un véhicule mis en circulation il y a 5 ans ou plus, sans échéance « Visite
   technique » à faire, **When** un membre ouvre sa fiche, **Then** une suggestion l'invite à
   créer cette échéance, pré-remplie d'après le catalogue (récurrence 12 mois) ; de même pour
   « Assurance » et « Vignette » sur tout véhicule qui n'en a pas.

---

### User Story 5 - Joindre les factures et justificatifs (Priority: P5)

Au garage, un membre photographie la facture avec son téléphone ou joint le PDF reçu par email,
et l'attache à l'entretien. Plus tard, tout membre ayant accès au véhicule retrouve la facture
depuis l'historique et l'ouvre ou la télécharge (par exemple pour une revente ou une garantie).

**Why this priority**: conserver les preuves d'entretien a de la valeur (revente, garantie,
litige), mais l'historique et les échéances restent utiles sans elles.

**Independent Test**: joindre une photo prise avec le téléphone et un PDF à un entretien, les
rouvrir depuis un autre compte autorisé sur le véhicule, puis vérifier qu'un compte non
autorisé ne peut pas les obtenir, même avec leur adresse directe.

**Acceptance Scenarios**:

1. **Given** un entretien sur un véhicule autorisé, **When** un membre y joint une photo prise
   avec l'appareil du téléphone, **Then** le justificatif apparaît sur l'entretien avec sa
   vignette, son nom, sa taille et sa date d'ajout.
2. **Given** un entretien avec une facture PDF, **When** un autre membre autorisé sur le
   véhicule l'ouvre, **Then** il peut la consulter et la télécharger.
3. **Given** un fichier d'un format non accepté ou de plus de 10 Mo, **When** un membre tente
   de le joindre, **Then** l'envoi est refusé avec un message indiquant les formats acceptés et
   la taille maximale.
4. **Given** un USER non autorisé sur le véhicule, **When** il demande un justificatif de ce
   véhicule, y compris avec son adresse directe, **Then** l'accès est refusé (403) et aucun
   contenu n'est renvoyé.
5. **Given** un justificatif ajouté par un membre, **When** son auteur ou un ADMIN le supprime
   (après confirmation), **Then** il disparaît de l'entretien et n'est plus téléchargeable.
6. **Given** un entretien avec des justificatifs, **When** l'entretien est supprimé, **Then**
   ses justificatifs sont supprimés avec lui, après une confirmation qui en indique le nombre.

---

### User Story 6 - Configurer le catalogue des types d'entretien (Priority: P6)

L'administrateur gère le catalogue des types d'entretien utilisés partout dans l'application :
nom, catégorie (Entretien ou Administratif), fréquence recommandée en kilomètres et/ou en mois,
et, pour les types administratifs, l'âge du véhicule à partir duquel l'échéance est proposée.
Il peut ajouter un type, le renommer, changer ses fréquences, et désactiver un type qui ne sert
plus. Le catalogue est livré pré-rempli avec des valeurs adaptées au Maroc, ce qui permet
d'utiliser les histoires 2 à 4 sans aucune configuration.

**Why this priority**: les fréquences recommandées dépendent du pays, des véhicules et des
habitudes de la famille ; le catalogue pré-rempli suffit pour démarrer, l'écran d'administration
permet ensuite de l'ajuster sans modifier l'application.

**Independent Test**: en ADMIN, passer la fréquence de la vidange à 8 000 km, créer le type
« Courroie d'accessoires » (60 000 km), désactiver « Climatisation », puis vérifier qu'un
membre voit ces changements dans le formulaire d'entretien et lors de la création d'une règle.

**Acceptance Scenarios**:

1. **Given** une installation neuve, **When** un membre ouvre le formulaire d'entretien,
   **Then** il voit les types du catalogue pré-rempli, dont « Vidange » (10 000 km ou 12 mois),
   et dans les échéances administratives « Assurance », « Vignette » et « Visite technique ».
2. **Given** un ADMIN, **When** il change la fréquence de « Vidange » en 8 000 km, **Then** la
   création d'une nouvelle règle « Vidange » propose 8 000 km ; les règles déjà créées gardent
   leurs propres intervalles.
3. **Given** un ADMIN, **When** il ajoute le type « Courroie d'accessoires » (Entretien,
   60 000 km), **Then** il peut être choisi dans les entretiens et les règles.
4. **Given** un type déjà utilisé, **When** l'ADMIN le renomme, **Then** le nouveau nom
   s'affiche partout, y compris dans l'historique.
5. **Given** un type déjà utilisé, **When** l'ADMIN le désactive, **Then** il n'est plus
   proposé pour les nouvelles saisies mais reste affiché sur les entretiens, règles et
   échéances existants ; il peut être réactivé. Aucun type n'est supprimé.
6. **Given** un nom déjà présent dans le catalogue (quelle que soit la casse), **When** l'ADMIN
   l'ajoute à nouveau, **Then** l'ajout est refusé avec un message indiquant le doublon.
7. **Given** un USER, **When** il tente de modifier le catalogue (écran ou service), **Then**
   l'accès est refusé (403) ; il peut seulement le consulter.

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
- Règle d'échéance sans kilométrage ni durée : refusée (au moins l'un des deux est obligatoire).
- Entretien rétroactif plus ancien que la dernière réalisation connue : il ne change pas
  l'échéance, qui part toujours de la réalisation la plus récente.
- Suppression de l'entretien qui servait de point de départ à une échéance : l'échéance est
  recalculée à partir de la réalisation précédente, ou du point de départ saisi à la création
  de la règle.
- Véhicule archivé : ses échéances ne sont plus signalées ; elles reviennent à la réactivation.
- Véhicule peu roulé : une règle « 15 000 km ou 12 mois » arrive à échéance par la date même si
  le kilométrage est loin d'être atteint.
- Aucun relevé récent : l'état d'une échéance au kilométrage se fonde sur le dernier
  kilométrage connu, dont la date est affichée pour que le membre sache s'il doit en saisir un ;
  au-delà de 30 jours sans relevé, l'accueil invite à en saisir un (FR-035).
- Photo très lourde prise par un téléphone récent : au-delà de 10 Mo, refus avec un message
  clair (pas de troncature silencieuse).
- Envoi d'un justificatif interrompu : rien n'est rattaché à l'entretien et le membre peut
  réessayer.
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
  une ou plusieurs opérations (obligatoire), coût total TTC en dirhams (facultatif, positif ou
  nul, 2 décimales au plus), garage ou intervenant (facultatif, texte libre, « Moi-même »
  possible) et remarques (facultatives).
- **FR-011**: Les opérations d'un entretien DOIVENT être choisies parmi les types actifs de
  catégorie Entretien du catalogue (FR-036). Un type marqué « description obligatoire » (par
  exemple « Autre ») DOIT être accompagné d'une description.
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
- **FR-020**: Les montants DOIVENT être affichés en dirhams (`1 234,50 DH`) et les
  kilométrages avec séparateur de milliers (`52 000 km`) ; les messages d'erreur sont en
  français et indiquent quoi faire.

**Échéances**

- **FR-021**: Tout membre ayant accès à un véhicule DOIT pouvoir définir, modifier et supprimer
  les règles d'échéance de ce véhicule. Une règle porte sur un type de catégorie Entretien et fixe
  un intervalle en kilomètres, en mois, ou les deux (« au premier des deux termes atteint »).
- **FR-022**: À la création d'une règle, le système DOIT proposer la fréquence recommandée du
  type dans le catalogue (par exemple Vidange : 10 000 km ou 12 mois), modifiable par le membre.
  Changer ensuite la fréquence dans le catalogue NE DOIT PAS modifier les règles existantes.
- **FR-023**: La prochaine échéance d'une règle DOIT être calculée à partir de la réalisation la
  plus récente de l'opération (le dernier entretien qui la comporte, sinon le point de départ
  saisi à la création de la règle) : date de réalisation + intervalle en mois, et kilométrage de
  réalisation + intervalle en kilomètres.
- **FR-024**: Chaque échéance DOIT avoir un état : « En retard » (date dépassée ou kilométrage
  actuel atteint), « Bientôt » (moins de 30 jours ou moins de 1 000 km restants) ou « À jour » ;
  avec deux termes, c'est le plus défavorable qui l'emporte.
- **FR-025**: Un membre DOIT pouvoir créer des échéances à date fixe, sans kilométrage, soit à
  partir d'un type de catégorie Administratif (assurance, vignette, visite technique : libellé
  et récurrence pré-remplis depuis le catalogue), soit avec un libellé libre ; avec une date et
  une récurrence facultative en mois ou en années. Marquer une telle échéance comme faite crée
  la suivante si elle est récurrente.
- **FR-026**: Les échéances DOIVENT être recalculées immédiatement après toute saisie,
  modification ou suppression de relevé, d'entretien ou de règle.
- **FR-027**: La fiche d'un véhicule DOIT lister ses échéances, en retard d'abord puis par
  proximité, avec ce qui reste (jours et/ou kilomètres) ou le retard.
- **FR-028**: L'accueil DOIT afficher, pour l'ensemble des véhicules actifs auxquels le membre a
  accès, les échéances « En retard » et « Bientôt », avec un indicateur visible dès l'ouverture
  de l'application ; les véhicules archivés n'y figurent pas.
- **FR-035**: L'accueil DOIT afficher, pour chaque véhicule actif auquel le membre a accès et
  dont le dernier kilométrage connu (relevé, entretien, ou à défaut kilométrage de départ) date
  de plus de 30 jours, un rappel invitant à relever le kilométrage, avec la date et la valeur du
  dernier kilométrage connu et un accès direct à la saisie d'un relevé. Ce rappel DOIT
  disparaître dès qu'un kilométrage plus récent est enregistré.

**Justificatifs**

- **FR-029**: Un membre ayant accès à un véhicule DOIT pouvoir joindre un ou plusieurs
  justificatifs à un entretien, à partir d'un fichier ou directement de l'appareil photo du
  téléphone.
- **FR-030**: Les formats acceptés DOIVENT être PDF, JPEG, PNG et WebP, avec une taille maximale
  de 10 Mo par fichier et 10 justificatifs par entretien ; tout autre fichier DOIT être refusé
  avec un message indiquant ces limites.
- **FR-031**: Un membre ayant accès au véhicule DOIT pouvoir consulter (aperçu pour les images,
  ouverture pour les PDF) et télécharger les justificatifs, sous leur nom d'origine.
- **FR-032**: L'accès au contenu d'un justificatif DOIT obéir aux mêmes règles que le véhicule
  (FR-002) et être contrôlé à chaque demande ; aucun justificatif NE DOIT être accessible par
  une adresse publique ou devinable sans session autorisée.
- **FR-033**: Seul l'auteur d'un justificatif ou un ADMIN DOIT pouvoir le supprimer, après
  confirmation ; la suppression d'un entretien DOIT supprimer ses justificatifs, après une
  confirmation qui en indique le nombre.
- **FR-034**: Les justificatifs DOIVENT être conservés durablement, indépendamment des
  redémarrages ou mises à jour de l'application.

**Catalogue des types d'entretien**

- **FR-036**: Le système DOIT gérer un catalogue de types d'entretien : nom (obligatoire, unique
  sans tenir compte de la casse, 60 caractères au plus), catégorie (Entretien ou Administratif),
  fréquence recommandée en kilomètres (Entretien seulement) et/ou en mois (facultatives),
  « description obligatoire » (Entretien seulement), âge du véhicule en années à partir duquel
  l'échéance est suggérée (Administratif seulement, facultatif), ordre d'affichage, et statut
  actif ou désactivé.
- **FR-037**: Un ADMIN DOIT pouvoir lister (actifs et désactivés), ajouter, modifier (y compris
  renommer) et désactiver ou réactiver un type ; la catégorie d'un type ne change plus une fois
  créé ; aucun type n'est supprimé. Toute modification du catalogue par un USER DOIT être
  refusée (403) côté serveur.
- **FR-038**: Tout membre DOIT pouvoir consulter les types actifs ; un type désactivé NE DOIT
  plus être proposé pour une nouvelle saisie mais DOIT rester affiché, sous son nom actuel, sur
  les entretiens, règles et échéances qui l'utilisent.
- **FR-039**: Le catalogue DOIT être pré-rempli à l'installation avec des valeurs adaptées au
  Maroc (voir Assumptions), de sorte que les histoires 2 à 4 fonctionnent sans configuration.
- **FR-040**: La fiche d'un véhicule actif DOIT suggérer, pour chaque type Administratif actif
  dont l'âge de suggestion est atteint (âge = année en cours − année de mise en circulation ;
  un âge de suggestion de 0 vaut pour tout véhicule) et pour lequel le véhicule n'a aucune
  échéance à faire de ce type, la création de l'échéance pré-remplie ; la suggestion peut être
  ignorée pour ce véhicule. Un véhicule sans année de mise en circulation ne reçoit que les
  suggestions d'âge 0.

### Key Entities

- **Relevé kilométrique** : un kilométrage constaté sur un véhicule à une date. Véhicule, date,
  kilométrage, remarque, auteur, dates de création et de modification.
- **Entretien** : une intervention réalisée sur un véhicule. Véhicule, date, kilométrage,
  opérations (une ou plusieurs), description (obligatoire pour « Autre »), coût total,
  garage ou intervenant, remarques, auteur, dates de création et de modification. Son
  kilométrage participe au calcul du kilométrage actuel au même titre qu'un relevé.
- **Type d'entretien** : un élément du catalogue géré par l'ADMIN. Nom, catégorie (Entretien ou
  Administratif), fréquence recommandée en kilomètres et/ou en mois, description obligatoire,
  âge de suggestion, ordre, statut actif. Les types Entretien sont les « opérations » d'un
  entretien et le point d'accroche des règles ; les types Administratif servent de modèles aux
  échéances à date fixe.
- **Règle d'échéance** : le rythme d'une opération sur un véhicule. Véhicule, opération,
  intervalle en kilomètres et/ou en mois, point de départ (date et kilométrage de la dernière
  réalisation connue avant tout entretien enregistré), auteur.
- **Échéance à date fixe** : une obligation datée sans kilométrage. Véhicule, type Administratif
  facultatif, libellé, date, récurrence facultative, état (à faire ou faite).
- **Échéance calculée** (non saisie) : pour chaque règle, la prochaine date et/ou le prochain
  kilométrage, l'état (En retard, Bientôt, À jour) et ce qui reste.
- **Justificatif** : un fichier rattaché à un entretien. Entretien, nom d'origine, format,
  taille, auteur, date d'ajout.
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

- **SC-007**: Pour 100 % des règles, la prochaine échéance affichée correspond au calcul
  « dernière réalisation + intervalle », vérifié sur des cas par date seule, kilométrage seul et
  les deux combinés.
- **SC-008**: Une échéance en retard ou proche est visible sur l'accueil sans aucune
  navigation, dès l'ouverture de l'application.
- **SC-009**: Un membre photographie et joint une facture à un entretien en moins de
  45 secondes depuis un téléphone.
- **SC-010**: 100 % des demandes de justificatif d'un véhicule non autorisé sont refusées, y
  compris avec l'adresse directe du fichier.
- **SC-011**: 100 % des véhicules actifs sans kilométrage depuis plus de 30 jours affichent le
  rappel de relevé sur l'accueil des membres qui y ont accès, et aucun véhicule relevé dans les
  30 derniers jours ne l'affiche.
- **SC-012**: Un ADMIN ajoute un type d'entretien ou change une fréquence recommandée en moins
  de 1 minute, et le changement est proposé dès la saisie suivante d'un membre, sans
  redémarrage ni intervention technique.
- **SC-013**: 100 % des véhicules actifs de 5 ans ou plus sans visite technique à faire
  affichent la suggestion correspondante, et aucun véhicule plus jeune ne l'affiche.

## Assumptions

- **Périmètre** : choisi par le mainteneur le 2026-11-25 : relevés kilométriques, historique
  des entretiens, échéances et rappels, justificatifs. Les histoires sont livrables dans
  l'ordre de leur priorité.
- Les rappels sont affichés dans l'application (accueil, fiche véhicule) ; aucun email ni
  notification push n'est envoyé dans cette fonctionnalité. Un rappel par email pourra être
  ajouté ensuite s'il manque à l'usage.
- Le kilométrage n'est connu que par les saisies des membres (aucun GPS ni boîtier connecté) ;
  le rappel de relevé à 30 jours (ajouté à la demande du mainteneur le 2026-11-25) limite le
  risque d'un kilométrage périmé.
- L'état « Bientôt » se fonde sur le dernier kilométrage connu ; il n'y a pas d'estimation du
  kilométrage futur à partir du rythme de roulage.
- Catalogue pré-rempli (valeurs indicatives pour le Maroc, modifiables par l'ADMIN) :
  - Entretien : Vidange (10 000 km ou 12 mois), Filtre à huile (10 000 km ou 12 mois), Filtre à
    air (20 000 km ou 24 mois), Filtre d'habitacle (15 000 km ou 12 mois), Filtre à carburant
    (40 000 km ou 24 mois), Révision (20 000 km ou 12 mois), Pneus (40 000 km), Freins
    (30 000 km), Distribution (80 000 km ou 60 mois), Batterie (48 mois), Climatisation
    (24 mois), Réparation (sans fréquence), Autre (sans fréquence, description obligatoire) ;
  - Administratif : Assurance (12 mois, suggérée pour tout véhicule), Vignette (12 mois,
    suggérée pour tout véhicule), Visite technique (12 mois, suggérée à partir de 5 ans).
- La règle « visite technique annuelle à partir de 5 ans » reflète la pratique marocaine
  indiquée par le mainteneur ; elle n'est qu'une suggestion paramétrable, l'application ne
  vérifie aucune réglementation.
- L'assurance, la vignette et la visite technique ne sont pas des entretiens (pas de
  kilométrage) : elles se suivent comme des échéances à date fixe.
- Le catalogue est commun à tous les véhicules ; il n'y a pas de fréquence recommandée par
  modèle de véhicule (les intervalles restent ajustables règle par règle).
- Les justificatifs se rattachent aux entretiens ; les documents propres au véhicule (carte
  grise, attestation d'assurance) ne sont pas gérés dans cette fonctionnalité.
- Pas de lecture automatique du contenu des factures (montant, garage) : le membre saisit ces
  informations lui-même.
- Un entretien a un seul coût total ; la ventilation du coût par opération n'est pas
  nécessaire pour un usage familial.
- Le garage est un texte libre ; il n'y a pas de carnet d'adresses de garages.
- Les montants sont en dirhams marocains (MAD, affichés « DH ») uniquement ; la date du jour
  est celle du Maroc (fuseau `Africa/Casablanca`, qui suit le passage à UTC+0 pendant le
  ramadan).
- Au Maroc la vignette se paie en janvier : le membre fixe la date de l'échéance « Vignette »
  (par exemple le 31 janvier) en acceptant la suggestion ; aucune date n'est imposée.
- La consommation de carburant (pleins) n'est pas suivie.
- Pas d'export (CSV, PDF) ni de graphique dans cette fonctionnalité.
- Volumétrie conforme à la constitution : quelques centaines de saisies par véhicule au plus ;
  ni pagination ni recherche avancée ne sont nécessaires.
- Les règles d'accès aux véhicules (attributions, archivage) sont celles de la fonctionnalité
  001 et ne changent pas.
