import { CarFront, Gauge, Paperclip, Pencil, Plus, Wrench } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { Link, useParams } from 'react-router'
import { ApiError, type HistoryEntry, type Vehicle, type VehicleHistory } from '@/api/client'
import { useVehicleHistory } from '@/api/history'
import { useMaintenanceTypes } from '@/api/maintenance'
import { useVehicle } from '@/api/vehicles'
import { useAuth } from '@/auth/AuthContext'
import { errorMessage } from '@/components/FormErrors'
import { IconTile, ListCard, ListSkeleton } from '@/components/ListCard'
import { MileageReadingDialog } from '@/components/MileageReadingDialog'
import { BackLink, SectionLabel } from '@/components/PageHeader'
import { Plate } from '@/components/Plate'
import { Alert } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button, buttonVariants } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { EmptyState } from '@/components/ui/empty-state'
import { SegmentedTabs } from '@/components/ui/segmented-tabs'
import { Select } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { fr } from '@/i18n/fr'
import { formatDate, formatKm, formatMoney } from '@/lib/format'

/**
 * Fiche d'un véhicule : kilométrage actuel, dépenses, historique filtrable (US3, FR-014 à FR-016)
 * et informations (FR-021, FR-022) ; « Accès refusé » si le serveur refuse.
 */
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
                <>
                  <Button className="w-full sm:w-auto" onClick={() => setRecording(true)}>
                    <Gauge aria-hidden />
                    {fr.mileage.record}
                  </Button>
                  <Link
                    to={`/vehicules/${vehicle.data.id}/entretiens/nouveau`}
                    className={buttonVariants({ variant: 'outline', className: 'w-full sm:w-auto' })}
                  >
                    <Plus aria-hidden />
                    {fr.maintenance.add}
                  </Link>
                </>
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

          <HistorySection vehicle={vehicle.data} />

          <Card>
            <CardContent className="flex flex-col gap-4 pt-5 sm:pt-6">
              <SectionLabel>{fr.vehicle.sectionDetails}</SectionLabel>
              <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <Detail label={fr.vehicle.registration}>
                  <Plate registration={vehicle.data.registration} />
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

type View = 'ALL' | 'RECORDS'

/** Kilométrage actuel, totaux de dépenses et historique filtrable (US3). */
function HistorySection({ vehicle }: { vehicle: Vehicle }) {
  const [view, setView] = useState<View>('ALL')
  const [typeId, setTypeId] = useState('')
  const filtered = view === 'RECORDS' || typeId !== ''
  const history = useVehicleHistory(vehicle.id, { typeId: typeId || undefined, recordsOnly: view === 'RECORDS' })
  const types = useMaintenanceTypes('MAINTENANCE')

  if (history.isPending) {
    return (
      <div className="flex flex-col gap-4" aria-busy="true">
        <p className="sr-only">{fr.loading}</p>
        <Skeleton className="h-36" />
        <ListSkeleton />
      </div>
    )
  }
  if (history.isError) {
    return (
      <Alert variant="destructive">
        {errorMessage(history.error)}
        <Button variant="outline" size="sm" onClick={() => void history.refetch()}>
          {fr.retry}
        </Button>
      </Alert>
    )
  }

  const data = history.data
  const resetFilters = () => {
    setView('ALL')
    setTypeId('')
  }

  return (
    <>
      <Summary history={data} />
      <section className="flex flex-col gap-3" aria-labelledby="history-title">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <SectionLabel id="history-title">{fr.history.title}</SectionLabel>
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
            <SegmentedTabs<View>
              label={fr.history.filterLabel}
              value={view}
              onChange={setView}
              options={[
                { value: 'ALL', label: fr.history.all },
                { value: 'RECORDS', label: fr.history.recordsOnly },
              ]}
            />
            <Select
              aria-label={fr.history.typeFilter}
              value={typeId}
              onChange={(event) => setTypeId(event.target.value)}
              className="sm:w-56"
            >
              <option value="">{fr.history.allTypes}</option>
              {(types.data ?? []).map((type) => (
                <option key={type.id} value={type.id}>
                  {type.name}
                </option>
              ))}
            </Select>
          </div>
        </div>
        {data.entries.length === 0 ? (
          <Card>
            {filtered ? (
              <EmptyState
                illustration="garage"
                title={fr.history.emptyFilteredTitle}
                description={fr.history.emptyFilteredBody}
                action={
                  <Button variant="outline" className="w-full sm:w-auto" onClick={resetFilters}>
                    {fr.history.showAll}
                  </Button>
                }
              />
            ) : (
              <EmptyState illustration="garage" title={fr.history.emptyTitle} description={fr.history.emptyBody} />
            )}
          </Card>
        ) : (
          <ul className="flex flex-col gap-3" aria-busy={history.isFetching}>
            {data.entries.map((entry) => (
              <HistoryItem key={`${entry.kind}-${entry.id}`} entry={entry} />
            ))}
          </ul>
        )}
      </section>
    </>
  )
}

