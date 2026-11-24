import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it } from 'vitest'
import { App } from '@/App'
import { ADMIN, goTo, stubApi, USER } from './apiStub'

const memberRow = { ...ADMIN, displayName: 'Said', createdAt: '2026-11-20T08:00:00Z', lastLoginAt: '2026-11-24T08:00:00Z' }

describe('gestion des membres', () => {
  afterEach(() => goTo('/'))

  it('affiche le refus de doublon sous le champ email sans perdre la saisie', async () => {
    stubApi({
      'GET /api/me': { status: 200, body: ADMIN },
      'GET /api/members': { status: 200, body: [memberRow] },
      'POST /api/members': {
        status: 409,
        body: { status: 409, code: 'EMAIL_ALREADY_EXISTS', detail: 'Cet email est déjà dans la liste des membres.' },
      },
    })
    goTo('/admin/membres')
    render(<App />)
    const user = userEvent.setup()

    await user.type(await screen.findByLabelText('Email Google'), 'Said@Example.com')
    await user.type(screen.getByLabelText('Nom affiché (facultatif)'), 'Papa')
    await user.click(screen.getByRole('button', { name: 'Ajouter le membre' }))

    expect(await screen.findByText('Cet email est déjà dans la liste des membres.')).toBeInTheDocument()
    expect(screen.getByLabelText('Email Google')).toHaveValue('Said@Example.com')
    expect(screen.getByLabelText('Nom affiché (facultatif)')).toHaveValue('Papa')
    expect(screen.getByLabelText('Email Google')).toHaveAttribute('aria-invalid', 'true')
  })

  it('ajoute un membre, vide le formulaire et envoie un email sans espaces', async () => {
    let sent: unknown
    stubApi({
      'GET /api/me': { status: 200, body: ADMIN },
      'GET /api/members': { status: 200, body: [memberRow] },
      'POST /api/members': async (request) => {
        sent = await request.json()
        return { status: 201, body: { id: 'n', email: 'epouse@example.com', role: 'USER', createdAt: '2026-11-24T09:00:00Z' } }
      },
    })
    goTo('/admin/membres')
    render(<App />)
    const user = userEvent.setup()

    await user.type(await screen.findByLabelText('Email Google'), ' epouse@example.com ')
    await user.click(screen.getByRole('button', { name: 'Ajouter le membre' }))

    expect(await screen.findByText('epouse@example.com a été ajouté.')).toBeInTheDocument()
    expect(sent).toEqual({ email: 'epouse@example.com', role: 'USER' })
    expect(screen.getByLabelText('Email Google')).toHaveValue('')
  })

  it('liste les membres avec leur rôle et leur dernière connexion', async () => {
    stubApi({
      'GET /api/me': { status: 200, body: ADMIN },
      'GET /api/members': {
        status: 200,
        body: [memberRow, { ...USER, createdAt: '2026-11-21T08:00:00Z' }],
      },
    })
    goTo('/admin/membres')
    render(<App />)

    const list = await screen.findByRole('list')
    expect(within(list).getByText('Jamais connecté')).toBeInTheDocument()
    expect(within(list).getAllByText('Administrateur')).toHaveLength(1)
    expect(within(list).getByText('Membre')).toBeInTheDocument()
  })

  it('masque l’administration à un USER et affiche un accès refusé', async () => {
    stubApi({ 'GET /api/me': { status: 200, body: USER } })
    goTo('/admin/membres')
    render(<App />)

    expect(await screen.findByText('Accès refusé')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Membres' })).not.toBeInTheDocument()
  })
})
