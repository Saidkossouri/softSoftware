import { createContext, useContext, type ReactNode } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError, logout as apiLogout, unwrap, type CurrentMember } from '@/api/client'

export type AuthState =
  | { status: 'loading' }
  | { status: 'anonymous' }
  | { status: 'revoked' }
  | { status: 'error'; retry: () => void }
  | { status: 'authenticated'; member: CurrentMember }

type AuthContextValue = { state: AuthState; logout: () => Promise<void> }

const AuthContext = createContext<AuthContextValue | null>(null)

export const CURRENT_MEMBER_KEY = ['me'] as const

/** Membre connecté, lu depuis GET /api/me : 401 = non connecté, 403 = accès retiré. */
export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const query = useQuery({
    queryKey: CURRENT_MEMBER_KEY,
    queryFn: async () => unwrap(await api.GET('/me')),
    retry: false,
    staleTime: 60_000,
  })

  let state: AuthState
  if (query.isPending) {
    state = { status: 'loading' }
  } else if (query.isSuccess) {
    state = { status: 'authenticated', member: query.data }
  } else if (query.error instanceof ApiError && query.error.status === 401) {
    state = { status: 'anonymous' }
  } else if (query.error instanceof ApiError && query.error.status === 403) {
    state = { status: 'revoked' }
  } else {
    state = { status: 'error', retry: () => void query.refetch() }
  }

  const logout = async () => {
    await apiLogout()
    queryClient.clear()
    await queryClient.invalidateQueries({ queryKey: CURRENT_MEMBER_KEY })
  }

  return <AuthContext.Provider value={{ state, logout }}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) {
    throw new Error('useAuth doit être utilisé dans un AuthProvider')
  }
  return value
}
