import { Outlet } from 'react-router'
import { ForbiddenPage } from '@/pages/ForbiddenPage'
import { useAuth } from './AuthContext'

/** Masque les pages d'administration à un USER. Confort seulement : le serveur refuse aussi (403). */
export function RequireAdmin() {
  const { state } = useAuth()
  if (state.status === 'authenticated' && state.member.role === 'ADMIN') {
    return <Outlet />
  }
  return <ForbiddenPage />
}
