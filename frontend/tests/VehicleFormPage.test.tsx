import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it } from 'vitest'
import { App } from '@/App'
import { ADMIN, goTo, stubApi, USER } from './apiStub'

const vehicle = {
  id: '33333333-3333-3333-3333-333333333333',
  nickname: 'La familiale',
  make: 'Peugeot',
  model: '5008',
  registration: 'AB-123-CD',
  initialMileageKm: 45000,
  currentMileage: { mileageKm: 45000, date: '2026-11-20' },
  status: 'ACTIVE',
  createdAt: '2026-11-20T08:00:00Z',
  updatedAt: '2026-11-20T08:00:00Z',
}

async function fillRequiredFields(user: ReturnType<typeof userEvent.setup>) {
  await user.type(await screen.findByLabelText('Surnom'), 'Citadine')
  await user.type(screen.getByLabelText('Marque'), 'Renault')
  await user.type(screen.getByLabelText('Modèle'), 'Clio')
  await user.type(screen.getByLabelText('Immatriculation'), 'ab-123 cd')
  await user.type(screen.getByLabelText('Kilométrage de départ (km)'), '12000')
}

describe('parc de véhicules', () => {
  afterEach(() => goTo('/'))

  it('affiche le doublon d’immatriculation sous le champ sans perdre la saisie', async () => {
    stubApi({
      'GET /api/me': { status: 200, body: ADMIN },
      'POST /api/vehicles': {
        status: 409,
        body: {
          status: 409,
          code: 'REGISTRATION_ALREADY_EXISTS',
          detail: 'Cette immatriculation est déjà utilisée par un autre véhicule (actif ou archivé).',
        },
      },
    })
    goTo('/admin/parc/nouveau')
    render(<App />)
    const user = userEvent.setup()

    await fillRequiredFields(user)
    await user.click(screen.getByRole('button', { name: 'Ajouter le véhicule' }))

    expect(
      await screen.findByText('Cette immatriculation est déjà utilisée par un autre véhicule (actif ou archivé).'),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('Immatriculation')).toHaveValue('ab-123 cd')
    expect(screen.getByLabelText('Immatriculation')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText('Surnom')).toHaveValue('Citadine')
  })

  it('envoie les champs facultatifs vides comme absents', async () => {
    let sent: unknown
    stubApi({
      'GET /api/me': { status: 200, body: ADMIN },
      'GET /api/vehicles': { status: 200, body: [] },
      'POST /api/vehicles': async (request) => {
        sent = await request.json()
        return { status: 201, body: vehicle }
      },
    })
    goTo('/admin/parc/nouveau')
    render(<App />)
    const user = userEvent.setup()

    await fillRequiredFields(user)
    await user.selectOptions(screen.getByLabelText("Type d'énergie (facultatif)"), 'Diesel')
    await user.click(screen.getByRole('button', { name: 'Ajouter le véhicule' }))

    expect(await screen.findByText('Parc de véhicules')).toBeInTheDocument()
    expect(sent).toEqual({
      nickname: 'Citadine',
      make: 'Renault',
      model: 'Clio',
      registration: 'ab-123 cd',
      initialMileageKm: 12000,
      energyType: 'DIESEL',
    })
  })

  it('refuse un VIN invalide avant l’envoi', async () => {
    const fetchMock = stubApi({ 'GET /api/me': { status: 200, body: ADMIN } })
    goTo('/admin/parc/nouveau')
    render(<App />)
    const user = userEvent.setup()

    await fillRequiredFields(user)
    await user.type(screen.getByLabelText('Numéro VIN (facultatif)'), 'VF3MCYHZRKS12345O')
    await user.click(screen.getByRole('button', { name: 'Ajouter le véhicule' }))

    expect(
      await screen.findByText('Le VIN compte 17 lettres ou chiffres, sans les lettres I, O et Q.'),
    ).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([input]) => String((input as Request).url ?? input).includes('/api/vehicles'))).toBe(false)
  })

  it('liste les archivés et réactive un véhicule', async () => {
    const archived = { ...vehicle, status: 'ARCHIVED' }
    let reactivated = false
    stubApi({
      'GET /api/me': { status: 200, body: ADMIN },
      'GET /api/vehicles': (request) => {
        const status = new URL(request.url).searchParams.get('status')
        if (status === 'ARCHIVED') return { status: 200, body: reactivated ? [] : [archived] }
        return { status: 200, body: reactivated ? [vehicle] : [] }
      },
      [`POST /api/vehicles/${vehicle.id}/reactivate`]: () => {
        reactivated = true
        return { status: 200, body: vehicle }
      },
    })
    goTo('/admin/parc')
    render(<App />)
    const user = userEvent.setup()

    expect(await screen.findByText('Aucun véhicule dans le parc pour le moment.')).toBeInTheDocument()
    await user.click(screen.getByRole('tab', { name: 'Archivés' }))
    const list = await screen.findByRole('list', { name: 'Archivés' })
    await user.click(within(list).getByRole('button', { name: 'Réactiver La familiale' }))

    expect(await screen.findByText('La familiale est de nouveau en service.')).toBeInTheDocument()
    expect(await screen.findByText('Aucun véhicule archivé.')).toBeInTheDocument()
  })

  it('confirme l’archivage dans un dialogue, annulable sans appel', async () => {
    const fetchMock = stubApi({
      'GET /api/me': { status: 200, body: ADMIN },
      [`GET /api/vehicles/${vehicle.id}`]: { status: 200, body: vehicle },
    })
    goTo(`/admin/parc/${vehicle.id}`)
    render(<App />)
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Archiver le véhicule' }))
    const dialog = screen.getByRole('dialog', { name: 'Archiver le véhicule' })
    expect(within(dialog).getByRole('button', { name: "Confirmer l'archivage" })).toBeInTheDocument()
    await user.click(within(dialog).getByRole('button', { name: 'Annuler' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([request]) => (request as Request).url?.endsWith('/archive'))).toBe(false)
  })

  it('masque le parc à un USER', async () => {
    stubApi({ 'GET /api/me': { status: 200, body: USER } })
    goTo('/admin/parc')
    render(<App />)

    expect(await screen.findByText('Accès refusé')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Parc' })).not.toBeInTheDocument()
  })
})
