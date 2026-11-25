import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it } from 'vitest'
import { App } from '@/App'
import { goTo, stubApi, USER } from './apiStub'

const logan = {
  id: '44444444-4444-4444-4444-444444444444',
  nickname: 'La Logan',
  make: 'Dacia',
  model: 'Logan',
  registration: '12345-A-6',
  initialMileageKm: 40000,
  firstRegistrationYear: 2020,
  currentMileage: { mileageKm: 49500, date: '2026-05-01' },
  status: 'ACTIVE',
  createdAt: '2025-01-10T08:00:00Z',
  updatedAt: '2026-05-20T08:00:00Z',
}

const VIDANGE = '10000000-0000-4000-8000-000000000001'
const ASSURANCE = '20000000-0000-4000-8000-000000000001'
const VISITE = '20000000-0000-4000-8000-000000000003'
const maintenanceTypes = [
  {
    id: VIDANGE,
    name: 'Vidange',
    category: 'MAINTENANCE',
    defaultIntervalKm: 10000,
    defaultIntervalMonths: 12,
    requiresDescription: false,
    displayOrder: 1,
    active: true,
  },
]
const adminTypes = [
  {
    id: ASSURANCE,
    name: 'Assurance',
    category: 'ADMINISTRATIVE',
    defaultIntervalMonths: 12,
    requiresDescription: false,
    suggestFromAgeYears: 0,
    displayOrder: 1,
    active: true,
  },
]
const assurance = {
  kind: 'DEADLINE',
  status: 'SOON',
  deadlineId: '55555555-5555-5555-5555-555555555555',
  type: { id: ASSURANCE, name: 'Assurance', active: true },
  label: 'Assurance',
  dueDate: '2027-04-01',
  recurrenceMonths: 12,
  remainingDays: 12,
}

function routes(extra: Record<string, Parameters<typeof stubApi>[0][string]> = {}) {
  return {
    'GET /api/me': { status: 200, body: USER },
    [`GET /api/vehicles/${logan.id}`]: { status: 200, body: logan },
    [`GET /api/vehicles/${logan.id}/due-items`]: { status: 200, body: [assurance] },
    [`GET /api/vehicles/${logan.id}/deadline-suggestions`]: {
      status: 200,
      body: [{ type: { id: VISITE, name: 'Visite technique', active: true }, recurrenceMonths: 12 }],
    },
    'GET /api/maintenance-types': (request: Request) => {
      const category = new URL(request.url).searchParams.get('category')
      return { status: 200, body: category === 'ADMINISTRATIVE' ? adminTypes : maintenanceTypes }
    },
    'GET /api/reminders': { status: 200, body: { dueItems: [], mileageReadings: [] } },
    ...extra,
  }
}

describe('plan d’entretien', () => {
  afterEach(() => goTo('/'))

  it('crée une fréquence pré-remplie avec la fréquence conseillée et le kilométrage actuel', async () => {
    const bodies: unknown[] = []
    stubApi(
      routes({
        [`POST /api/vehicles/${logan.id}/maintenance-rules`]: async (request) => {
          bodies.push(await request.json())
          return {
            status: 201,
            body: {
              id: 'r1',
              vehicleId: logan.id,
              type: { id: VIDANGE, name: 'Vidange', active: true },
              baselineDate: '2026-05-01',
              baselineMileageKm: 49500,
            },
          }
        },
      }),
    )
    goTo(`/vehicules/${logan.id}/plan`)
    render(<App />)

    await userEvent.click(await screen.findByRole('button', { name: 'Ajouter une fréquence' }))
    await userEvent.selectOptions(await screen.findByLabelText('Opération'), VIDANGE)
    expect(screen.getByLabelText('Tous les (km)')).toHaveValue('10000')
    expect(screen.getByLabelText('Ou tous les (mois)')).toHaveValue('12')
    expect(screen.getByLabelText('Dernière fois (km)')).toHaveValue('49500')
    await userEvent.clear(screen.getByLabelText('Tous les (km)'))
    await userEvent.type(screen.getByLabelText('Tous les (km)'), '15000')
    await userEvent.click(screen.getByRole('button', { name: 'Enregistrer' }))

    await waitFor(() => expect(bodies).toHaveLength(1))
    expect(bodies[0]).toMatchObject({ typeId: VIDANGE, intervalKm: 15000, intervalMonths: 12, baselineMileageKm: 49500 })
    expect(await screen.findByText('Fréquence enregistrée.')).toBeInTheDocument()
  })

  it('marque une échéance comme faite et annonce la suivante', async () => {
    stubApi(
      routes({
        [`POST /api/deadlines/${assurance.deadlineId}/complete`]: {
          status: 200,
          body: {
            completed: { id: assurance.deadlineId, vehicleId: logan.id, label: 'Assurance', dueDate: '2027-04-01', done: true },
            next: { id: 'n1', vehicleId: logan.id, label: 'Assurance', dueDate: '2028-04-01', done: false },
          },
        },
      }),
    )
    goTo(`/vehicules/${logan.id}/plan`)
    render(<App />)

    expect(await screen.findByText('Visite technique')).toBeInTheDocument()
    await userEvent.click(await screen.findByRole('button', { name: 'Marquer Assurance comme faite' }))

    expect(
      await screen.findByText('« Assurance » marquée comme faite. Prochaine échéance le 01/04/2028.'),
    ).toBeInTheDocument()
  })
})
