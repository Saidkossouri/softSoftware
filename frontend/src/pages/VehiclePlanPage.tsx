import { CalendarClock, Check, Lightbulb, Pencil, Plus, Trash2, Wrench } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { Link, useParams } from 'react-router'
import { ApiError, type DueItem, type Vehicle } from '@/api/client'
import {
  useCompleteDeadline,
  useDeadlineSuggestions,
  useDeleteDeadline,
  useDeleteRule,
  useDismissSuggestion,
  useDueItems,
} from '@/api/schedule'
import { useVehicle } from '@/api/vehicles'
import { DueItemCard } from '@/components/DueItemCard'
import { errorMessage } from '@/components/FormErrors'
import { IconTile, ListCard, ListSkeleton } from '@/components/ListCard'
import { BackLink, PageHeader, SectionLabel } from '@/components/PageHeader'
import { DeadlineDialog, RuleDialog } from '@/components/ScheduleDialogs'
import { Alert } from '@/components/ui/alert'
import { Button, buttonVariants } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { ConfirmDialog } from '@/components/ui/dialog'
import { EmptyState } from '@/components/ui/empty-state'
import { useToast } from '@/components/ui/toast'
import { fr } from '@/i18n/fr'
import { formatDate } from '@/lib/format'
import { itemName } from '@/lib/schedule'

type Editing =
  | { kind: 'rule'; item?: DueItem }
  | { kind: 'deadline'; item?: DueItem; preset?: { typeId: string; recurrenceMonths?: number } }
type Deleting = { kind: 'rule' | 'deadline'; item: DueItem }

/**
 * Plan d'entretien d'un véhicule (US4, FR-021, FR-022, FR-036 à FR-040) : fréquences d'entretien,
 * échéances à date fixe et suggestions selon l'âge du véhicule. Tout membre ayant accès au
 * véhicule peut tout modifier ; un véhicule archivé est en lecture seule.
 */
export function VehiclePlanPage() {
  const { vehicleId = '' } = useParams()
  const vehicle = useVehicle(vehicleId)
  const forbidden = vehicle.error instanceof ApiError && vehicle.error.code === 'VEHICLE_FORBIDDEN'

  return (
    <>
      <BackLink to={`/vehicules/${vehicleId}`}>{fr.plan.back}</BackLink>
      {vehicle.isPending && <ListSkeleton />}
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
      {vehicle.isSuccess && <Plan vehicle={vehicle.data} />}
    </>
  )
}

