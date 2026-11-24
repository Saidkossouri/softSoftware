---
name: charte-interface
description: Charte d'interface de carSupervision (couleurs, composants shadcn/ui, mise en page mobile-first, textes en français, états de chargement et d'erreur). À utiliser avant de créer ou de modifier un écran, un composant ou un style dans frontend/.
---

# Charte d'interface carSupervision

Application familiale de suivi d'entretien des véhicules. L'interface doit être **simple, calme
et rapide** : on la remplit au garage ou à la station, souvent sur téléphone, d'une main.
Version 1 de la charte, déduite des écrans existants ; à enrichir après validation des
maquettes des écrans membres et véhicules.

## Principes

1. **Mobile d'abord.** Concevoir à 360 px de large, puis élargir. Rien ne doit déborder
   horizontalement à 360 px (`min-w-0`, `truncate`, `flex-wrap`).
2. **Une action principale par écran**, en bouton `default`. Les autres actions en `outline`
   ou `ghost`. Les actions destructives en `destructive`, toujours avec confirmation.
3. **Zones tactiles d'au moins 40 px** de haut (tailles de bouton `default` ou `lg` sur
   mobile ; `sm` réservé aux actions secondaires de l'en-tête ou des listes denses).
4. **Sobriété.** Pas de dégradés, d'ombres fortes ni d'animations décoratives. La hiérarchie
   vient de la typographie, de l'espacement et des cartes.
5. **Le serveur décide des droits.** Masquer une action interdite côté interface est un confort,
   jamais une sécurité (constitution, principe IV).

## Couleurs et thème

- N'utiliser **que les jetons** définis dans `frontend/src/index.css` via les classes Tailwind :
  `bg-background`, `bg-card`, `text-foreground`, `text-muted-foreground`, `bg-primary`,
  `text-primary`, `bg-secondary`, `border`, `text-destructive`, `ring`…
- **Jamais de couleur en dur** (`#hex`, `rgb()`, `text-blue-600`…) : le thème clair/sombre suit
  le système (`prefers-color-scheme`) et doit rester lisible dans les deux.
- Couleur principale : bleu (`primary`). Rouge (`destructive`) réservé aux erreurs et aux
  suppressions. Pour un statut d'échéance (à jour, bientôt, dépassé), ajouter des jetons dédiés
  dans `index.css` plutôt que des couleurs ponctuelles.
- Arrondis : `rounded-md` pour les contrôles, `rounded-xl` pour les cartes (via `--radius`).

## Mise en page

- Conteneur de page : `mx-auto max-w-3xl px-4 py-6` (déjà fourni par `AppLayout`).
- Écrans hors connexion (connexion, accès refusé, erreur) : `CenteredCard`
  (`frontend/src/auth/RequireAuth.tsx`), carte `max-w-sm` centrée.
- Contenu groupé dans des `Card` (`CardHeader` / `CardTitle` / `CardDescription` /
  `CardContent`). Espacement vertical entre blocs : `gap-4` ou `space-y-4`.
- Listes (membres, véhicules) : une carte ou une ligne par élément, information principale en
  `font-medium`, secondaire en `text-sm text-muted-foreground`. Sur desktop, un tableau est
  possible, mais la version mobile reste une liste.
- Formulaires : un champ par ligne sur mobile, libellé au-dessus du champ, message d'erreur
  sous le champ en `text-sm text-destructive`, bouton de validation en bas sur toute la largeur
  sur mobile (`w-full sm:w-auto`).

## Composants

- Composants shadcn/ui (style `new-york`, base `slate`) dans `frontend/src/components/ui/` :
  `button`, `card`, `badge`, `alert`. Les réutiliser avant d'en créer.
- Nouveau composant de base : l'ajouter dans `components/ui/` au format shadcn
  (`cva` + `cn`), sans dépendance Radix si une version simple suffit (comme `button.tsx`).
- Composants métier dans `frontend/src/components/` ou à côté de la page qui les utilise.
- Lien qui ressemble à un bouton : `buttonVariants(...)` sur un `<a>` (voir `LoginPage`).
- Rôles affichés avec `Badge` et le libellé de `fr.roles`.

## Textes

- Interface **uniquement en français**. Tout libellé passe par `frontend/src/i18n/fr.ts`,
  jamais en dur dans un composant.
- Ton : vouvoiement, phrases courtes, verbes d'action sur les boutons (« Ajouter un véhicule »,
  « Enregistrer »), pas de jargon technique ni de code d'erreur brut.
- Messages d'erreur : dire ce qui s'est passé et quoi faire (« Cet email est déjà dans la
  liste. »). Les erreurs de l'API (Problem Details) sont traduites en message lisible.

## États à toujours prévoir

Pour chaque écran qui charge des données :

1. **Chargement** : texte `fr.loading` en `text-muted-foreground` (ou squelette sobre).
2. **Vide** : message explicite et, si l'utilisateur peut agir, l'action principale
   (ex. « Aucun véhicule attribué. »).
3. **Erreur** : `Alert` avec message lisible et bouton « Réessayer ».
4. **Succès d'une action** : retour visible (liste mise à jour, message court).

## Accessibilité

- Éléments natifs (`button`, `a`, `label` + `input`) plutôt que des `div` cliquables.
- Focus visible conservé (`focus-visible:ring-2 focus-visible:ring-ring`).
- Contraste suffisant : texte secondaire en `text-muted-foreground`, jamais plus pâle.
- `lang="fr"` est déjà posé dans `index.html`.

## Vérification avant de terminer

- Rendu contrôlé à 360 px et sur desktop, en thème clair et sombre.
- Aucun libellé en dur, aucune couleur en dur.
- États chargement / vide / erreur présents.
- `npm run lint` et `npm test -- --run` passent dans `frontend/`.
