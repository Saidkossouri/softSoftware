import { CarFront, Gauge, Pencil, Plus, Wrench } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { Link, useParams } from 'react-router'
import { ApiError } from '@/api/client'
import { useVehicle } from '@/api/vehicles'
import { useAuth } from '@/auth/AuthContext'
import { errorMessage } from '@/components/FormErrors'
import { IconTile } from '@/components/ListCard'
import { MileageReadingDialog } from '@/components/MileageReadingDialog'
import { BackLink, SectionLabel } from '@/components/PageHeader'
import { Plate } from '@/components/Plate'
import { Alert } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button, buttonVariants } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { EmptyState } from '@/components/ui/empty-state'
import { Skeleton } from '@/components/ui/skeleton'
import { fr } from '@/i18n/fr'
import { formatDate, formatKm } from '@/lib/format'

/** Fiche d'un véhicule (US4, FR-021, FR-022) ; « Accès refusé » si le serveur refuse. */
export function VehicleDetailPage() {
  const { vehicleId = '' } = useParams()
  const vehicle = useVehicle(vehicleId)
  const { state } = useAuth()
  const isAdmin = state.status === 'authenticated' && state.member.role === 'ADMIN'
  const forbidden = vehicle.error instanceof ApiError && vehicle.error.code === 'VEHICLE_FORBIDDEN'
  const [recording, setRecording] = useState(false)

  return (
    <>
      <BackLink to="/">{fr.vehicle.back}</BackLink>
      {vehicle.isPending && (
        <div className="flex flex-col gap-4" aria-busy="true">
          <p className="sr-only">{fr.loading}</p>
          <Skeleton className="h-16" />
          <Skeleton className="h-56" />
        </div>
      )}
      {forbidden && (
        <Card>
          <EmptyState
            illustration="locked"
            title={fr.vehicle.forbiddenTitle}
            description={fr.vehicle.forbiddenBody}
            action={
              <Link to="/" className={buttonVariants({ variant: 'outline', className: 'w-full sm:w-auto' })}>
                {fr.vehicle.back}
              </Link>
            }
          />
        </Card>
      )}
      {vehicle.isError && !forbidden && (
        <Alert variant="destructive">
          {errorMessage(vehicle.error)}
          <Button variant="outline" size="sm" onClick={() => void vehicle.refetch()}>
            {fr.retry}
          </Button>
        </Alert>
      )}
      {vehicle.isSuccess && (
        <>
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex min-w-0 items-center gap-4">
              <IconTile className="size-14">
                <CarFront />
              </IconTile>
              <div className="min-w-0">
                <h1 className="flex min-w-0 items-center gap-2 text-2xl font-bold">
                  <span className="truncate">{vehicle.data.nickname}</span>
                  {vehicle.data.status === 'ARCHIVED' && <Badge>{fr.fleet.archivedBadge}</Badge>}
                </h1>
                <p className="truncate text-sm text-muted-foreground">
                  {fr.fleet.makeModel(vehicle.data.make, vehicle.data.model)}
                </p>
              </div>
            </div>
            <div className="flex flex-col gap-2 sm:flex-row">
              {vehicle.data.status === 'ACTIVE' && (
                <Button className="w-full sm:w-auto" onClick={() => setRecording(true)}>
                  <Gauge aria-hidden />
                  {fr.mileage.record}
                </Button>
              )}
              {isAdmin && (
                <Link
                  to={`/admin/parc/${vehicle.data.id}`}
                  className={buttonVariants({ variant: 'outline', className: 'w-full sm:w-auto' })}
                >
                  <Pencil aria-hidden />
                  {fr.vehicle.edit}
                </Link>
              )}
            </div>
          </div>
          {recording && <MileageReadingDialog vehicle={vehicle.data} onClose={() => setRecording(false)} />}

          <Card>
            <CardContent className="flex flex-col gap-4 pt-5 sm:pt-6">
              <SectionLabel>{fr.vehicle.sectionDetails}</SectionLabel>
              <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <Detail label={fr.vehicle.registration}>
                  <Plate registration={vehicle.data.registration} />
                </Detail>
                <Detail label={fr.vehicle.currentMileage}>
                  <span className="tabular-nums">
                    {fr.fleet.currentMileage(
                      formatKm(vehicle.data.currentMileage.mileageKm),
                      formatDate(vehicle.data.currentMileage.date),
                    )}
                  </span>
                </Detail>
                <Detail label={fr.vehicle.mileage}>
                  <span className="tabular-nums">{fr.fleet.mileage(vehicle.data.initialMileageKm)}</span>
                </Detail>
                <Detail label={fr.vehicle.year}>
                  {vehicle.data.firstRegistrationYear ?? <NotSet />}
                </Detail>
                <Detail label={fr.vehicle.energy}>
                  {vehicle.data.energyType ? fr.energyTypes[vehicle.data.energyType] : <NotSet />}
                </Detail>
                <Detail label={fr.vehicle.vin}>
                  {vehicle.data.vin ? <span className="font-mono text-sm break-all">{vehicle.data.vin}</span> : <NotSet />}
                </Detail>
              </dl>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="flex flex-col gap-4 pt-5 sm:flex-row sm:items-center sm:pt-6">
              <div className="flex min-w-0 flex-1 items-center gap-4">
                <IconTile className="bg-muted text-muted-foreground">
                  <Wrench />
                </IconTile>
                <p className="text-sm text-muted-foreground">{fr.vehicle.maintenanceSoon}</p>
              </div>
              {vehicle.data.status === 'ACTIVE' && (
                <Link
                  to={`/vehicules/${vehicle.data.id}/entretiens/nouveau`}
                  className={buttonVariants({ variant: 'outline', className: 'w-full sm:w-auto' })}
                >
                  <Plus aria-hidden />
                  {fr.maintenance.add}
                </Link>
              )}
            </CardContent>
          </Card>
        </>
      )}
    </>
  )
}

function Detail({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex min-w-0 flex-col gap-1">
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="font-medium">{children}</dd>
    </div>
  )
}

function NotSet() {
  return <span className="text-muted-foreground">{fr.vehicle.notSet}</span>
}
