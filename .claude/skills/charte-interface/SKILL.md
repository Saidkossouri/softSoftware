---
name: charte-interface
description: Charte d'interface de carSupervision (piste « Lumineux » : couleurs, typographie, composants shadcn/ui, mise en page mobile-first, textes en français, états vides, toasts et dialogues). À utiliser avant de créer ou de modifier un écran, un composant ou un style dans frontend/.
---

# Charte d'interface carSupervision (v2 · piste « Lumineux »)

Application familiale de suivi d'entretien des véhicules. On la remplit au garage ou à la
station, souvent sur téléphone, d'une main. L'interface doit être **lumineuse, chaleureuse et
rapide** : des cartes blanches qui flottent sur un fond gris-bleu, un bleu vif pour l'action,
des titres affirmés et des statuts colorés lisibles d'un coup d'œil.

Version 2, validée par Said le 25/11/2026 sur maquettes (piste A « Lumineux » parmi trois
pistes). Elle remplace la v1 « sobre ».

## Principes

1. **Mobile d'abord.** Concevoir à 360 px de large, puis élargir. Rien ne déborde
   horizontalement à 360 px (`min-w-0`, `truncate`, `flex-wrap`).
2. **Une action principale par écran**, en bouton `default` (bleu plein, ombre colorée). Les
   autres en `outline`, `secondary` ou `ghost`. Destructif en `destructive`, toujours confirmé
   par un `ConfirmDialog`.
3. **Zones tactiles d'au moins 44 px** (tailles `default` et `lg` ; `sm` = 36 px réservé aux
   actions secondaires des listes denses, `icon` = 44 × 44).
4. **Relief doux, jamais criard.** Ombres douces (`shadow-card`, `shadow-primary`), grands
   arrondis, transitions courtes (150 ms) sur survol et focus. Pas de dégradés décoratifs, pas
   d'animations en boucle ; respecter `prefers-reduced-motion`.
5. **Le serveur décide des droits.** Masquer une action interdite est un confort, jamais une
   sécurité (constitution, principe IV).

## Couleurs et thème

- **Uniquement les jetons** de `frontend/src/index.css` via les classes Tailwind. **Jamais de
  couleur en dur** (`#hex`, `rgb()`, `text-blue-600`…).
- Thème clair et sombre suivant le système (`prefers-color-scheme`) ; les deux sont soignés et
  doivent rester lisibles (contraste texte ≥ 4,5:1).

| Jeton | Usage |
|---|---|
| `background` | fond de page gris-bleu très clair (bleu nuit en sombre) |
| `card` / `card-foreground` | surfaces : cartes, en-tête, barre de navigation |
| `foreground` | texte principal |
| `muted-foreground` | texte secondaire (jamais plus pâle) |
| `primary` / `primary-foreground` | bleu vif : action principale, élément actif, carte « héros » |
| `accent` / `accent-foreground` | bleu très pâle : tuiles d'icône, élément de navigation actif |
| `secondary` / `muted` | fonds neutres : boutons secondaires, onglets inactifs |
| `success`, `warning`, `destructive` (+ `-soft`) | statuts : à jour, bientôt, en retard / erreur. Pastille = fond `*-soft` + texte de la couleur |
| `border`, `input`, `ring` | bordures, champs, focus |

- Rouge (`destructive`) réservé aux erreurs, retards et suppressions.
- Ombres : `shadow-card` (cartes, listes), `shadow-primary` (bouton principal, carte héros).
- Arrondis : `rounded-xl` pour les contrôles (boutons, champs), `rounded-2xl` pour les cartes,
  `rounded-full` pour pastilles et avatars.

## Typographie

- Texte : **Inter** (`font-sans`, par défaut). Titres et chiffres marquants : **Space Grotesk**
  (`font-display`). Polices embarquées via `@fontsource-variable` (aucun appel externe).
- Titre de page : `PageHeader` (`text-2xl font-bold font-display`). Titre de carte :
  `CardTitle` (`font-display font-semibold`). Petites étiquettes de section : `SectionLabel`
  (majuscules, `text-xs`, `tracking-wider`, `text-muted-foreground`).
- Immatriculation : composant `Plate` (style plaque française, bandeau bleu « F »).
- Kilométrages et nombres : `tabular-nums`, séparateur de milliers français
  (`toLocaleString('fr-FR')`).

## Mise en page

- `AppLayout` : en-tête blanc (marque + membre connecté + déconnexion) ; navigation **en bas
  de l'écran sur mobile** (barre fixe avec icônes), **dans l'en-tête à partir de `sm`**. Un seul
  élément `<nav>` dans le DOM.
- Conteneur de page : `mx-auto max-w-3xl px-4 py-6` (fourni par `AppLayout`, avec la marge
  basse pour la barre de navigation mobile).
