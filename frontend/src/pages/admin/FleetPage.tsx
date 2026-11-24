import { useState } from 'react'
import { Link } from 'react-router'
import type { Vehicle, VehicleStatus } from '@/api/client'
import { useReactivateVehicle, useVehicles } from '@/api/vehicles'
import { errorMessage } from '@/components/FormErrors'
import { Alert } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button, buttonVariants } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { fr } from '@/i18n/fr'
import { cn } from '@/lib/utils'

/** Parc de véhicules : en service et archivés, réactivation (US3, FR-015 à FR-018). */
export function FleetPage() {
  const [status, setStatus] = useState<VehicleStatus>('ACTIVE')
  const vehicles = useVehicles(status)
  const reactivate = useReactivateVehicle()
  const [reactivated, setReactivated] = useState<string | null>(null)

  const onReactivate = async (vehicle: Vehicle) => {
    setReactivated(null)
    try {
      await reactivate.mutateAsync(vehicle.id)
      setReactivated(fr.fleet.reactivated(vehicle.nickname))
    } catch {
      // l'erreur est affichée via reactivate.error
    }
  }

  const changeTab = (next: VehicleStatus) => {
    setStatus(next)
    setReactivated(null)
    reactivate.reset()
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{fr.fleet.title}</CardTitle>
        <CardDescription>{fr.fleet.description}</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div role="tablist" aria-label={fr.fleet.tabsLabel} className="flex gap-1">
            <Tab selected={status === 'ACTIVE'} onSelect={() => changeTab('ACTIVE')}>
              {fr.fleet.activeTab}
            </Tab>
            <Tab selected={status === 'ARCHIVED'} onSelect={() => changeTab('ARCHIVED')}>
              {fr.fleet.archivedTab}
            </Tab>
          </div>
          <Link to="/admin/parc/nouveau" className={buttonVariants({ className: 'w-full sm:w-auto' })}>
            {fr.fleet.add}
          </Link>
        </div>

        {reactivated && <Alert role="status">{reactivated}</Alert>}
        {reactivate.isError && <Alert variant="destructive">{errorMessage(reactivate.error)}</Alert>}

        {vehicles.isPending && <p className="text-muted-foreground">{fr.loading}</p>}
        {vehicles.isError && (
          <Alert variant="destructive" className="flex flex-col items-start gap-3">
            {errorMessage(vehicles.error)}
            <Button variant="outline" size="sm" onClick={() => void vehicles.refetch()}>
              {fr.retry}
            </Button>
          </Alert>
        )}
        {vehicles.isSuccess && vehicles.data.length === 0 && (
          <p className="text-muted-foreground">
            {status === 'ACTIVE' ? fr.fleet.emptyActive : fr.fleet.emptyArchived}
          </p>
        )}
        {vehicles.isSuccess && vehicles.data.length > 0 && (
          <ul className="flex flex-col divide-y" aria-label={status === 'ACTIVE' ? fr.fleet.activeTab : fr.fleet.archivedTab}>
            {vehicles.data.map((vehicle) => (
              <li
                key={vehicle.id}
                className="flex flex-col gap-2 py-3 sm:flex-row sm:items-center sm:justify-between sm:gap-3"
              >
                <div className="min-w-0">
                  <div className="flex min-w-0 items-center gap-2">
                    <p className="truncate font-medium">{vehicle.nickname}</p>
                    {vehicle.status === 'ARCHIVED' && <Badge className="shrink-0">{fr.fleet.archivedBadge}</Badge>}
                  </div>
                  <p className="truncate text-sm text-muted-foreground">
                    {fr.fleet.summary(vehicle.make, vehicle.model, vehicle.registration)}
                  </p>
                </div>
                <div className="flex shrink-0 gap-2">
                  {vehicle.status === 'ARCHIVED' && (
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={reactivate.isPending}
                      aria-label={`${fr.fleet.reactivate} ${vehicle.nickname}`}
                      onClick={() => void onReactivate(vehicle)}
                    >
                      {reactivate.isPending && reactivate.variables === vehicle.id
                        ? fr.fleet.reactivating
                        : fr.fleet.reactivate}
                    </Button>
                  )}
                  <Link
                    to={`/admin/parc/${vehicle.id}`}
                    className={buttonVariants({ variant: 'ghost', size: 'sm' })}
                    aria-label={`${fr.fleet.edit} ${vehicle.nickname}`}
                  >
                    {fr.fleet.edit}
                  </Link>
                </div>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}

function Tab({ selected, onSelect, children }: { selected: boolean; onSelect: () => void; children: string }) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={selected}
      onClick={onSelect}
      className={cn(
        'inline-flex h-10 items-center rounded-md px-3 text-sm font-medium hover:bg-secondary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
        selected ? 'bg-secondary text-foreground' : 'text-muted-foreground',
      )}
    >
      {children}
    </button>
  )
}
