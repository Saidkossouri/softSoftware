import { Plus } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import type { Member, Vehicle } from '@/api/client'
import { useMemberVehicleAccess, useReplaceMemberVehicleAccess } from '@/api/members'
import { useVehicles } from '@/api/vehicles'
import { errorMessage } from '@/components/FormErrors'
import { Plate } from '@/components/Plate'
import { Alert } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button, buttonVariants } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { EmptyState } from '@/components/ui/empty-state'
import { Skeleton } from '@/components/ui/skeleton'
import { useToast } from '@/components/ui/toast'
import { fr } from '@/i18n/fr'

/**
 * Section « Véhicules accessibles » d'un membre USER (US4, FR-019, FR-020) : cases à cocher des
 * véhicules en service, véhicules archivés déjà attribués signalés, enregistrement en un seul PUT.
 */
export function VehicleAccessCard({ member }: { member: Member }) {
  const access = useMemberVehicleAccess(member.id)
  const active = useVehicles('ACTIVE')
  const archived = useVehicles('ARCHIVED')
  const queries = [access, active, archived]

  return (
    <Card>
      <CardHeader>
        <CardTitle>{fr.access.title}</CardTitle>
        <CardDescription>{fr.access.description}</CardDescription>
      </CardHeader>
      <CardContent>
        {queries.some((query) => query.isPending) && !queries.some((query) => query.isError) && (
          <div className="flex flex-col gap-2" aria-busy="true">
            <p className="sr-only">{fr.loading}</p>
            <Skeleton className="h-14" />
            <Skeleton className="h-14" />
          </div>
        )}
        {queries.some((query) => query.isError) && (
          <Alert variant="destructive">
            {errorMessage(queries.find((query) => query.isError)?.error)}
            <Button variant="outline" size="sm" onClick={() => queries.forEach((query) => void query.refetch())}>
              {fr.retry}
            </Button>
          </Alert>
        )}
        {access.isSuccess && active.isSuccess && archived.isSuccess && (
          <AccessForm
            memberId={member.id}
            initial={access.data.vehicleIds}
            active={active.data}
            archived={archived.data.filter((vehicle) => access.data.vehicleIds.includes(vehicle.id))}
          />
        )}
      </CardContent>
    </Card>
  )
}

function AccessForm({
  memberId,
  initial,
  active,
  archived,
}: {
  memberId: string
  initial: string[]
  active: Vehicle[]
  archived: Vehicle[]
}) {
  const [selected, setSelected] = useState(() => new Set(initial))
  const replace = useReplaceMemberVehicleAccess(memberId)
  const toast = useToast()
  const vehicles = [...active, ...archived]

  const toggle = (vehicleId: string, checked: boolean) => {
    setSelected((current) => {
      const next = new Set(current)
      if (checked) next.add(vehicleId)
      else next.delete(vehicleId)
      return next
    })
  }

  const save = async () => {
    try {
      // Seuls les véhicules affichés sont envoyés : un véhicule disparu entre-temps est ignoré.
      const vehicleIds = vehicles.map((vehicle) => vehicle.id).filter((id) => selected.has(id))
      await replace.mutateAsync({ vehicleIds })
      toast.success(fr.access.saved)
    } catch {
      // l'erreur est affichée via replace.error
    }
  }

  if (vehicles.length === 0) {
    return (
      <EmptyState
        illustration="garage"
        title={fr.access.empty}
        description={fr.access.emptyHint}
        className="py-4"
        action={
          <Link to="/admin/parc/nouveau" className={buttonVariants({ variant: 'outline', className: 'w-full sm:w-auto' })}>
            <Plus aria-hidden />
            {fr.access.addVehicle}
          </Link>
        }
      />
    )
  }

  const count = vehicles.filter((vehicle) => selected.has(vehicle.id)).length

  return (
    <div className="flex flex-col gap-4">
      <ul className="flex flex-col gap-2" aria-label={fr.access.listLabel}>
        {vehicles.map((vehicle) => {
          const id = `access-${vehicle.id}`
          const isArchived = vehicle.status === 'ARCHIVED'
          return (
            <li key={vehicle.id}>
              <label
                htmlFor={id}
                className="flex min-h-14 cursor-pointer items-center gap-3 rounded-xl border border-border/60 px-3 py-2.5 transition-colors duration-150 hover:bg-secondary has-[:checked]:border-primary/40 has-[:checked]:bg-accent"
              >
                <Checkbox
                  id={id}
                  checked={selected.has(vehicle.id)}
                  onChange={(event) => toggle(vehicle.id, event.target.checked)}
                />
                <span className="flex min-w-0 flex-1 flex-col gap-1">
                  <span className="flex min-w-0 items-center gap-2 font-semibold">
                    <span className="truncate">{vehicle.nickname}</span>
                    {isArchived && <Badge>{fr.fleet.archivedBadge}</Badge>}
                  </span>
                  {isArchived ? (
                    <span className="text-sm text-muted-foreground">{fr.access.archivedNotice}</span>
                  ) : (
                    <span className="truncate text-sm text-muted-foreground">
                      {fr.fleet.makeModel(vehicle.make, vehicle.model)}
                    </span>
                  )}
                </span>
                <Plate registration={vehicle.registration} className="hidden sm:inline-flex" />
              </label>
            </li>
          )
        })}
      </ul>
      {replace.isError && <Alert variant="destructive">{errorMessage(replace.error)}</Alert>}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-sm text-muted-foreground" aria-live="polite">
          {fr.access.count(count)}
        </p>
        <Button className="w-full sm:w-auto" disabled={replace.isPending} onClick={() => void save()}>
          {replace.isPending ? fr.access.saving : fr.access.save}
        </Button>
      </div>
    </div>
  )
}
