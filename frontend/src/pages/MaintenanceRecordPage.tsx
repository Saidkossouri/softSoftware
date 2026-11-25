import { Pencil, Trash2, Wrench } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ApiError } from '@/api/client'
import { useDeleteMaintenanceRecord, useMaintenanceRecord } from '@/api/maintenance'
import { errorMessage } from '@/components/FormErrors'
import { IconTile } from '@/components/ListCard'
import { BackLink, SectionLabel } from '@/components/PageHeader'
import { Alert } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button, buttonVariants } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { ConfirmDialog } from '@/components/ui/dialog'
import { EmptyState } from '@/components/ui/empty-state'
import { Skeleton } from '@/components/ui/skeleton'
import { useToast } from '@/components/ui/toast'
import { fr } from '@/i18n/fr'
import { formatDate, formatKm, formatMoney } from '@/lib/format'

/** Détail d'un entretien (US2) ; Modifier et Supprimer seulement pour l'auteur ou un ADMIN. */
export function MaintenanceRecordPage() {
  const { recordId = '' } = useParams()
  const record = useMaintenanceRecord(recordId)
  const remove = useDeleteMaintenanceRecord()
  const navigate = useNavigate()
  const toast = useToast()
  const [confirming, setConfirming] = useState(false)
  const forbidden = record.error instanceof ApiError && record.error.code === 'VEHICLE_FORBIDDEN'

  async function onDelete(vehicleId: string) {
    try {
      await remove.mutateAsync(recordId)
      toast.success(fr.maintenance.deleted)
      void navigate(`/vehicules/${vehicleId}`)
    } catch {
      // Message affiché dans le dialogue.
    }
  }

  return (
    <>
      <BackLink to={record.isSuccess ? `/vehicules/${record.data.vehicleId}` : '/'}>{fr.maintenance.back}</BackLink>
      {record.isPending && (
        <div className="flex flex-col gap-4" aria-busy="true">
          <p className="sr-only">{fr.loading}</p>
          <Skeleton className="h-16" />
          <Skeleton className="h-56" />
        </div>
      )}
      {forbidden && (
        <Card>
          <EmptyState illustration="locked" title={fr.vehicle.forbiddenTitle} description={fr.vehicle.forbiddenBody} />
        </Card>
      )}
      {record.isError && !forbidden && (
        <Alert variant="destructive">
          {errorMessage(record.error)}
          <Button variant="outline" size="sm" onClick={() => void record.refetch()}>
            {fr.retry}
          </Button>
        </Alert>
      )}
      {record.isSuccess && (
        <>
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex min-w-0 items-center gap-4">
              <IconTile className="size-14">
                <Wrench />
              </IconTile>
              <div className="min-w-0">
                <h1 className="text-2xl font-bold">{fr.maintenance.detailTitle(formatDate(record.data.recordDate))}</h1>
                <p className="truncate text-sm text-muted-foreground">{fr.maintenance.author(record.data.authorName)}</p>
              </div>
            </div>
            {record.data.canEdit && (
              <div className="flex flex-col gap-2 sm:flex-row">
                <Link
                  to={`/entretiens/${record.data.id}/modifier`}
                  className={buttonVariants({ variant: 'outline', className: 'w-full sm:w-auto' })}
                >
                  <Pencil aria-hidden />
                  {fr.maintenance.edit}
                </Link>
                <Button variant="destructive-outline" className="w-full sm:w-auto" onClick={() => setConfirming(true)}>
                  <Trash2 aria-hidden />
                  {fr.maintenance.delete}
                </Button>
              </div>
            )}
          </div>

          <Card>
            <CardContent className="flex flex-col gap-4 pt-5 sm:pt-6">
              <SectionLabel>{fr.maintenance.sectionWhat}</SectionLabel>
              <ul className="flex flex-wrap gap-2" aria-label={fr.maintenance.types}>
                {record.data.types.map((type) => (
                  <li key={type.id}>
                    <Badge variant="primary">{type.name}</Badge>
                  </li>
                ))}
              </ul>
              {record.data.description && <p>{record.data.description}</p>}
              <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <Detail label={fr.maintenance.mileage}>
                  <span className="tabular-nums">{formatKm(record.data.mileageKm)}</span>
                </Detail>
                <Detail label={fr.maintenance.costLabel}>
                  {record.data.costCents === undefined ? (
                    <NotSet />
                  ) : (
                    <span className="tabular-nums">{formatMoney(record.data.costCents)}</span>
                  )}
                </Detail>
                <Detail label={fr.maintenance.garageLabel}>{record.data.garage ?? <NotSet />}</Detail>
                <Detail label={fr.maintenance.notesLabel}>
                  {record.data.notes ? <span className="whitespace-pre-line">{record.data.notes}</span> : <NotSet />}
                </Detail>
              </dl>
            </CardContent>
          </Card>

          <ConfirmDialog
            open={confirming}
            title={fr.maintenance.deleteTitle}
            description={fr.maintenance.deleteDescription(record.data.documentCount)}
            confirmLabel={remove.isPending ? fr.maintenance.deleting : fr.maintenance.deleteConfirm}
            cancelLabel={fr.maintenance.cancel}
            pending={remove.isPending}
            error={remove.isError ? <Alert variant="destructive">{errorMessage(remove.error)}</Alert> : undefined}
            onConfirm={() => void onDelete(record.data.vehicleId)}
            onCancel={() => setConfirming(false)}
          />
        </>
      )}
    </>
  )
}

function Detail({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex min-w-0 flex-col gap-1">
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="font-medium break-words">{children}</dd>
    </div>
  )
}

function NotSet() {
  return <span className="text-muted-foreground">{fr.maintenance.notSet}</span>
}
