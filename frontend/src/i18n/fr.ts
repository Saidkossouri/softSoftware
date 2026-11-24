import type { Role } from '@/api/client'

/** Libellés de l'interface, en français uniquement (spec : hypothèses). */
export const fr = {
  appName: 'carSupervision',
  login: {
    title: 'Bienvenue sur carSupervision',
    subtitle: "Le suivi d'entretien des véhicules de la famille.",
    button: 'Se connecter avec Google',
    help: "Seuls les membres ajoutés par un administrateur peuvent se connecter.",
  },
  logout: 'Se déconnecter',
  home: {
    title: 'Accueil',
    body: 'Vous êtes connecté. La gestion du parc de véhicules arrive dans les prochaines étapes.',
  },
  nav: {
    home: 'Accueil',
    members: 'Membres',
  },
  forbidden: {
    title: 'Accès refusé',
    body: "Cette page est réservée aux administrateurs. Demandez à un administrateur si vous pensez qu'il s'agit d'une erreur.",
    back: "Retour à l'accueil",
  },
  members: {
    title: 'Membres de la famille',
    description: 'Les personnes autorisées à se connecter avec leur compte Google.',
    empty: 'Aucun membre pour le moment.',
    neverLoggedIn: 'Jamais connecté',
    lastLogin: (date: string) => `Dernière connexion le ${date}`,
    addTitle: 'Ajouter un membre',
    addDescription: "Saisissez l'email Google du membre. Il pourra se connecter dès son ajout.",
    email: 'Email Google',
    emailPlaceholder: 'prenom.nom@gmail.com',
    displayName: 'Nom affiché (facultatif)',
    role: 'Rôle',
    add: 'Ajouter le membre',
    adding: 'Ajout…',
    added: (email: string) => `${email} a été ajouté.`,
    edit: 'Modifier',
    back: 'Retour aux membres',
    editTitle: 'Modifier le membre',
    save: 'Enregistrer',
    saving: 'Enregistrement…',
    saved: 'Modifications enregistrées.',
    remove: "Retirer l'accès",
    removeConfirm: (email: string) =>
      `Retirer l'accès de ${email} ? Sa prochaine action sera refusée, même s'il est connecté.`,
    removeConfirmButton: 'Confirmer le retrait',
    cancel: 'Annuler',
    removing: 'Retrait…',
    you: 'vous',
  },
  validation: {
    required: 'Ce champ est obligatoire.',
    email: 'Saisissez une adresse email valide.',
    maxLength: (max: number) => `${max} caractères maximum.`,
  },
  revoked: {
    title: 'Accès refusé',
    body: "Votre accès à carSupervision a été retiré. Demandez à un administrateur de vous ajouter de nouveau.",
  },
  loading: 'Chargement…',
  unexpectedError: 'Une erreur inattendue est survenue. Réessayez plus tard.',
  retry: 'Réessayer',
  roles: {
    ADMIN: 'Administrateur',
    USER: 'Membre',
  } satisfies Record<Role, string>,
  roleHelp: {
    ADMIN: 'Gère les membres, le parc et voit tous les véhicules.',
    USER: 'Voit et renseigne uniquement les véhicules qui lui sont attribués.',
  } satisfies Record<Role, string>,
  energyTypes: {
    PETROL: 'Essence',
    DIESEL: 'Diesel',
    HYBRID: 'Hybride',
    ELECTRIC: 'Électrique',
    LPG: 'GPL',
    OTHER: 'Autre',
  },
} as const
