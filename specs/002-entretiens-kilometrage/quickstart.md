# Quickstart : valider la fonctionnalité 002 de bout en bout

**Feature**: `002-entretiens-kilometrage` | **Plan**: [plan.md](./plan.md)

Guide de validation une fois la fonctionnalité implémentée. Les endpoints sont dans le contrat
[`backend/api-contract/src/main/resources/openapi/openapi.yaml`](../../backend/api-contract/src/main/resources/openapi/openapi.yaml)
(version de conception : [contracts/openapi.yaml](./contracts/openapi.yaml)), les règles dans
[data-model.md](./data-model.md) et [research.md](./research.md).

## Prérequis

- L'environnement de la fonctionnalité 001 ([quickstart 001](../001-acces-membres-flotte/quickstart.md)) :
  `.env` renseigné, deux comptes Google (un ADMIN, un USER).
- Nouvelle variable facultative dans `.env` : `DOCUMENTS_DIR`, dossier des justificatifs
  (par défaut `./data/documents` en natif ; en Docker, le volume `documents-data`).
- Un téléphone sur le même réseau, ou l'émulation mobile du navigateur à 360 px de large.
- Un PDF et une photo JPEG de moins de 10 Mo ; un fichier de plus de 10 Mo ; un fichier texte
  renommé en `.pdf`.

## 1. Lancer l'application

```bash
docker compose up -d postgres
./gradlew bootRun
cd frontend && npm install && npm run dev
```

ou tout en Docker : `docker compose up -d --build` (le backend monte le volume
`documents-data`).

## 2. Tests automatisés

```bash
./gradlew test                    # dont les IT Testcontainers de R16 (RBAC, kilométrage, échéances, justificatifs)
cd frontend && npm test -- --run && npm run lint
```

## 3. Scénarios manuels

Préparation : en ADMIN, créer le véhicule A (kilométrage de départ 50 000 km, mis en
circulation il y a 6 ans) et le véhicule B (mis en circulation il y a 2 ans) ; attribuer A
seulement au USER.

| # | Étapes | Résultat attendu | Spec |
|---|---|---|---|
| 1 | USER, sur téléphone : « Mes véhicules » → A → « Relever le kilométrage », saisir 51 200 | A affiche 51 200 km et la date du jour, en moins de 20 s | US1-1, SC-001 |
| 2 | Saisir 50 900 km daté d'aujourd'hui | Refus citant le relevé de 51 200 km | US1-2, SC-003 |
| 3 | Saisir 251 300 km | Demande de confirmation (écart > 100 000 km) ; annuler | FR-007 |
| 4 | Enregistrer un entretien : Vidange + Filtre à huile, 52 000 km, 450,00 DH, « Garage du Centre » | En tête de l'historique, kilométrage actuel 52 000 km, en moins de 1 min 30 | US2-1, US2-2, SC-002 |
| 5 | Ajouter un entretien daté de l'an dernier à 48 000 km (vieux carnet), 1 200 DH | Rangé à sa place ; kilométrage actuel inchangé ; totaux : 450 DH cette année, 1 650 DH depuis le début | US2-6, US3-2 |
| 6 | Filtrer l'historique sur « Vidange » | Seuls les entretiens avec vidange, en moins de 10 s | US3-3, SC-005 |
| 7 | ADMIN : modifier le kilométrage de départ de A à 49 000 km | Refusé (plus grand que 48 000 km déjà saisi) | FR-009 |
| 8 | USER : créer la règle « Vidange » (défaut proposé par le catalogue : 10 000 km ou 12 mois) | Prochaine vidange à 62 000 km ou dans 12 mois, état « À jour » | US4-1, FR-022 |
| 9 | Relever 61 300 km | La vidange passe « Bientôt » (700 km) sur la fiche et sur l'accueil | US4-2, SC-008 |
| 10 | Relever 62 400 km | « En retard » de 400 km, en tête | US4-3 |
| 11 | Enregistrer une vidange à 62 500 km | L'échéance repart : 72 500 km ou dans 12 mois | US4-4 |
| 12 | Sur A, accepter la suggestion « Assurance » (récurrence 12 mois pré-remplie), date au 1er avril prochain, puis « Marquer comme faite » | Une nouvelle échéance au 1er avril de l'année suivante | US4-6 |
| 13 | Sur l'entretien de l'étape 4, joindre une photo prise avec le téléphone, puis le PDF | Vignette, nom, taille, date ; en moins de 45 s pour la photo | US5-1, SC-009 |
| 14 | Joindre le fichier de plus de 10 Mo, puis le texte renommé en `.pdf` | Deux refus clairs (taille ; format) | US5-3 |
| 15 | ADMIN : ouvrir et télécharger le PDF joint par le USER | Aperçu et téléchargement sous le nom d'origine | US5-2 |
| 16 | Copier l'adresse du contenu du PDF ; l'ouvrir avec un compte autorisé seulement sur B (ou après avoir retiré A au USER) | 403, aucun contenu | US5-4, SC-010 |
| 17 | Supprimer l'entretien de l'étape 4 | Confirmation annonçant 2 justificatifs ; fichiers absents de `DOCUMENTS_DIR` ensuite | US5-6 |
| 18 | USER : tenter de modifier un entretien saisi par l'ADMIN | Pas de bouton ; l'appel direct répond 403 | US2-5 |
| 19 | ADMIN : archiver A | A n'apparaît plus chez le USER, ni ses échéances sur l'accueil ; l'ADMIN lit l'historique mais ne peut rien saisir | FR-004, FR-018 |
| 20 | Un véhicule actif dont le dernier kilométrage date de plus de 30 jours (couvert par un IT à horloge fixée ; à la main, attendre 30 jours sans relevé) | Rappel « Pense à relever le kilométrage » sur l'accueil, qui disparaît après un relevé | US4-8, FR-035 |
| 21 | Fiche de A (6 ans) puis de B (2 ans) | A : suggestions Assurance, Vignette, Visite technique ; B : Assurance et Vignette seulement ; « Ignorer » fait disparaître une suggestion pour ce véhicule | US4-9, FR-040, SC-013 |
| 22 | ADMIN : Administration → Types d'entretien, passer « Vidange » à 8 000 km | Une nouvelle règle « Vidange » propose 8 000 km ; la règle de l'étape 8 garde 10 000 km ; en moins de 1 min | US6-2, SC-012 |
| 23 | ADMIN : ajouter « Courroie d'accessoires » (Entretien, 60 000 km), puis tenter « vidange » | Le nouveau type est proposé dans le formulaire d'entretien ; le doublon est refusé | US6-3, US6-6 |
| 24 | ADMIN : renommer « Vidange » en « Vidange moteur », puis désactiver « Climatisation » | Le nouveau nom apparaît dans l'historique ; « Climatisation » n'est plus proposée mais reste affichée sur les saisies existantes | US6-4, US6-5 |
| 25 | USER : appeler directement la modification d'un type | 403 | US6-7 |

## 4. Sauvegarde

Sauvegarder ensemble la base PostgreSQL et le dossier des justificatifs (`DOCUMENTS_DIR`, ou
le volume Docker `documents-data`) : l'un sans l'autre laisse des justificatifs sans fichier ou
des fichiers sans justificatif.
