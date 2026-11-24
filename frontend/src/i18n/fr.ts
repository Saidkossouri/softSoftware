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
    body: 'Vous êtes connecté. La gestion des membres et du parc de véhicules arrive dans les prochaines étapes.',
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
  energyTypes: {
    PETROL: 'Essence',
    DIESEL: 'Diesel',
    HYBRID: 'Hybride',
    ELECTRIC: 'Électrique',
    LPG: 'GPL',
    OTHER: 'Autre',
  },
} as const
