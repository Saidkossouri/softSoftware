import { Car, CarFront, ChevronRight, Gauge, Plus, Sparkles, Users, type LucideIcon } from 'lucide-react'
import { Link } from 'react-router'
import type { Vehicle } from '@/api/client'
import { useVehicles } from '@/api/vehicles'
import { useAuth } from '@/auth/AuthContext'
import { errorMessage } from '@/components/FormErrors'
import { IconTile, ListSkeleton } from '@/components/ListCard'
import { SectionLabel } from '@/components/PageHeader'
import { Plate } from '@/components/Plate'
import { Alert } from '@/components/ui/alert'
import { Button, buttonVariants } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { EmptyState } from '@/components/ui/empty-state'
import { fr } from '@/i18n/fr'

/**
 * Accueil (US4, FR-021) : « Mes véhicules » pour un USER (ses véhicules actifs attribués,
 * filtrés par le serveur), véhicules en service pour un ADMIN, plus ses raccourcis.
 */
export function VehiclesPage() {
  const { state } = useAuth()
  const member = state.status === 'authenticated' ? state.member : null
  const isAdmin = member?.role === 'ADMIN'
  const firstName = member?.displayName?.split(' ')[0]
  const vehicles = useVehicles('ACTIVE')

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

      <section className="flex flex-col gap-3">
        <SectionLabel>{isAdmin ? fr.home.fleetVehicles : fr.home.myVehicles}</SectionLabel>
        {vehicles.isPending && <ListSkeleton />}
        {vehicles.isError && (
          <Alert variant="destructive">
            {errorMessage(vehicles.error)}
            <Button variant="outline" size="sm" onClick={() => void vehicles.refetch()}>
              {fr.retry}
            </Button>
          </Alert>
        )}
        {vehicles.isSuccess && vehicles.data.length === 0 && (
          <Card>
            {isAdmin ? (
              <EmptyState
                illustration="garage"
                title={fr.home.emptyAdmin}
                description={fr.home.emptyAdminHint}
                action={
                  <Link to="/admin/parc/nouveau" className={buttonVariants({ className: 'w-full sm:w-auto' })}>
                    <Plus aria-hidden />
                    {fr.fleet.add}
                  </Link>
                }
              />
            ) : (
              <EmptyState illustration="garage" title={fr.home.emptyUser} description={fr.home.emptyUserHint} />
            )}
          </Card>
        )}
        {vehicles.isSuccess && vehicles.data.length > 0 && (
          <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2" aria-label={isAdmin ? fr.home.fleetVehicles : fr.home.myVehicles}>
            {vehicles.data.map((vehicle) => (
              <li key={vehicle.id}>
                <VehicleTile vehicle={vehicle} />
              </li>
            ))}
          </ul>
        )}
      </section>

      {isAdmin && (
        <section className="flex flex-col gap-3">
          <SectionLabel>{fr.home.shortcuts}</SectionLabel>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <Shortcut to="/admin/parc" icon={Car} title={fr.fleet.title} description={fr.home.fleetShortcut} />
            <Shortcut to="/admin/membres" icon={Users} title={fr.members.title} description={fr.home.membersShortcut} />
          </div>
        </section>
      )}
    </>
  )
}

const tileClass =
  'group flex h-full items-center gap-4 rounded-2xl border border-border/60 bg-card p-4 shadow-card transition-transform duration-150 outline-none hover:-translate-y-0.5 focus-visible:ring-[3px] focus-visible:ring-ring/50 motion-reduce:transition-none motion-reduce:hover:translate-y-0'

function VehicleTile({ vehicle }: { vehicle: Vehicle }) {
  return (
    <Link to={`/vehicules/${vehicle.id}`} className={tileClass} aria-label={fr.home.open(vehicle.nickname)}>
      <IconTile>
        <CarFront />
      </IconTile>
      <span className="flex min-w-0 flex-1 flex-col gap-1.5">
        <span className="truncate font-semibold">{vehicle.nickname}</span>
        <span className="truncate text-sm text-muted-foreground">{fr.fleet.makeModel(vehicle.make, vehicle.model)}</span>
        <span className="flex flex-wrap items-center gap-2">
          <Plate registration={vehicle.registration} />
          <span className="inline-flex items-center gap-1 text-xs text-muted-foreground tabular-nums">
            <Gauge aria-hidden className="size-4" />
            {fr.fleet.mileage(vehicle.initialMileageKm)}
          </span>
        </span>
      </span>
      <ChevronRight aria-hidden className="size-5 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-0.5" />
    </Link>
  )
}

function Shortcut({ to, icon: Icon, title, description }: { to: string; icon: LucideIcon; title: string; description: string }) {
  return (
    <Link to={to} className={tileClass}>
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
