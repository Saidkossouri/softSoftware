import { describe, expect, it } from 'vitest'
import { formatDate, formatKm, formatMoney, parseMoneyToCents, todayIso } from '@/lib/format'

/** Espaces insécables remplacés par des espaces simples pour comparer lisiblement. */
const plain = (text: string) => text.replace(/[\u00a0\u202f]/g, ' ')

describe('format', () => {
  it('affiche un kilométrage avec le séparateur de milliers', () => {
    expect(plain(formatKm(52000))).toBe('52 000 km')
    expect(plain(formatKm(0))).toBe('0 km')
  })

  it('affiche un montant en dirhams', () => {
    expect(plain(formatMoney(123450))).toBe('1 234,50 DH')
    expect(plain(formatMoney(18950))).toBe('189,50 DH')
    expect(plain(formatMoney(5))).toBe('0,05 DH')
  })

  it('convertit une saisie de montant en centimes', () => {
    expect(parseMoneyToCents('189,5')).toBe(18950)
    expect(parseMoneyToCents('189.50')).toBe(18950)
    expect(parseMoneyToCents('1 234,50')).toBe(123450)
    expect(parseMoneyToCents('450')).toBe(45000)
    expect(parseMoneyToCents('0,07')).toBe(7)
  })

  it('refuse un montant invalide', () => {
    for (const input of ['', 'abc', '12,345', '-5', '1,2,3']) {
      expect(() => parseMoneyToCents(input)).toThrow()
    }
  })

  it('affiche une date du contrat en jour/mois/année', () => {
    expect(formatDate('2026-11-24')).toBe('24/11/2026')
  })

  it("donne la date du jour de l'appareil", () => {
    expect(todayIso(new Date(2026, 0, 5, 23, 59))).toBe('2026-01-05')
  })
})
