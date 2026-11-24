import { NavLink, Outlet } from 'react-router'
import { useAuth } from '@/auth/AuthContext'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { fr } from '@/i18n/fr'
import { cn } from '@/lib/utils'

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
        {member && (
          <nav aria-label="Navigation principale" className="mx-auto flex max-w-3xl gap-1 overflow-x-auto px-4 pb-2">
            <NavItem to="/" end>
              {fr.nav.home}
            </NavItem>
            {member.role === 'ADMIN' && (
              <>
                <NavItem to="/admin/parc">{fr.nav.fleet}</NavItem>
                <NavItem to="/admin/membres">{fr.nav.members}</NavItem>
              </>
            )}
          </nav>
        )}
      </header>
      <main className="mx-auto max-w-3xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}

function NavItem({ to, end, children }: { to: string; end?: boolean; children: string }) {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) =>
        cn(
          'inline-flex h-10 shrink-0 items-center rounded-md px-3 text-sm font-medium hover:bg-secondary',
          isActive ? 'bg-secondary text-foreground' : 'text-muted-foreground',
        )
      }
    >
      {children}
    </NavLink>
  )
}
