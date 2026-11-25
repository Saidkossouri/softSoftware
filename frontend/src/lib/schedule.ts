import type { DueItem, DueStatus } from '@/api/client'
import { fr } from '@/i18n/fr'
import { formatDate, formatKm } from '@/lib/format'

/** Restes d'une échéance en mots : « dans 800 km · dans 92 jours », « en retard de 400 km ». */
export function remainingText(item: DueItem): string {
  const parts: string[] = []
  if (item.remainingKm != null) {
    const km = item.remainingKm
    parts.push(km < 0 ? fr.schedule.lateKm(formatKm(-km)) : km === 0 ? fr.schedule.dueNow : fr.schedule.inKm(formatKm(km)))
  }
  if (item.remainingDays != null) {
    const days = item.remainingDays
    parts.push(days < 0 ? fr.schedule.lateDays(-days) : days === 0 ? fr.schedule.dueToday : fr.schedule.inDays(days))
  }
  return parts.join(' · ')
}

/** Termes de l'échéance : « le 15/09/2026 · à 60 000 km ». */
export function dueText(item: DueItem): string {
  const parts: string[] = []
  if (item.dueDate) parts.push(fr.schedule.dueOn(formatDate(item.dueDate)))
  if (item.dueMileageKm != null) parts.push(fr.schedule.dueAt(formatKm(item.dueMileageKm)))
  return parts.join(' · ')
}

/** Fréquence d'une règle ou récurrence d'une échéance à date fixe. */
export function frequencyText(item: DueItem): string {
  if (item.kind === 'RULE') {
    return fr.schedule.every(item.intervalKm != null ? formatKm(item.intervalKm) : null, item.intervalMonths ?? null)
  }
  return item.recurrenceMonths ? fr.schedule.recurrence(item.recurrenceMonths) : fr.schedule.oneOff
}

export function itemName(item: DueItem): string {
  return item.label ?? item.type?.name ?? ''
}

export const statusVariant = {
  OVERDUE: 'destructive',
  SOON: 'warning',
  OK: 'success',
} as const satisfies Record<DueStatus, string>