function Plan({ vehicle }: { vehicle: Vehicle }) {
  const toast = useToast()
  const dueItems = useDueItems(vehicle.id)
  const suggestions = useDeadlineSuggestions(vehicle.id)
  const dismiss = useDismissSuggestion(vehicle.id)
  const complete = useCompleteDeadline()
  const deleteRule = useDeleteRule()
  const deleteDeadline = useDeleteDeadline()
  const [editing, setEditing] = useState<Editing | null>(null)
  const [deleting, setDeleting] = useState<Deleting | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const writable = vehicle.status === 'ACTIVE'

  const rules = dueItems.data?.filter((item) => item.kind === 'RULE') ?? []
  const deadlines = dueItems.data?.filter((item) => item.kind === 'DEADLINE') ?? []
  const deletion = deleting?.kind === 'rule' ? deleteRule : deleteDeadline

  async function onComplete(item: DueItem) {
    setActionError(null)
    try {
      const result = await complete.mutateAsync(item.deadlineId ?? '')
      toast.success(
        result.next
          ? fr.plan.doneNext(itemName(item), formatDate(result.next.dueDate))
          : fr.plan.done(itemName(item)),
      )
    } catch (error) {
      setActionError(errorMessage(error))
    }
  }

  async function onDismiss(typeId: string) {
    setActionError(null)
    try {
      await dismiss.mutateAsync(typeId)
      toast.success(fr.plan.dismissed)
    } catch (error) {
      setActionError(errorMessage(error))
    }
  }

  async function onDelete() {
    if (!deleting) return
    const id = deleting.kind === 'rule' ? deleting.item.ruleId : deleting.item.deadlineId
    try {
      await deletion.mutateAsync(id ?? '')
      toast.success(fr.plan.deleted)
      setDeleting(null)
    } catch {
      // Affiché dans le dialogue de confirmation.
    }
  }

  function itemActions(item: DueItem) {
    if (!writable) return undefined
    const name = itemName(item)
    const kind = item.kind === 'RULE' ? 'rule' : 'deadline'
    return (
      <div className="flex items-center gap-1">
        {item.kind === 'DEADLINE' && (
          <Button
            variant="outline"
            size="icon"
            aria-label={fr.plan.markDoneLabel(name)}
            title={fr.plan.markDone}
            disabled={complete.isPending}
            onClick={() => void onComplete(item)}
          >
            <Check aria-hidden />
          </Button>
        )}
        <Button
          variant="ghost"
          size="icon"
          aria-label={fr.plan.editLabel(name)}
          title={fr.plan.edit}
          onClick={() => setEditing({ kind, item })}
        >
          <Pencil aria-hidden />
        </Button>
        <Button
          variant="ghost"
          size="icon"
          aria-label={fr.plan.deleteLabel(name)}
          title={fr.plan.delete}
          onClick={() => {
            deleteRule.reset()
            deleteDeadline.reset()
            setDeleting({ kind, item })
          }}
        >
          <Trash2 aria-hidden />
        </Button>
      </div>
    )
  }

  return (
    <>
      <PageHeader title={fr.plan.title} description={fr.plan.description(vehicle.nickname)} />
      {actionError && <Alert variant="destructive">{actionError}</Alert>}

      {writable && suggestions.data && suggestions.data.length > 0 && (
        <section className="flex flex-col gap-3" aria-labelledby="plan-suggestions">
          <SectionLabel id="plan-suggestions">{fr.plan.suggestionsTitle}</SectionLabel>
          <ul className="flex flex-col gap-3">
            {suggestions.data.map((suggestion) => (
              <ListCard
                key={suggestion.type.id}
                stack
                wrap
                leading={
                  <IconTile className="bg-warning-soft text-warning">
                    <Lightbulb />
                  </IconTile>
                }
                title={<span className="truncate">{suggestion.type.name}</span>}
                subtitle={fr.plan.suggestionBody(
                  suggestion.recurrenceMonths ? fr.schedule.recurrence(suggestion.recurrenceMonths) : null,
                )}
                trailing={
                  <>
                    <Button
                      size="sm"
                      onClick={() =>
                        setEditing({
                          kind: 'deadline',
                          preset: { typeId: suggestion.type.id, recurrenceMonths: suggestion.recurrenceMonths },
                        })
                      }
                    >
                      {fr.plan.create}
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      disabled={dismiss.isPending}
                      onClick={() => void onDismiss(suggestion.type.id)}
                    >
                      {fr.plan.dismiss}
                    </Button>
                  </>
                }
              />
            ))}
          </ul>
        </section>
      )}

      {dueItems.isPending && <ListSkeleton />}
      {dueItems.isError && (
        <Alert variant="destructive">
          {errorMessage(dueItems.error)}
          <Button variant="outline" size="sm" onClick={() => void dueItems.refetch()}>
            {fr.retry}
          </Button>
        </Alert>
      )}
      {dueItems.isSuccess && (
        <>
          <PlanSection
            id="plan-rules"
            title={fr.plan.rulesTitle}
            empty={fr.plan.rulesEmpty}
            icon={<Wrench />}
            addLabel={writable ? fr.plan.addRule : undefined}
            onAdd={() => setEditing({ kind: 'rule' })}
          >
            {rules.map((item) => (
              <DueItemCard key={item.ruleId} item={item} actions={itemActions(item)} />
            ))}
          </PlanSection>
          <PlanSection
            id="plan-deadlines"
            title={fr.plan.deadlinesTitle}
            empty={fr.plan.deadlinesEmpty}
            icon={<CalendarClock />}
            addLabel={writable ? fr.plan.addDeadline : undefined}
            onAdd={() => setEditing({ kind: 'deadline' })}
          >
            {deadlines.map((item) => (
              <DueItemCard key={item.deadlineId} item={item} actions={itemActions(item)} />
            ))}
          </PlanSection>
        </>
      )}

      {editing?.kind === 'rule' && (
        <RuleDialog
          vehicle={vehicle}
          rule={editing.item}
          excludedTypeIds={rules.flatMap((item) => (item.type ? [item.type.id] : []))}
          onClose={() => setEditing(null)}
        />
      )}
      {editing?.kind === 'deadline' && (
        <DeadlineDialog
          vehicleId={vehicle.id}
          deadline={editing.item}
          preset={editing.preset}
          onClose={() => setEditing(null)}
        />
      )}
      <ConfirmDialog
        open={deleting !== null}
        title={deleting?.kind === 'rule' ? fr.plan.deleteRuleTitle : fr.plan.deleteDeadlineTitle}
        description={deleting?.kind === 'rule' ? fr.plan.deleteRuleBody : fr.plan.deleteDeadlineBody}
        confirmLabel={fr.plan.delete}
        cancelLabel={fr.plan.cancel}
        pending={deletion.isPending}
        error={deletion.isError ? <Alert variant="destructive">{errorMessage(deletion.error)}</Alert> : undefined}
        onConfirm={() => void onDelete()}
        onCancel={() => setDeleting(null)}
      />
    </>
  )
}

function PlanSection({
  id,
  title,
  empty,
  icon,
  addLabel,
  onAdd,
  children,
}: {
  id: string
  title: string
  empty: string
  icon: ReactNode
  addLabel?: string
  onAdd: () => void
  children: ReactNode[]
}) {
  return (
    <section className="flex flex-col gap-3" aria-labelledby={id}>
      <div className="flex items-center justify-between gap-3">
        <SectionLabel id={id}>{title}</SectionLabel>
        {addLabel && (
          <Button variant="outline" size="sm" onClick={onAdd}>
            <Plus aria-hidden />
            {addLabel}
          </Button>
        )}
      </div>
      {children.length > 0 ? (
        <ul className="flex flex-col gap-3">{children}</ul>
      ) : (
        <Card>
          <CardContent className="flex items-center gap-4 pt-5 sm:pt-6">
            <IconTile className="bg-muted text-muted-foreground">{icon}</IconTile>
            <p className="text-sm text-muted-foreground">{empty}</p>
          </CardContent>
        </Card>
      )}
    </section>
  )
}
