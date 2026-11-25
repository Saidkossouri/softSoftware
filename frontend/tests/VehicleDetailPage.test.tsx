import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it } from 'vitest'
import { App } from '@/App'
import { formatKm, formatMoney } from '@/lib/format'
import { goTo, stubApi, USER } from './apiStub'

const logan = {
  id: '44444444-4444-4444-4444-444444444444',
  nickname: 'La Logan',
  make: 'Dacia',
  model: 'Logan',
  registration: '12345-A-6',
  initialMileageKm: 40000,
  currentMileage: { mileageKm: 49500, date: '2026-05-20' },
  status: 'ACTIVE',
  createdAt: '2025-01-10T08:00:00Z',
  updatedAt: '2026-05-20T08:00:00Z',
}

/** Garde les espaces insécables des montants et kilométrages tels que formatés. */
const exact = { normalizer: (text: string) => text.trim() }

const VIDANGE = '10000000-0000-4000-8000-000000000001'
const types = [
  { id: VIDANGE, name: 'Vidange', category: 'MAINTENANCE', requiresDescription: false, displayOrder: 1, active: true },
]

const reading = {
  kind: 'READING',
  id: '77777777-7777-7777-7777-777777777777',
  date: '2026-05-20',
  mileageKm: 49500,
  authorName: 'Enfant',
  canEdit: true,
  note: 'Plein',
}
const vidange = {
  kind: 'RECORD',
  id: '66666666-6666-6666-6666-666666666666',
  date: '2026-05-20',
  mileageKm: 49000,
  authorName: 'Enfant',
  canEdit: true,
  types: [{ id: VIDANGE, name: 'Vidange', active: true }],
  costCents: 80000,
  garage: 'Garage Atlas',
  documentCount: 2,
}

function history(entries: unknown[], currentYearCents = 110000, allTimeCents = 230000) {
  return {
    vehicleId: logan.id,
    currentMileage: logan.currentMileage,
    costTotals: { year: 2026, currentYearCents, allTimeCents },
    entries,
  }
}

describe('fiche véhicule', () => {
  afterEach(() => goTo('/'))

  it('affiche le kilométrage, les totaux en dirhams et l’historique, puis filtre par opération', async () => {
    const queries: string[] = []
    stubApi({
      'GET /api/me': { status: 200, body: USER },
      [`GET /api/vehicles/${logan.id}`]: { status: 200, body: logan },
      'GET /api/maintenance-types': { status: 200, body: types },
      [`GET /api/vehicles/${logan.id}/history`]: (request) => {
        const query = new URL(request.url).search
        queries.push(query)
        return { status: 200, body: history(query.includes('typeId') ? [vidange] : [reading, vidange]) }
      },
    })
    goTo(`/vehicules/${logan.id}`)
    render(<App />)

    expect(await screen.findByText('Dépenses 2026')).toBeInTheDocument()
    expect(screen.getByText(formatMoney(110000), exact)).toBeInTheDocument()
    expect(screen.getByText(formatMoney(230000), exact)).toBeInTheDocument()
    const list = screen.getByRole('list')
    expect(within(list).getAllByRole('listitem')).toHaveLength(2)
    expect(within(list).getByText(`Relevé : ${formatKm(49500)}`, exact)).toBeInTheDocument()
    expect(within(list).getByRole('link', { name: 'Vidange' })).toHaveAttribute('href', `/entretiens/${vidange.id}`)
    expect(within(list).getAllByText(formatMoney(80000), exact)).not.toHaveLength(0)
    expect(within(list).getByText('2 justificatifs')).toBeInTheDocument()

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Opération' }), VIDANGE)
    await waitFor(() => expect(queries.some((query) => query.includes(`typeId=${VIDANGE}`))).toBe(true))
    expect(await within(screen.getByRole('list')).findAllByRole('listitem')).toHaveLength(1)

    await userEvent.click(screen.getByRole('tab', { name: 'Entretiens' }))
    await waitFor(() => expect(queries.some((query) => query.includes('recordsOnly=true'))).toBe(true))
  })

  it('invite à une première saisie quand le véhicule n’a aucun historique', async () => {
    stubApi({
      'GET /api/me': { status: 200, body: USER },
      [`GET /api/vehicles/${logan.id}`]: { status: 200, body: logan },
      'GET /api/maintenance-types': { status: 200, body: types },
      [`GET /api/vehicles/${logan.id}/history`]: { status: 200, body: history([], 0, 0) },
    })
    goTo(`/vehicules/${logan.id}`)
    render(<App />)

    expect(await screen.findByText('Aucune saisie pour le moment')).toBeInTheDocument()
    expect(screen.getAllByText(formatMoney(0), exact)).toHaveLength(2)
    expect(screen.getByRole('link', { name: 'Enregistrer un entretien' })).toBeInTheDocument()
  })
})