/** Carte héros (kilométrage actuel) et tuiles de dépenses de l'année et depuis le début. */
function Summary({ history }: { history: VehicleHistory }) {
  return (
    <div className="grid grid-cols-2 gap-4">
      <Card className="col-span-2 bg-primary text-primary-foreground shadow-primary">
        <CardContent className="flex flex-col gap-1 pt-5 sm:pt-6">
          <p className="text-sm opacity-90">{fr.history.currentMileage}</p>
          <p className="font-display text-3xl font-bold tabular-nums">{formatKm(history.currentMileage.mileageKm)}</p>
          <p className="text-sm opacity-90">{fr.history.since(formatDate(history.currentMileage.date))}</p>
        </CardContent>
      </Card>
      <Total label={fr.history.spentYear(history.costTotals.year)} cents={history.costTotals.currentYearCents} />
      <Total label={fr.history.spentAll} cents={history.costTotals.allTimeCents} />
    </div>
  )
}

function Total({ label, cents }: { label: string; cents: number }) {
  return (
    <Card>
      <CardContent className="flex min-w-0 flex-col gap-1 pt-5 sm:pt-6">
        <p className="truncate text-sm text-muted-foreground">{label}</p>
        <p className="font-display text-xl font-semibold tabular-nums">{formatMoney(cents)}</p>
      </CardContent>
    </Card>
  )
}

function HistoryItem({ entry }: { entry: HistoryEntry }) {
  const line = fr.history.entryLine(formatDate(entry.date), formatKm(entry.mileageKm))
  if (entry.kind === 'READING') {
    return (
      <ListCard
        leading={
          <IconTile className="bg-muted text-muted-foreground">
            <Gauge />
          </IconTile>
        }
        title={<span className="truncate">{fr.history.reading(formatKm(entry.mileageKm))}</span>}
        subtitle={[formatDate(entry.date), entry.note, fr.history.by(entry.authorName)].filter(Boolean).join(' · ')}
      />
    )
  }
  const title = (entry.types ?? []).map((type) => type.name).join(', ')
  return (
    <ListCard
      className="relative transition-shadow duration-150 focus-within:ring-[3px] focus-within:ring-ring/50 hover:shadow-primary"
      leading={
        <IconTile>
          <Wrench />
        </IconTile>
      }
      title={
        <Link
          to={`/entretiens/${entry.id}`}
          className="truncate outline-none after:absolute after:inset-0 after:rounded-2xl"
        >
          {title}
        </Link>
      }
      subtitle={line}
      meta={
        entry.costCents != null || entry.garage || (entry.documentCount ?? 0) > 0 ? (
          <>
            {entry.costCents != null && (
              <span className="text-sm font-semibold tabular-nums sm:hidden">{formatMoney(entry.costCents)}</span>
            )}
            {entry.garage && <span className="min-w-0 truncate text-xs text-muted-foreground">{entry.garage}</span>}
            {(entry.documentCount ?? 0) > 0 && (
              <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
                <Paperclip aria-hidden className="size-3.5" />
                {fr.history.documents(entry.documentCount ?? 0)}
              </span>
            )}
          </>
        ) : undefined
      }
      trailing={
        entry.costCents != null ? (
          <span className="hidden font-semibold tabular-nums sm:inline">{formatMoney(entry.costCents)}</span>
        ) : undefined
      }
    />
  )
}
