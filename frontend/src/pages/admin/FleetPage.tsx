import { CarFront, Fuel, Gauge, Pencil, Plus, RotateCcw } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import type { Vehicle, VehicleStatus } from '@/api/client'
import { useReactivateVehicle, useVehicles } from '@/api/vehicles'
import { errorMessage } from '@/components/FormErrors'
import { IconTile, ListCard, ListSkeleton } from '@/components/ListCard'
import { PageHeader } from '@/components/PageHeader'
import { Plate } from '@/components/Plate'
import { Alert } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button, buttonVariants } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { EmptyState } from '@/components/ui/empty-state'
import { SegmentedTabs } from '@/components/ui/segmented-tabs'
import { useToast } from '@/components/ui/toast'
import { fr } from '@/i18n/fr'
import { formatDate, formatKm } from '@/lib/format'

const TABS = [
  { value: 'ACTIVE', label: fr.fleet.activeTab },
  { value: 'ARCHIVED', label: fr.fleet.archivedTab },
] as const satisfies readonly { value: VehicleStatus; label: string }[]

/** Parc de véhicules : en service et archivés, réactivation (US3, FR-015 à FR-018). */
export function FleetPage() {
  const [status, setStatus] = useState<VehicleStatus>('ACTIVE')
  const vehicles = useVehicles(status)
  const reactivate = useReactivateVehicle()
  const toast = useToast()

  const onReactivate = async (vehicle: Vehicle) => {
    try {
      await reactivate.mutateAsync(vehicle.id)
      toast.success(fr.fleet.reactivated(vehicle.nickname))
    } catch {
      // l'erreur est affichée via reactivate.error
    }
  }

  const changeTab = (next: VehicleStatus) => {
    setStatus(next)
    reactivate.reset()
  }

  const addButton = (
    <Link to="/admin/parc/nouveau" className={buttonVariants({ className: 'w-full sm:w-auto' })}>
      <Plus aria-hidden />
      {fr.fleet.add}
    </Link>
  )

  return (
    <>
      <PageHeader title={fr.fleet.title} description={fr.fleet.description} action={addButton} />
      <SegmentedTabs label={fr.fleet.tabsLabel} value={status} options={TABS} onChange={changeTab} className="sm:max-w-xs" />

      {reactivate.isError && <Alert variant="destructive">{errorMessage(reactivate.error)}</Alert>}

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
          {status === 'ACTIVE' ? (
            <EmptyState
              illustration="garage"
              title={fr.fleet.emptyActive}
              description={fr.fleet.emptyActiveHint}
            />
          ) : (
            <EmptyState illustration="archive" title={fr.fleet.emptyArchived} description={fr.fleet.emptyArchivedHint} />
          )}
        </Card>
      )}
      {vehicles.isSuccess && vehicles.data.length > 0 && (
        <ul className="flex flex-col gap-3" aria-label={status === 'ACTIVE' ? fr.fleet.activeTab : fr.fleet.archivedTab}>
          {vehicles.data.map((vehicle) => (
            <ListCard
              key={vehicle.id}
              leading={
                <IconTile className={vehicle.status === 'ARCHIVED' ? 'bg-muted text-muted-foreground' : undefined}>
                  <CarFront />
                </IconTile>
              }
              title={
                <>
                  <span className="truncate">{vehicle.nickname}</span>
                  {vehicle.status === 'ARCHIVED' && <Badge>{fr.fleet.archivedBadge}</Badge>}
                </>
              }
              subtitle={fr.fleet.makeModel(vehicle.make, vehicle.model)}
              meta={
                <>
                  <Plate registration={vehicle.registration} />
                  <span className="inline-flex items-center gap-1 text-xs text-muted-foreground tabular-nums">
                    <Gauge aria-hidden className="size-4" />
                    {fr.fleet.currentMileage(formatKm(vehicle.currentMileage.mileageKm), formatDate(vehicle.currentMileage.date))}
                  </span>
                  {vehicle.energyType && (
                    <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
                      <Fuel aria-hidden className="size-4" />
                      {fr.energyTypes[vehicle.energyType]}
                    </span>
                  )}
                </>
              }
              trailing={
                <>
                  {vehicle.status === 'ARCHIVED' && (
                    <Button
                      variant="outline"
                      size="sm"
                      className="size-11 px-0 sm:h-9 sm:w-auto sm:px-3"
                      disabled={reactivate.isPending}
                      aria-label={`${fr.fleet.reactivate} ${vehicle.nickname}`}
                      onClick={() => void onReactivate(vehicle)}
                    >
                      <RotateCcw aria-hidden className="size-4" />
                      <span className="hidden sm:inline">
                        {reactivate.isPending && reactivate.variables === vehicle.id
                          ? fr.fleet.reactivating
                          : fr.fleet.reactivate}
                      </span>
                    </Button>
                  )}
                  <Link
                    to={`/admin/parc/${vehicle.id}`}
                    className={buttonVariants({ variant: 'ghost', size: 'sm', className: 'size-11 px-0 sm:h-9 sm:w-auto sm:px-3' })}
                    aria-label={`${fr.fleet.edit} ${vehicle.nickname}`}
                  >
                    <Pencil aria-hidden className="size-4" />
                    <span className="hidden sm:inline">{fr.fleet.edit}</span>
                  </Link>
                </>
              }
            />
          ))}
        </ul>
      )}
    </>
  )
}
