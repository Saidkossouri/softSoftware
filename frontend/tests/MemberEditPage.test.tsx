import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it } from 'vitest'
import { App } from '@/App'
import { ADMIN, goTo, stubApi } from './apiStub'

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
