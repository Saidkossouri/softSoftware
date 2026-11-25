import { TriangleAlert } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router'
import {
  ApiError,
  type MaintenanceRecord,
  type MaintenanceRecordInput,
  type MaintenanceType,
  type TypeRef,
  type Vehicle,
} from '@/api/client'
import {
  useCreateMaintenanceRecord,
  useMaintenanceRecord,
  useMaintenanceTypes,
  useUpdateMaintenanceRecord,
} from '@/api/maintenance'
import { useVehicle } from '@/api/vehicles'
import { errorMessage, FormAlert, FormField } from '@/components/FormErrors'
import { BackLink, PageHeader, SectionLabel } from '@/components/PageHeader'
import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { useToast } from '@/components/ui/toast'
import { fr } from '@/i18n/fr'
import { formatKm, parseMoneyToCents, todayIso } from '@/lib/format'

type Field = 'recordDate' | 'mileageKm' | 'typeIds' | 'description' | 'costCents' | 'garage' | 'notes'
type Errors = Partial<Record<Field, string>>
const FIELDS: readonly string[] = ['recordDate', 'mileageKm', 'typeIds', 'description', 'costCents', 'garage', 'notes']

type Values = {
  recordDate: string
  mileageKm: string
  typeIds: string[]
  description: string
  cost: string
  garage: string
  notes: string
}

/** Montant en centimes affiché pour la saisie : « 450 » ou « 189,50 ». */
function centsToInput(cents: number): string {
  const units = Math.floor(cents / 100)
  const rest = cents % 100
  return rest === 0 ? String(units) : `${units},${String(rest).padStart(2, '0')}`
}

/**
 * Création (`/vehicules/:vehicleId/entretiens/nouveau`) et modification
 * (`/entretiens/:recordId/modifier`) d'un entretien (US2, FR-010 à FR-013).
 */
export function MaintenanceRecordFormPage() {
  const { vehicleId, recordId } = useParams()
  return recordId ? <EditRecord recordId={recordId} /> : <CreateRecord vehicleId={vehicleId ?? ''} />
}

function CreateRecord({ vehicleId }: { vehicleId: string }) {
  const vehicle = useVehicle(vehicleId)
  return (
    <>
      <BackLink to={`/vehicules/${vehicleId}`}>
        {vehicle.isSuccess ? fr.maintenance.backToVehicle(vehicle.data.nickname) : fr.maintenance.back}
      </BackLink>
      <Loader query={vehicle}>
        {(data) => (
          <RecordForm
            vehicle={data}
            initial={{
              recordDate: todayIso(),
              mileageKm: String(data.currentMileage.mileageKm),
              typeIds: [],
              description: '',
              cost: '',
              garage: '',
              notes: '',
            }}
          />
        )}
      </Loader>
    </>
  )
}

function EditRecord({ recordId }: { recordId: string }) {
  const record = useMaintenanceRecord(recordId)
  return (
    <>
      <BackLink to={`/entretiens/${recordId}`}>{fr.maintenance.back}</BackLink>
      <Loader query={record}>
        {(data) => <EditRecordForm record={data} />}
      </Loader>
    </>
  )
}

function EditRecordForm({ record }: { record: MaintenanceRecord }) {
  const vehicle = useVehicle(record.vehicleId)
  return (
    <Loader query={vehicle}>
      {(data) => (
        <RecordForm
          vehicle={data}
          record={record}
          initial={{
            recordDate: record.recordDate,
            mileageKm: String(record.mileageKm),
            typeIds: record.types.map((type) => type.id),
            description: record.description ?? '',
            cost: record.costCents === undefined ? '' : centsToInput(record.costCents),
            garage: record.garage ?? '',
            notes: record.notes ?? '',
          }}
        />
      )}
    </Loader>
  )
}

type QueryLike<T> = { isPending: boolean; isError: boolean; error: unknown; data?: T; refetch: () => unknown }

function Loader<T>({ query, children }: { query: QueryLike<T>; children: (data: T) => React.ReactNode }) {
  if (query.isPending) {
    return (
      <div className="flex flex-col gap-4" aria-busy="true">
        <p className="sr-only">{fr.loading}</p>
        <Skeleton className="h-9 w-2/3" />
        <Skeleton className="h-64" />
      </div>
    )
  }
  if (query.isError || query.data === undefined) {
    return (
      <Alert variant="destructive">
        {errorMessage(query.error)}
        <Button variant="outline" size="sm" onClick={() => void query.refetch()}>
          {fr.retry}
        </Button>
      </Alert>
    )
  }
  return <>{children(query.data)}</>
}

