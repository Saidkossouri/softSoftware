/** Mise en forme des valeurs affichées (kilométrages, montants en dirhams, dates). */

const kmFormat = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 })
const moneyFormat = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

/** « 52 000 km » (séparateur de milliers français). */
export function formatKm(km: number): string {
  return `${kmFormat.format(km)}\u00a0km`
}

/**
 * Montant en centimes de dirham affiché « 1 234,50 DH », l'écriture courante au Maroc
 * (research R7 ; `currency: 'MAD'` afficherait « MAD »).
 */
export function formatMoney(cents: number): string {
  return `${moneyFormat.format(cents / 100)}\u00a0DH`
}

/**
 * Saisie d'un montant (« 189,5 », « 189.50 », « 1 234,50 ») convertie en centimes, sans arrondi
 * flottant. Lève une erreur si le texte n'est pas un montant positif à 2 décimales au plus.
 */
export function parseMoneyToCents(input: string): number {
  const text = input.replace(/[\s\u00a0\u202f]/g, '').replace(/dh$/i, '')
  const match = /^(\d+)(?:[.,](\d{1,2}))?$/.exec(text)
  if (!match) {
    throw new Error(`Montant invalide : ${input}`)
  }
  const units = Number(match[1])
  const decimals = Number((match[2] ?? '').padEnd(2, '0'))
  return units * 100 + decimals
}

/** Date ISO du contrat (« 2026-11-24 ») affichée « 24/11/2026 », sans conversion de fuseau. */
export function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.slice(0, 10).split('-')
  return `${day}/${month}/${year}`
}

/** Date du jour de l'appareil au format du contrat (« 2026-11-24 »). */
export function todayIso(now: Date = new Date()): string {
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}
