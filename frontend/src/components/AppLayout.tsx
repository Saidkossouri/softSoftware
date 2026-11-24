import { Outlet } from 'react-router'
import { useAuth } from '@/auth/AuthContext'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { fr } from '@/i18n/fr'

/** Mise en page mobile-first : en-tête avec le membre connecté (FR-014) et déconnexion (FR-008). */
export function AppLayout() {
  const { state, logout } = useAuth()
  const member = state.status === 'authenticated' ? state.member : null

  return (
    <div className="min-h-svh">
      <header className="border-b bg-card">
        <div className="mx-auto flex max-w-3xl flex-wrap items-center justify-between gap-3 px-4 py-3">
          <span className="font-semibold">{fr.appName}</span>
          {member && (
            <div className="flex min-w-0 items-center gap-3">
              <div className="min-w-0 text-right">
                <p className="truncate text-sm font-medium">{member.displayName ?? member.email}</p>
                <p className="truncate text-xs text-muted-foreground">{member.email}</p>
              </div>
              <Badge>{fr.roles[member.role]}</Badge>
              <Button variant="outline" size="sm" onClick={() => void logout()}>
                {fr.logout}
              </Button>
            </div>
          )}
        </div>
      </header>
      <main className="mx-auto max-w-3xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
