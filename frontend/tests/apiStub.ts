import { vi } from 'vitest'

type Reply = { status: number; body?: unknown }
type Handler = (request: Request) => Reply | Promise<Reply>

/** Remplace fetch : chaque route « MÉTHODE /chemin » renvoie une réponse JSON ou Problem Details. */
export function stubApi(routes: Record<string, Handler | Reply>) {
  const fetchMock = vi.fn(async (input: Request | string, init?: RequestInit) => {
    const request = input instanceof Request ? input : new Request(new URL(input, window.location.origin), init)
    const key = `${request.method} ${new URL(request.url).pathname}`
    const route = routes[key]
    if (!route) {
      return new Response(JSON.stringify({ status: 404, code: 'NOT_FOUND', detail: key }), { status: 404 })
    }
    const reply = typeof route === 'function' ? await route(request) : route
    return new Response(reply.body === undefined ? null : JSON.stringify(reply.body), {
      status: reply.status,
      headers: { 'Content-Type': reply.status < 400 ? 'application/json' : 'application/problem+json' },
    })
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

export const ADMIN = { id: '11111111-1111-1111-1111-111111111111', email: 'said@example.com', role: 'ADMIN' }
export const USER = { id: '22222222-2222-2222-2222-222222222222', email: 'enfant@example.com', role: 'USER' }

export function goTo(path: string) {
  window.history.pushState({}, '', path)
}
