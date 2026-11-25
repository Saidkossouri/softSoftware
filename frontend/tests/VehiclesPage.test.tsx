import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { App } from '@/App'
import { ADMIN, goTo, stubApi, USER } from './apiStub'

const clio = {
  id: '33333333-3333-3333-3333-333333333333',
  nickname: 'La Clio',
  make: 'Renault',
  model: 'Clio',
  registration: 'AB-123-CD',
  initialMileageKm: 45000,
  status: 'ACTIVE',
  createdAt: '2026-11-20T08:00:00Z',
  updatedAt: '2026-11-20T08:00:00Z',
}

describe('accueil « Mes véhicules »', () => {
  afterEach(() => goTo('/'))

  it('indique à un membre sans véhicule de demander à un administrateur', async () => {
    stubApi({
      'GET /api/me': { status: 200, body: USER },
      'GET /api/vehicles': { status: 200, body: [] },
    })
    render(<App />)

    expect(
      await screen.findByText('Aucun véhicule ne vous est encore attribué. Demandez à un administrateur.'),
    ).toBeInTheDocument()
    expect(screen.getByText('Mes véhicules')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Parc' })).not.toBeInTheDocument()
  })

  it('liste les véhicules attribués avec un lien vers leur fiche', async () => {
    stubApi({
      'GET /api/me': { status: 200, body: USER },
      'GET /api/vehicles': { status: 200, body: [clio] },
    })
    render(<App />)

    const link = await screen.findByRole('link', { name: 'Ouvrir La Clio' })
    expect(link).toHaveAttribute('href', `/vehicules/${clio.id}`)
    expect(screen.getByText(/^45\s000 km$/)).toBeInTheDocument()
  })

  it('propose à un administrateur d’ajouter le premier véhicule', async () => {
    stubApi({
      'GET /api/me': { status: 200, body: ADMIN },
      'GET /api/vehicles': { status: 200, body: [] },
    })
    render(<App />)

    expect(await screen.findByText('Aucun véhicule dans le parc pour le moment.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ajouter un véhicule' })).toHaveAttribute('href', '/admin/parc/nouveau')
  })
})

describe('fiche d’un véhicule', () => {
  afterEach(() => goTo('/'))

  it('affiche « Accès refusé » quand le véhicule n’est pas attribué', async () => {
    stubApi({
      'GET /api/me': { status: 200, body: USER },
      [`GET /api/vehicles/${clio.id}`]: {
        status: 403,
        body: { status: 403, code: 'VEHICLE_FORBIDDEN', detail: "Vous n'avez pas accès à ce véhicule." },
      },
    })
    goTo(`/vehicules/${clio.id}`)
    render(<App />)

    expect(await screen.findByText('Accès refusé')).toBeInTheDocument()
  })

  it('affiche les informations du véhicule', async () => {
    stubApi({
      'GET /api/me': { status: 200, body: USER },
      [`GET /api/vehicles/${clio.id}`]: { status: 200, body: clio },
    })
    goTo(`/vehicules/${clio.id}`)
    render(<App />)

    expect(await screen.findByRole('heading', { name: 'La Clio' })).toBeInTheDocument()
    expect(screen.getByText('AB-123-CD')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Modifier' })).not.toBeInTheDocument()
  })
})
