import createClient, { type Middleware } from 'openapi-fetch'
import type { components, paths } from './generated/schema'

export type Problem = components['schemas']['Problem']
export type CurrentMember = components['schemas']['CurrentMember']
export type Role = components['schemas']['Role']
export type Member = components['schemas']['Member']
export type MemberCreateRequest = components['schemas']['MemberCreateRequest']
export type MemberUpdateRequest = components['schemas']['MemberUpdateRequest']
export type FieldError = components['schemas']['FieldError']
export type Vehicle = components['schemas']['Vehicle']
export type VehicleInput = components['schemas']['VehicleInput']
export type VehicleStatus = components['schemas']['VehicleStatus']
export type EnergyType = components['schemas']['EnergyType']

/** Erreur de l'API au format Problem Details, avec le code stable du contrat. */
export class ApiError extends Error {
  readonly status: number
  readonly problem: Problem

  constructor(status: number, problem: Problem) {
    super(problem.detail)
    this.status = status
    this.problem = problem
  }

  get code() {
    return this.problem.code
  }
}

const UNSAFE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

/** Valeur du cookie CSRF posé par Spring Security (lisible par le JavaScript par conception). */
export function readCsrfToken(): string | undefined {
  return document.cookie
    .split('; ')
    .find((cookie) => cookie.startsWith('XSRF-TOKEN='))
    ?.slice('XSRF-TOKEN='.length)
}

const csrfMiddleware: Middleware = {
  onRequest({ request }) {
    const token = readCsrfToken()
    if (token && UNSAFE_METHODS.has(request.method)) {
      request.headers.set('X-XSRF-TOKEN', decodeURIComponent(token))
    }
    return request
  },
}

export const api = createClient<paths>({
  baseUrl: `${window.location.origin}/api`,
  credentials: 'same-origin',
  // Résolution tardive de fetch (remplaçable dans les tests).
  fetch: (request) => globalThis.fetch(request),
})
api.use(csrfMiddleware)

/** Transforme une réponse openapi-fetch en données ou en {@link ApiError}. */
export function unwrap<T>(result: { data?: T; error?: unknown; response: Response }): T {
  if (result.error !== undefined || !result.response.ok) {
    const problem: Problem = (result.error as Problem | undefined) ?? {
      type: 'about:blank',
      status: result.response.status,
      code: 'INTERNAL_ERROR',
      detail: 'Une erreur inattendue est survenue. Réessayez plus tard.',
    }
    throw new ApiError(result.response.status, problem)
  }
  return result.data as T
}

/** Déconnexion : point d'entrée de Spring Security, hors contrat REST. */
export async function logout(): Promise<void> {
  const token = readCsrfToken()
  await fetch('/logout', {
    method: 'POST',
    credentials: 'same-origin',
    headers: token ? { 'X-XSRF-TOKEN': decodeURIComponent(token) } : {},
  })
}

export const GOOGLE_LOGIN_URL = '/oauth2/authorization/google'
