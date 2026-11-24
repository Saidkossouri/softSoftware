import type { Member } from '@/api/client'
import { fr } from '@/i18n/fr'

const dateFormat = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'short', timeStyle: 'short' })

export function lastLoginLabel(member: Member): string {
  return member.lastLoginAt
    ? fr.members.lastLogin(dateFormat.format(new Date(member.lastLoginAt)))
    : fr.members.neverLoggedIn
}

/** Validation de confort côté navigateur ; le serveur reste la référence. */
export const EMAIL_PATTERN = /^[^@\s]+@[^@\s]+\.[^@\s]+$/
