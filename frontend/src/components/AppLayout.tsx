import { Car, House, LogOut, Users, type LucideIcon } from 'lucide-react'
import { NavLink, Outlet } from 'react-router'
import { useAuth } from '@/auth/AuthContext'
import { BrandMark } from '@/components/BrandMark'
import { Avatar } from '@/components/ListCard'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { fr } from '@/i18n/fr'
import { cn } from '@/lib/utils'

/**
 * Mise en page mobile-first (charte v2) : en-tête avec le membre connecté (FR-014) et
 * déconnexion (FR-008) ; navigation en barre fixe en bas sur mobile, dans l'en-tête sur desktop.
 */
export function AppLayout() {
  const { state, logout } = useAuth()
  const member = state.status === 'authenticated' ? state.member : null

  return (
    <div className="min-h-svh">
      <header className="sticky top-0 z-40 border-b border-border/60 bg-card">
        <div className="mx-auto flex h-16 max-w-4xl items-center justify-between gap-3 px-4">
          <BrandMark className="shrink-0" />
          {member && (
            <nav
              aria-label={fr.nav.label}
              className="fixed inset-x-0 bottom-0 z-40 flex justify-around border-t border-border/60 bg-card px-2 pt-1.5 pb-[max(0.5rem,env(safe-area-inset-bottom))] sm:static sm:mr-auto sm:ml-6 sm:justify-start sm:gap-1 sm:border-0 sm:bg-transparent sm:p-0"
            >
              <NavItem to="/" end icon={House}>
                {fr.nav.home}
              </NavItem>
              {member.role === 'ADMIN' && (
                <>
                  <NavItem to="/admin/parc" icon={Car}>
                    {fr.nav.fleet}
                  </NavItem>
                  <NavItem to="/admin/membres" icon={Users}>
                    {fr.nav.members}
                  </NavItem>
                </>
              )}
            </nav>
          )}
          {member && (
            <div className="flex min-w-0 items-center gap-3">
              <div className="min-w-0 text-right">
                <p className="truncate text-sm font-semibold">{member.displayName ?? member.email}</p>
                <div className="flex items-center justify-end gap-2">
                  <p className="hidden truncate text-xs text-muted-foreground md:block">{member.email}</p>
                  <Badge variant="primary" className="px-2 py-0.5">
                    {fr.roles[member.role]}
                  </Badge>
                </div>
              </div>
              <Avatar name={member.displayName ?? member.email} className="hidden size-10 sm:flex" />
              <Button variant="ghost" size="icon" aria-label={fr.logout} title={fr.logout} onClick={() => void logout()}>
                <LogOut aria-hidden />
              </Button>
            </div>
          )}
        </div>
      </header>
      <main className="animate-in mx-auto flex max-w-4xl flex-col gap-5 px-4 pt-6 pb-28 sm:pb-10">
        <Outlet />
      </main>
    </div>
  )
}

function NavItem({ to, end, icon: Icon, children }: { to: string; end?: boolean; icon: LucideIcon; children: string }) {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) =>
        cn(
          'flex min-w-20 flex-col items-center gap-1 rounded-xl px-3 py-1.5 text-xs font-medium transition-colors outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50 sm:h-10 sm:min-w-0 sm:flex-row sm:gap-2 sm:text-sm',
          isActive
            ? 'text-primary sm:bg-accent sm:text-accent-foreground'
            : 'text-muted-foreground hover:text-foreground sm:hover:bg-secondary',
        )
      }
    >
      <Icon aria-hidden className="size-6 sm:size-4" />
      {children}
    </NavLink>
  )
}
