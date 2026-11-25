import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it } from 'vitest'
import { App } from '@/App'
import { ADMIN, goTo, stubApi, USER } from './apiStub'

const self = { ...ADMIN, displayName: 'Said', createdAt: '2026-11-20T08:00:00Z' }

describe('modification d’un membre', () => {
  afterEach(() => goTo('/'))

  it('affiche le refus « dernier administrateur » sous le rôle', async () => {
    stubApi({
      'GET /api/me': { status: 200, body: ADMIN },
      [`GET /api/members/${ADMIN.id}`]: { status: 200, body: self },
      [`PATCH /api/members/${ADMIN.id}`]: {
        status: 409,
        body: { status: 409, code: 'LAST_ADMIN', detail: 'Il doit toujours rester au moins un administrateur.' },
      },
    })
    goTo(`/admin/membres/${ADMIN.id}`)
    render(<App />)
    const user = userEvent.setup()

    await user.selectOptions(await screen.findByLabelText('Rôle'), 'USER')
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }))

    expect(await screen.findByText('Il doit toujours rester au moins un administrateur.')).toBeInTheDocument()
    expect(screen.getByLabelText('Rôle')).toHaveValue('USER')
  })

  it('demande une confirmation avant de retirer l’accès', async () => {
    const fetchMock = stubApi({
      'GET /api/me': { status: 200, body: ADMIN },
      [`GET /api/members/${ADMIN.id}`]: { status: 200, body: self },
    })
    goTo(`/admin/membres/${ADMIN.id}`)
    render(<App />)
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: "Retirer l'accès" }))

    expect(screen.getByRole('button', { name: 'Confirmer le retrait' })).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([request]) => (request as Request).method === 'DELETE')).toBe(false)
  })
})

describe('véhicules accessibles d’un membre', () => {
  afterEach(() => goTo('/'))

  const child = { ...USER, displayName: 'Leila', createdAt: '2026-11-20T08:00:00Z' }
  const vehicle = (id: string, nickname: string, status: 'ACTIVE' | 'ARCHIVED') => ({
    id,
    nickname,
    make: 'Renault',
    model: 'Clio',
    registration: `${id.slice(0, 2)}-123-CD`,
    initialMileageKm: 0,
    status,
    createdAt: '2026-11-20T08:00:00Z',
    updatedAt: '2026-11-20T08:00:00Z',
  })
  const a = vehicle('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Citadine', 'ACTIVE')
  const b = vehicle('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Zoé', 'ACTIVE')
  const old = vehicle('cccccccc-cccc-cccc-cccc-cccccccccccc', 'Ancienne', 'ARCHIVED')

  it('enregistre la sélection en un seul PUT', async () => {
    let sent: unknown
    stubApi({
      'GET /api/me': { status: 200, body: ADMIN },
      [`GET /api/members/${USER.id}`]: { status: 200, body: child },
      [`GET /api/members/${USER.id}/vehicles`]: { status: 200, body: { vehicleIds: [a.id, old.id] } },
      'GET /api/vehicles': (request) => ({
        status: 200,
        body: new URL(request.url).searchParams.get('status') === 'ARCHIVED' ? [old] : [a, b],
      }),
      [`PUT /api/members/${USER.id}/vehicles`]: async (request) => {
        sent = await request.json()
        return { status: 200, body: sent }
      },
    })
    goTo(`/admin/membres/${USER.id}`)
    render(<App />)
    const user = userEvent.setup()

    expect(await screen.findByLabelText(/Citadine/)).toBeChecked()
    expect(screen.getByLabelText(/Zoé/)).not.toBeChecked()
    expect(screen.getByText(/Archivé : invisible pour le membre/)).toBeInTheDocument()

    await user.click(screen.getByLabelText(/Zoé/))
    await user.click(screen.getByLabelText(/Ancienne/))
    await user.click(screen.getByRole('button', { name: 'Enregistrer les accès' }))

    expect(await screen.findByText('Accès aux véhicules enregistrés.')).toBeInTheDocument()
    expect(sent).toEqual({ vehicleIds: [a.id, b.id] })
  })

  it('n’affiche pas la section pour un administrateur', async () => {
    stubApi({
      'GET /api/me': { status: 200, body: ADMIN },
      [`GET /api/members/${ADMIN.id}`]: { status: 200, body: self },
    })
    goTo(`/admin/membres/${ADMIN.id}`)
    render(<App />)

    expect(await screen.findByRole('button', { name: "Retirer l'accès" })).toBeInTheDocument()
    expect(screen.queryByText('Véhicules accessibles')).not.toBeInTheDocument()
  })
})
