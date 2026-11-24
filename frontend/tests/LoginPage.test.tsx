import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { App } from '@/App'

function stubMeResponse(status: number, body: unknown) {
  vi.stubGlobal(
    'fetch',
    vi.fn(async () =>
      new Response(JSON.stringify(body), {
        status,
        headers: { 'Content-Type': status < 400 ? 'application/json' : 'application/problem+json' },
      }),
    ),
  )
}

describe('connexion', () => {
  it("affiche l'écran de connexion Google quand /api/me répond 401", async () => {
    stubMeResponse(401, { status: 401, code: 'UNAUTHENTICATED', detail: 'Connectez-vous.' })

    render(<App />)

    const button = await screen.findByRole('link', { name: 'Se connecter avec Google' })
    expect(button).toHaveAttribute('href', '/oauth2/authorization/google')
  })

  it('affiche le nom, l’email et le rôle du membre connecté', async () => {
    stubMeResponse(200, { id: 'b3c1', email: 'said@example.com', displayName: 'Said', role: 'ADMIN' })

    render(<App />)

    expect(await screen.findByText('Said')).toBeInTheDocument()
    expect(screen.getByText('said@example.com')).toBeInTheDocument()
    expect(screen.getByText('Administrateur')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Se déconnecter' })).toBeInTheDocument()
  })

  it("affiche l'accès refusé quand le membre a été retiré (403)", async () => {
    stubMeResponse(403, { status: 403, code: 'MEMBER_REVOKED', detail: 'Accès retiré.' })

    render(<App />)

    expect(await screen.findByText('Accès refusé')).toBeInTheDocument()
  })
})