function RecordForm({ vehicle, record, initial }: { vehicle: Vehicle; record?: MaintenanceRecord; initial: Values }) {
  const navigate = useNavigate()
  const toast = useToast()
  const types = useMaintenanceTypes('MAINTENANCE')
  const create = useCreateMaintenanceRecord(vehicle.id)
  const update = useUpdateMaintenanceRecord(record?.id ?? '')
  const mutation = record ? update : create
  const today = todayIso()

  const [values, setValues] = useState<Values>(initial)
  const [errors, setErrors] = useState<Errors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [jumpWarning, setJumpWarning] = useState<string | null>(null)

  // Types proposés : catalogue actif, plus les types désactivés déjà présents sur l'entretien.
  const kept: TypeRef[] = (record?.types ?? []).filter((type) => !type.active)
  const options: (MaintenanceType | TypeRef)[] = [...(types.data ?? []), ...kept]
  const requiring = (types.data ?? []).find((type) => type.requiresDescription && values.typeIds.includes(type.id))

  function set<K extends keyof Values>(key: K, value: Values[K]) {
    setValues((current) => ({ ...current, [key]: value }))
  }

  function toggle(typeId: string, checked: boolean) {
    set('typeIds', checked ? [...values.typeIds, typeId] : values.typeIds.filter((id) => id !== typeId))
  }

  function validate(): { found: Errors; costCents?: number } {
    const found: Errors = {}
    if (!values.recordDate) found.recordDate = fr.validation.required
    else if (values.recordDate > today) found.recordDate = fr.mileage.futureDate
    if (!/^\d{1,7}$/.test(values.mileageKm.trim())) found.mileageKm = fr.mileage.invalidKm
    if (values.typeIds.length === 0) found.typeIds = fr.maintenance.noType
    if (requiring && !values.description.trim()) found.description = fr.validation.required
    let costCents: number | undefined
    if (values.cost.trim()) {
      try {
        costCents = parseMoneyToCents(values.cost)
      } catch {
        found.costCents = fr.maintenance.invalidCost
      }
    }
    return { found, costCents }
  }

  async function send(confirmLargeJump: boolean) {
    const { found, costCents } = validate()
    setErrors(found)
    setFormError(null)
    if (Object.keys(found).length > 0) return
    const body: MaintenanceRecordInput = {
      recordDate: values.recordDate,
      mileageKm: Number(values.mileageKm.trim()),
      typeIds: values.typeIds,
      confirmLargeJump,
      costCents,
      description: values.description.trim() || undefined,
      garage: values.garage.trim() || undefined,
      notes: values.notes.trim() || undefined,
    }
    try {
      const saved = await mutation.mutateAsync(body)
      toast.success(record ? fr.maintenance.saved : fr.maintenance.created)
      void navigate(`/entretiens/${saved.id}`)
    } catch (error) {
      setJumpWarning(null)
      if (error instanceof ApiError && error.code === 'MILEAGE_JUMP_NOT_CONFIRMED') {
        setJumpWarning(error.problem.detail)
      } else if (error instanceof ApiError && error.code === 'MILEAGE_INCONSISTENT') {
        setErrors({ mileageKm: error.problem.detail })
      } else if (error instanceof ApiError && error.problem.fieldErrors?.length) {
        const placed: Errors = {}
        for (const fieldError of error.problem.fieldErrors) {
          if (FIELDS.includes(fieldError.field)) placed[fieldError.field as Field] = fieldError.message
        }
        setErrors(placed)
        if (Object.keys(placed).length === 0) setFormError(errorMessage(error))
      } else {
        setFormError(errorMessage(error))
      }
    }
  }

  function onSubmit(event: FormEvent) {
    event.preventDefault()
    void send(false)
  }

  return (
    <>
      <PageHeader
        title={record ? fr.maintenance.editTitle : fr.maintenance.createTitle}
        description={`${vehicle.nickname} · ${formatKm(vehicle.currentMileage.mileageKm)}`}
      />
      <form noValidate onSubmit={onSubmit} className="flex flex-col gap-4">
        <Card>
          <CardContent className="flex flex-col gap-4 pt-5 sm:pt-6">
            <SectionLabel>{fr.maintenance.sectionWhen}</SectionLabel>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <FormField id="record-date" label={fr.maintenance.date} error={errors.recordDate}>
                <Input
                  id="record-date"
                  type="date"
                  max={today}
                  value={values.recordDate}
                  aria-invalid={errors.recordDate ? true : undefined}
                  onChange={(event) => set('recordDate', event.target.value)}
                />
              </FormField>
              <FormField id="record-km" label={fr.maintenance.km} error={errors.mileageKm}>
                <Input
                  id="record-km"
                  inputMode="numeric"
                  autoComplete="off"
                  className="tabular-nums"
                  value={values.mileageKm}
                  aria-invalid={errors.mileageKm ? true : undefined}
                  onChange={(event) => {
                    set('mileageKm', event.target.value)
                    setJumpWarning(null)
                  }}
                />
              </FormField>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="flex flex-col gap-4 pt-5 sm:pt-6">
            <SectionLabel>{fr.maintenance.sectionWhat}</SectionLabel>
            <fieldset className="flex flex-col gap-2" aria-describedby="record-types-help">
              <legend className="mb-2 text-sm font-medium">{fr.maintenance.types}</legend>
              {types.isPending && <Skeleton className="h-40" />}
              {types.isError && <Alert variant="destructive">{errorMessage(types.error)}</Alert>}
              <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                {options.map((type) => (
                  <label
                    key={type.id}
                    className="flex min-h-11 cursor-pointer items-center gap-3 rounded-xl border border-input bg-card px-3.5 py-2 text-sm has-checked:border-primary has-checked:bg-accent"
                  >
                    <Checkbox
                      checked={values.typeIds.includes(type.id)}
                      onChange={(event) => toggle(type.id, event.target.checked)}
                    />
                    <span className="min-w-0 flex-1">{type.name}</span>
                    {!type.active && <span className="text-xs text-muted-foreground">{fr.maintenance.typeInactive}</span>}
                  </label>
                ))}
              </div>
              <p id="record-types-help" className={errors.typeIds ? 'text-sm text-destructive' : 'text-sm text-muted-foreground'}>
                {errors.typeIds ?? fr.maintenance.typesHint}
              </p>
            </fieldset>
            <FormField
              id="record-description"
              label={requiring ? fr.maintenance.descriptionRequired(requiring.name) : fr.maintenance.description}
              error={errors.description}
            >
              <Input
                id="record-description"
                maxLength={200}
                placeholder={fr.maintenance.descriptionPlaceholder}
                value={values.description}
                aria-invalid={errors.description ? true : undefined}
                onChange={(event) => set('description', event.target.value)}
              />
            </FormField>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="flex flex-col gap-4 pt-5 sm:pt-6">
            <SectionLabel>{fr.maintenance.sectionDetails}</SectionLabel>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <FormField id="record-cost" label={fr.maintenance.cost} error={errors.costCents}>
                <Input
                  id="record-cost"
                  inputMode="decimal"
                  autoComplete="off"
                  placeholder={fr.maintenance.costPlaceholder}
                  className="tabular-nums"
                  value={values.cost}
                  aria-invalid={errors.costCents ? true : undefined}
                  onChange={(event) => set('cost', event.target.value)}
                />
              </FormField>
              <FormField id="record-garage" label={fr.maintenance.garage} error={errors.garage}>
                <Input
                  id="record-garage"
                  maxLength={100}
                  placeholder={fr.maintenance.garagePlaceholder}
                  value={values.garage}
                  onChange={(event) => set('garage', event.target.value)}
                />
              </FormField>
            </div>
            <FormField id="record-notes" label={fr.maintenance.notes} error={errors.notes}>
              <textarea
                id="record-notes"
                maxLength={2000}
                rows={3}
                value={values.notes}
                onChange={(event) => set('notes', event.target.value)}
                className="min-h-24 w-full rounded-xl border border-input bg-card px-3.5 py-2.5 text-base shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/25 sm:text-sm"
              />
            </FormField>
          </CardContent>
        </Card>

        <FormAlert message={formError} />

        {jumpWarning ? (
          <div role="alert" className="flex flex-col gap-3 rounded-xl bg-warning-soft px-4 py-3 text-sm text-warning">
            <p className="flex items-center gap-2 font-semibold">
              <TriangleAlert aria-hidden className="size-4 shrink-0" />
              {fr.mileage.jumpTitle}
            </p>
            <p className="text-foreground">{jumpWarning}</p>
            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <Button type="button" variant="outline" onClick={() => setJumpWarning(null)}>
                {fr.mileage.jumpFix}
              </Button>
              <Button type="button" disabled={mutation.isPending} onClick={() => void send(true)}>
                {fr.mileage.jumpConfirm}
              </Button>
            </div>
          </div>
        ) : (
          <Button type="submit" className="w-full sm:w-auto sm:self-end" disabled={mutation.isPending}>
            {mutation.isPending ? fr.maintenance.saving : fr.maintenance.save}
          </Button>
        )}
      </form>
    </>
  )
}
