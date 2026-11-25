import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { DueItem } from '@/api/client'
import { DueItemCard } from '@/components/DueItemCard'
import { formatKm } from '@/lib/format'

/** Garde les espaces insécables des kilométrages tels que formatés. */
const exact = { normalizer: (text: string) => text.trim() }

const vidange: DueItem = {
  kind: 'RULE',
  status: 'SOON',
  ruleId: 'r1',
  type: { id: 't1', name: 'Vidange', active: true },
  label: 'Vidange',
  intervalKm: 15000,
  intervalMonths: 12,
  lastDoneDate: '2025-09-15',
  lastDoneMileageKm: 45000,
  dueDate: '2026-09-15',
  dueMileageKm: 60000,
  remainingDays: 92,
  remainingKm: 800,
}

describe('carte d’échéance', () => {
  it('affiche l’état, les restes, les termes et la dernière réalisation d’une règle', () => {
    render(<DueItemCard item={vidange} />)

    expect(screen.getByText('Vidange')).toBeInTheDocument()
    expect(screen.getByText('Bientôt')).toBeInTheDocument()
    expect(screen.getByText(`dans ${formatKm(800)} · dans 92 jours`, exact)).toBeInTheDocument()
    expect(
      screen.getByText(`le 15/09/2026 · à ${formatKm(60000)} · Tous les ${formatKm(15000)} ou 12 mois`, exact),
    ).toBeInTheDocument()
    expect(screen.getByText(`Dernière fois le 15/09/2025 à ${formatKm(45000)}`, exact)).toBeInTheDocument()
  })

  it('formule un retard en kilomètres et en jours', () => {
    render(<DueItemCard item={{ ...vidange, status: 'OVERDUE', remainingKm: -400, remainingDays: -1 }} />)

    expect(screen.getByText('En retard')).toBeInTheDocument()
    expect(screen.getByText(`en retard de ${formatKm(400)} · en retard de 1 jour`, exact)).toBeInTheDocument()
  })

  it('affiche une échéance à date fixe et sa récurrence', () => {
    render(
      <DueItemCard
        item={{
          kind: 'DEADLINE',
          status: 'OK',
          deadlineId: 'd1',
          label: 'Assurance',
          dueDate: '2027-04-01',
          recurrenceMonths: 12,
          remainingDays: 0,
        }}
        context="La Logan"
      />,
    )

    expect(screen.getByText('À jour')).toBeInTheDocument()
    expect(screen.getByText('La Logan')).toBeInTheDocument()
    expect(screen.getByText("à faire aujourd'hui")).toBeInTheDocument()
    expect(screen.getByText('le 01/04/2027 · Chaque année')).toBeInTheDocument()
  })
})
