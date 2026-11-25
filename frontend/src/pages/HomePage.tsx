import { Car, ChevronRight, Sparkles, Users, type LucideIcon } from 'lucide-react'
import { Link } from 'react-router'
import { useAuth } from '@/auth/AuthContext'
import { IconTile } from '@/components/ListCard'
import { SectionLabel } from '@/components/PageHeader'
import { fr } from '@/i18n/fr'

/** Accueil provisoire, remplacé par « Mes véhicules » avec l'histoire 4. */
export function HomePage() {
  const { state } = useAuth()
  const member = state.status === 'authenticated' ? state.member : null
  const firstName = member?.displayName?.split(' ')[0]

  return (
    <>
      <section className="flex flex-col gap-3 rounded-2xl bg-primary p-6 text-primary-foreground shadow-primary">
        <p className="text-sm font-medium opacity-85">{fr.home.today(new Date())}</p>
        <h1 className="text-2xl font-bold sm:text-3xl">{firstName ? fr.home.greeting(firstName) : fr.home.title}</h1>
        <p className="max-w-prose text-sm opacity-90">{fr.home.body}</p>
        <p className="mt-1 inline-flex items-center gap-2 self-start rounded-full bg-primary-foreground/15 px-3 py-1.5 text-xs font-semibold">
          <Sparkles aria-hidden className="size-4" />
          {fr.home.comingSoon}
        </p>
      </section>
      {member?.role === 'ADMIN' && (
        <section className="flex flex-col gap-3">
          <SectionLabel>{fr.home.shortcuts}</SectionLabel>
          <div className="grid gap-3 sm:grid-cols-2">
            <Shortcut to="/admin/parc" icon={Car} title={fr.fleet.title} description={fr.home.fleetShortcut} />
            <Shortcut to="/admin/membres" icon={Users} title={fr.members.title} description={fr.home.membersShortcut} />
          </div>
        </section>
      )}
    </>
  )
}

function Shortcut({ to, icon: Icon, title, description }: { to: string; icon: LucideIcon; title: string; description: string }) {
  return (
    <Link
      to={to}
      className="group flex items-center gap-4 rounded-2xl border border-border/60 bg-card p-4 shadow-card transition-transform duration-150 outline-none hover:-translate-y-0.5 focus-visible:ring-[3px] focus-visible:ring-ring/50"
    >
      <IconTile>
        <Icon />
      </IconTile>
      <span className="min-w-0 flex-1">
        <span className="block font-semibold">{title}</span>
        <span className="block text-sm text-muted-foreground">{description}</span>
      </span>
      <ChevronRight aria-hidden className="size-5 text-muted-foreground transition-transform group-hover:translate-x-0.5" />
    </Link>
  )
}