- Chaque page commence par un `PageHeader` (titre, description, action principale à droite
  sur desktop, pleine largeur sur mobile) ou par un lien retour `BackLink` puis le titre.
- Écrans hors connexion (connexion, accès retiré, erreur) : `CenteredCard`
  (`frontend/src/auth/RequireAuth.tsx`) avec le logo `BrandMark`.
- Contenu groupé dans des `Card` sans bordure marquée, ombre `shadow-card`. Espacement entre
  blocs : `gap-4` ; dans une carte : `gap-4` entre champs.
- **Listes** (membres, véhicules) : une carte par élément (`ListCard`) avec, à gauche, une tuile
  d'icône (`IconTile`) ou un avatar, au centre l'information principale en `font-semibold` et
  la secondaire en `text-sm text-muted-foreground`, à droite les actions ou le statut.
- **Onglets** : contrôle segmenté (`SegmentedTabs`), fond `muted`, onglet actif blanc avec
  ombre.
- **Formulaires** : champs regroupés par sections dans des cartes, avec `SectionLabel`. Un
  champ par ligne sur mobile, deux colonnes possibles à partir de `sm`. Libellé au-dessus,
  erreur sous le champ en `text-sm text-destructive`, bouton de validation en bas, pleine
  largeur sur mobile (`w-full sm:w-auto`).

## Composants

Dans `frontend/src/components/ui/` (format shadcn/ui, `cva` + `cn`, sans Radix quand une
version simple suffit) : `button`, `card`, `badge` (variantes `success`, `warning`,
`destructive`, `primary`), `alert`, `input`, `select`, `label`, `dialog` (`ConfirmDialog`),
`toast` (`Toaster` + `useToast`), `empty-state`, `segmented-tabs`.
Composants métier dans `frontend/src/components/` : `PageHeader`, `BackLink`, `ListCard`,
`IconTile`, `Plate`, `Avatar`, `BrandMark`, `Illustration`.

- Icônes : **lucide-react** uniquement, `aria-hidden` quand le texte suffit, `aria-label` sur
  un bouton qui n'a qu'une icône. Taille 20 px (`size-5`) dans les listes, 24 px dans la
  navigation.
- Lien qui ressemble à un bouton : `buttonVariants(...)` sur un `<Link>` ou un `<a>`.
- Rôles affichés avec `Badge` et le libellé de `fr.roles`.

## Retours à l'utilisateur

- **Succès d'une action** (enregistrement, ajout, réactivation) : **toast** via `useToast()`,
  en bas de l'écran au-dessus de la navigation, fermé automatiquement après 4 s. La liste se
  met à jour en même temps.
- **Erreur d'une action** : `Alert variant="destructive"` près de l'action, ou sous le champ
  concerné ; jamais seulement un toast.
- **Action destructive** (retirer un accès, archiver) : `ConfirmDialog` avec le texte
  d'explication, bouton `destructive` et « Annuler ».

## États à toujours prévoir

1. **Chargement** : squelettes (`Skeleton`) de la forme du contenu, ou `fr.loading` en
   `text-muted-foreground` pour un petit bloc.
2. **Vide** : `EmptyState` avec illustration, titre court, explication et, si l'utilisateur
   peut agir et que l'action principale n'est pas déjà visible dans l'en-tête, cette action.
3. **Erreur** : `Alert variant="destructive"` avec message lisible et bouton « Réessayer ».
4. **Succès** : toast (voir ci-dessus).

## Textes

- Interface **uniquement en français**. Tout libellé passe par `frontend/src/i18n/fr.ts`,
  jamais en dur dans un composant (y compris `aria-label`).
- Ton : vouvoiement, phrases courtes, verbes d'action sur les boutons (« Ajouter un véhicule »,
  « Enregistrer »), pas de jargon ni de code d'erreur brut.
- Messages d'erreur : ce qui s'est passé et quoi faire. Les erreurs de l'API (Problem Details)
  sont affichées avec leur `detail`, déjà en français.

## Accessibilité

- Éléments natifs (`button`, `a`, `label` + `input`, `select`) plutôt que des `div` cliquables.
- Focus visible conservé (`focus-visible:ring-[3px] focus-visible:ring-ring/50`).
- Onglets : `role="tablist"` / `role="tab"` / `aria-selected`. Dialogue : `role="dialog"`,
  `aria-modal`, titre relié, fermeture par Échap, focus placé dans le dialogue.
- Toasts dans une région `role="status"` `aria-live="polite"`.
- `lang="fr"` est posé dans `index.html`.

## Vérification avant de terminer

- Rendu contrôlé à 360 px et sur desktop, en thème clair et sombre.
- Aucun libellé en dur, aucune couleur en dur.
- États chargement / vide / erreur / succès présents.
- `npm run lint` et `npm test -- --run` passent dans `frontend/`.
