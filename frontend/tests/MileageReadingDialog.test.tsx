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
  initialMileageKm: 50000,
  currentMileage: { mileageKm: 50000, date: '2026-11-20' },
  status: 'ACTIVE',
  createdAt: '2026-11-20T08:00:00Z',
  updatedAt: '2026-11-20T08:00:00Z',
}

function reading(mileageKm: number) {
  return {
    id: '55555555-5555-5555-5555-555555555555',
    vehicleId: logan.id,
    readingDate: '2026-11-25',
    mileageKm,
    authorName: 'Enfant',
    canEdit: true,
    createdAt: '2026-11-25T08:00:00Z',
    updatedAt: '2026-11-25T08:00:00Z',
    currentMileage: { mileageKm, date: '2026-11-25' },
  }
}

async function openDialog() {
  render(<App />)
  await userEvent.click(await screen.findByRole('button', { name: 'Relever le kilométrage de La Logan' }))
  const km = screen.getByLabelText('Kilométrage au compteur (km)')
  expect(km).toHaveValue('50000')
  await userEvent.clear(km)
  return km
}

describe('relevé du kilométrage', () => {
  afterEach(() => goTo('/'))

  it('enregistre le relevé et confirme par un message', async () => {
    const bodies: unknown[] = []
    stubApi({
      'GET /api/me': { status: 200, body: USER },
      'GET /api/vehicles': { status: 200, body: [logan] },
      [`POST /api/vehicles/${logan.id}/mileage-readings`]: async (request) => {
        bodies.push(await request.json())
        return { status: 201, body: reading(51200) }
      },
    })
    const km = await openDialog()
    await userEvent.type(km, '51200')
    await userEvent.click(screen.getByRole('button', { name: 'Enregistrer le relevé' }))

    expect(await screen.findByText(/Kilométrage de 51\s200\skm enregistré\./)).toBeInTheDocument()
    expect(bodies).toEqual([expect.objectContaining({ mileageKm: 51200, confirmLargeJump: false })])
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('affiche sous le champ le relevé qui bloque, sans perdre la saisie', async () => {
    const detail = 'Un kilométrage de 51 200 km a été saisi le 24/11/2026.'
    stubApi({
      'GET /api/me': { status: 200, body: USER },
      'GET /api/vehicles': { status: 200, body: [logan] },
      [`POST /api/vehicles/${logan.id}/mileage-readings`]: {
        status: 409,
        body: { status: 409, code: 'MILEAGE_INCONSISTENT', detail },
      },
    })
    const km = await openDialog()
    await userEvent.type(km, '50900')
    await userEvent.click(screen.getByRole('button', { name: 'Enregistrer le relevé' }))

    expect(await screen.findByText(detail)).toBeInTheDocument()
    expect(km).toHaveValue('50900')
    expect(km).toHaveAttribute('aria-invalid', 'true')
  })

  it('demande une confirmation pour un grand écart puis renvoie le relevé confirmé', async () => {
    const bodies: { confirmLargeJump?: boolean }[] = []
    stubApi({
      'GET /api/me': { status: 200, body: USER },
      'GET /api/vehicles': { status: 200, body: [logan] },
      [`POST /api/vehicles/${logan.id}/mileage-readings`]: async (request) => {
        const body = (await request.json()) as { confirmLargeJump?: boolean }
        bodies.push(body)
        return body.confirmLargeJump
          ? { status: 201, body: reading(251300) }
          : {
              status: 409,
              body: {
                status: 409,
                code: 'MILEAGE_JUMP_NOT_CONFIRMED',
                detail: 'Écart de plus de 100 000 km avec le kilométrage précédent. Confirmez-le.',
              },
            }
      },
    })
    const km = await openDialog()
    await userEvent.type(km, '251300')
    await userEvent.click(screen.getByRole('button', { name: 'Enregistrer le relevé' }))

    expect(await screen.findByText('Écart inhabituel')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Confirmer ce kilométrage' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(bodies.map((body) => body.confirmLargeJump)).toEqual([false, true])
  })

  it('refuse un kilométrage non entier avant l’envoi', async () => {
    const fetchMock = stubApi({
      'GET /api/me': { status: 200, body: USER },
      'GET /api/vehicles': { status: 200, body: [logan] },
    })
    const km = await openDialog()
    await userEvent.type(km, '51 200,5')
    await userEvent.click(screen.getByRole('button', { name: 'Enregistrer le relevé' }))

    expect(await screen.findByText('Saisissez un kilométrage entier, sans espace ni décimale.')).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([input]) => String(input instanceof Request ? input.url : input).includes('mileage-readings'))).toBe(false)
  })
})
