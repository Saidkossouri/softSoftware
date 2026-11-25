import { render, screen } from '@testing-library/react'
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
  initialMileageKm: 50000,
  currentMileage: { mileageKm: 51200, date: '2026-11-20' },
  status: 'ACTIVE',
  createdAt: '2026-11-20T08:00:00Z',
  updatedAt: '2026-11-20T08:00:00Z',
}

const VIDANGE = '10000000-0000-4000-8000-000000000001'
const AUTRE = '10000000-0000-4000-8000-000000000013'
const types = [
  { id: VIDANGE, name: 'Vidange', category: 'MAINTENANCE', requiresDescription: false, displayOrder: 1, active: true },
  { id: AUTRE, name: 'Autre', category: 'MAINTENANCE', requiresDescription: true, displayOrder: 13, active: true },
]

function saved(body: Record<string, unknown>) {
  return {
    ...body,
    id: '66666666-6666-6666-6666-666666666666',
    vehicleId: logan.id,
    types: [],
    authorName: 'Enfant',
    canEdit: true,
    documentCount: 0,
    createdAt: '2026-11-25T08:00:00Z',
    updatedAt: '2026-11-25T08:00:00Z',
    currentMileage: { mileageKm: 52000, date: '2026-11-25' },
  }
}

describe('formulaire d’entretien', () => {
  afterEach(() => goTo('/'))

  it('envoie les types cochés et le coût converti en centimes', async () => {
    const bodies: Record<string, unknown>[] = []
    stubApi({
      'GET /api/me': { status: 200, body: USER },
      [`GET /api/vehicles/${logan.id}`]: { status: 200, body: logan },
      'GET /api/maintenance-types': { status: 200, body: types },
      [`POST /api/vehicles/${logan.id}/maintenance-records`]: async (request) => {
        const body = (await request.json()) as Record<string, unknown>
        bodies.push(body)
        return { status: 201, body: saved(body) }
      },
      'GET /api/maintenance-records/66666666-6666-6666-6666-666666666666': { status: 200, body: saved({}) },
    })
    goTo(`/vehicules/${logan.id}/entretiens/nouveau`)
    render(<App />)

    const km = await screen.findByLabelText('Kilométrage au compteur (km)')
    expect(km).toHaveValue('51200')
    await userEvent.clear(km)
    await userEvent.type(km, '52000')
    await userEvent.click(await screen.findByRole('checkbox', { name: 'Vidange' }))
    await userEvent.type(screen.getByLabelText('Coût total TTC (DH, facultatif)'), '189,5')
    await userEvent.type(screen.getByLabelText('Garage (facultatif)'), 'Garage du Centre')
    await userEvent.click(screen.getByRole('button', { name: "Enregistrer l'entretien" }))

    expect(await screen.findByText('Entretien enregistré.')).toBeInTheDocument()
    expect(bodies).toEqual([
      expect.objectContaining({ mileageKm: 52000, typeIds: [VIDANGE], costCents: 18950, garage: 'Garage du Centre' }),
    ])
  })

  it('exige une description pour un type qui l’impose', async () => {
    const fetchMock = stubApi({
      'GET /api/me': { status: 200, body: USER },
      [`GET /api/vehicles/${logan.id}`]: { status: 200, body: logan },
      'GET /api/maintenance-types': { status: 200, body: types },
    })
    goTo(`/vehicules/${logan.id}/entretiens/nouveau`)
    render(<App />)

    await userEvent.click(await screen.findByRole('checkbox', { name: 'Autre' }))
    expect(screen.getByLabelText('Description (obligatoire pour « Autre »)')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: "Enregistrer l'entretien" }))

    expect(await screen.findByText('Ce champ est obligatoire.')).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([input]) => input instanceof Request && input.method === 'POST')).toBe(false)
  })

  it('refuse un coût invalide et l’absence d’opération', async () => {
    stubApi({
      'GET /api/me': { status: 200, body: USER },
      [`GET /api/vehicles/${logan.id}`]: { status: 200, body: logan },
      'GET /api/maintenance-types': { status: 200, body: types },
    })
    goTo(`/vehicules/${logan.id}/entretiens/nouveau`)
    render(<App />)

    await userEvent.type(await screen.findByLabelText('Coût total TTC (DH, facultatif)'), '12,345')
    await userEvent.click(screen.getByRole('button', { name: "Enregistrer l'entretien" }))

    expect(await screen.findByText('Saisissez un montant positif, par exemple 450 ou 189,50.')).toBeInTheDocument()
    expect(screen.getByText('Choisissez au moins une opération.')).toBeInTheDocument()
  })
})
