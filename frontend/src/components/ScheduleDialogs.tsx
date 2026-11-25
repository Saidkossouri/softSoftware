import { useState, type FormEvent } from 'react'
import { ApiError, type DueItem, type Vehicle } from '@/api/client'
import { useMaintenanceTypes } from '@/api/maintenance'
import { useCreateDeadline, useCreateRule, useUpdateDeadline, useUpdateRule } from '@/api/schedule'
import { FormAlert, FormField, errorMessage } from '@/components/FormErrors'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { Sheet } from '@/components/ui/sheet'
import { useToast } from '@/components/ui/toast'
import { fr } from '@/i18n/fr'
import { todayIso } from '@/lib/format'
import { itemName } from '@/lib/schedule'

type RuleField = 'typeId' | 'intervalKm' | 'intervalMonths' | 'baselineDate' | 'baselineMileageKm'
type DeadlineField = 'typeId' | 'label' | 'dueDate' | 'recurrenceMonths'

const WHOLE_NUMBER = /^\d{1,7}$/

function optionalNumber(text: string): number | undefined {
  return text.trim() === '' ? undefined : Number(text.trim())
}

/** Place les erreurs par champ de l'API ; renvoie le message général s'il en reste un. */
function placeErrors<F extends string>(
  error: unknown,
  fields: readonly F[],
  setErrors: (errors: Partial<Record<F, string>>) => void,
): string | null {
  if (error instanceof ApiError && error.problem.fieldErrors?.length) {
    const placed: Partial<Record<F, string>> = {}
    for (const fieldError of error.problem.fieldErrors) {
      if ((fields as readonly string[]).includes(fieldError.field)) placed[fieldError.field as F] = fieldError.message
    }
    setErrors(placed)
    if (Object.keys(placed).length > 0) return null
  }
  return errorMessage(error)
}

/**
 * Fréquence d'entretien (US4, FR-021, FR-022) : opération, kilomètres et/ou mois pré-remplis avec
 * la fréquence conseillée du catalogue, et dernière réalisation connue (aujourd'hui et le
 * kilométrage actuel par défaut). En modification, l'opération ne change pas.
 */
export function RuleDialog({
  vehicle,
  rule,
  excludedTypeIds = [],
  onClose,
}: {
  vehicle: Vehicle
  rule?: DueItem
  excludedTypeIds?: string[]
  onClose: () => void
}) {
  const toast = useToast()
  const today = todayIso()
  const types = useMaintenanceTypes('MAINTENANCE')
  const create = useCreateRule(vehicle.id)
  const update = useUpdateRule()
  const pending = create.isPending || update.isPending

  const [typeId, setTypeId] = useState(rule?.type?.id ?? '')
  const [km, setKm] = useState(rule?.intervalKm != null ? String(rule.intervalKm) : '')
  const [months, setMonths] = useState(rule?.intervalMonths != null ? String(rule.intervalMonths) : '')
  const [baselineDate, setBaselineDate] = useState(rule?.lastDoneDate ?? today)
  const [baselineKm, setBaselineKm] = useState(
    String(rule?.lastDoneMileageKm ?? vehicle.currentMileage.mileageKm),
  )
  const [errors, setErrors] = useState<Partial<Record<RuleField, string>>>({})
  const [formError, setFormError] = useState<string | null>(null)

  const choices = (types.data ?? []).filter((type) => !excludedTypeIds.includes(type.id))

  function chooseType(id: string) {
    setTypeId(id)
    const type = types.data?.find((candidate) => candidate.id === id)
    setKm(type?.defaultIntervalKm != null ? String(type.defaultIntervalKm) : '')
    setMonths(type?.defaultIntervalMonths != null ? String(type.defaultIntervalMonths) : '')
  }

  function validate() {
    const found: Partial<Record<RuleField, string>> = {}
    if (!rule && !typeId) found.typeId = fr.plan.chooseType
    for (const [field, value] of [['intervalKm', km], ['intervalMonths', months]] as const) {
      if (value.trim() !== '' && (!WHOLE_NUMBER.test(value.trim()) || Number(value) < 1)) {
        found[field] = fr.plan.invalidNumber
      }
    }
    if (!found.intervalKm && !found.intervalMonths && km.trim() === '' && months.trim() === '') {
      found.intervalKm = fr.plan.noInterval
    }
    if (!baselineDate) found.baselineDate = fr.validation.required
    else if (baselineDate > today) found.baselineDate = fr.plan.futureDate
    if (!WHOLE_NUMBER.test(baselineKm.trim())) found.baselineMileageKm = fr.plan.invalidNumber
    return found
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    const found = validate()
    setErrors(found)
    setFormError(null)
    if (Object.keys(found).length > 0) return
    const common = {
      intervalKm: optionalNumber(km),
      intervalMonths: optionalNumber(months),
      baselineDate,
      baselineMileageKm: Number(baselineKm.trim()),
    }
    try {
      if (rule?.ruleId) await update.mutateAsync({ ruleId: rule.ruleId, body: common })
      else await create.mutateAsync({ typeId, ...common })
      toast.success(fr.plan.ruleSaved)
      onClose()
    } catch (error) {
      if (error instanceof ApiError && error.code === 'RULE_ALREADY_EXISTS') {
        setErrors({ typeId: error.problem.detail })
        return
      }
      setFormError(
        placeErrors(error, ['typeId', 'intervalKm', 'intervalMonths', 'baselineDate', 'baselineMileageKm'], setErrors),
      )
    }
  }

  return (
    <Sheet
      title={rule ? fr.plan.ruleEditTitle(itemName(rule)) : fr.plan.ruleCreateTitle}
      description={fr.plan.ruleDescription}
      closeLabel={fr.plan.close}
      onClose={onClose}
    >
      <form noValidate onSubmit={(event) => void onSubmit(event)} className="flex flex-col gap-4">
        {!rule && (
          <FormField id="rule-type" label={fr.plan.type} error={errors.typeId}>
            <Select
              id="rule-type"
              value={typeId}
              aria-invalid={errors.typeId ? true : undefined}
              onChange={(event) => chooseType(event.target.value)}
            >
              <option value="">{fr.plan.chooseType}</option>
              {choices.map((type) => (
                <option key={type.id} value={type.id}>
                  {type.name}
                </option>
              ))}
            </Select>
          </FormField>
        )}
        <div className="grid grid-cols-2 gap-4">
          <FormField id="rule-km" label={fr.plan.intervalKm} error={errors.intervalKm}>
            <Input
              id="rule-km"
              inputMode="numeric"
              className="tabular-nums"
              value={km}
              aria-invalid={errors.intervalKm ? true : undefined}
              onChange={(event) => setKm(event.target.value)}
            />
          </FormField>
          <FormField id="rule-months" label={fr.plan.intervalMonths} error={errors.intervalMonths}>
            <Input
              id="rule-months"
              inputMode="numeric"
              className="tabular-nums"
              value={months}
              aria-invalid={errors.intervalMonths ? true : undefined}
              onChange={(event) => setMonths(event.target.value)}
            />
          </FormField>
        </div>
        <p className="-mt-2 text-sm text-muted-foreground">{fr.plan.intervalHint}</p>
        <div className="grid grid-cols-2 gap-4">
          <FormField id="rule-baseline-date" label={fr.plan.baselineDate} error={errors.baselineDate}>
            <Input
              id="rule-baseline-date"
              type="date"
              max={today}
              value={baselineDate}
              aria-invalid={errors.baselineDate ? true : undefined}
              onChange={(event) => setBaselineDate(event.target.value)}
            />
          </FormField>
          <FormField id="rule-baseline-km" label={fr.plan.baselineKm} error={errors.baselineMileageKm}>
            <Input
              id="rule-baseline-km"
              inputMode="numeric"
              className="tabular-nums"
              value={baselineKm}
              aria-invalid={errors.baselineMileageKm ? true : undefined}
              onChange={(event) => setBaselineKm(event.target.value)}
            />
          </FormField>
        </div>
        <p className="-mt-2 text-sm text-muted-foreground">{fr.plan.baselineHint}</p>
        <FormAlert message={formError} />
        <Button type="submit" disabled={pending} className="w-full">
          {pending ? fr.plan.saving : fr.plan.save}
        </Button>
      </form>
    </Sheet>
  )
}

type Recurrence = 'NONE' | 'YEARLY' | 'OTHER'

/** Valeur du choix « Autre (libellé libre) » ; '' = rien de choisi. */
const OTHER = 'OTHER'

function recurrenceOf(months: number | null | undefined): { mode: Recurrence; months: string } {
  if (months == null) return { mode: 'NONE', months: '' }
  return months === 12 ? { mode: 'YEARLY', months: '12' } : { mode: 'OTHER', months: String(months) }
}

/**
 * Échéance à date fixe (US4, FR-036 à FR-039) : type administratif (assurance, vignette, visite
 * technique) ou libellé libre, date limite et récurrence pré-remplie depuis le type.
 */
export function DeadlineDialog({
  vehicleId,
  deadline,
  preset,
  onClose,
}: {
  vehicleId: string
  deadline?: DueItem
  preset?: { typeId: string; recurrenceMonths?: number }
  onClose: () => void
}) {
  const toast = useToast()
  const types = useMaintenanceTypes('ADMINISTRATIVE')
  const create = useCreateDeadline(vehicleId)
  const update = useUpdateDeadline()
  const pending = create.isPending || update.isPending

  const initialType = deadline ? (deadline.type?.id ?? OTHER) : (preset?.typeId ?? '')
  const initialRecurrence = recurrenceOf(deadline ? deadline.recurrenceMonths : preset?.recurrenceMonths)
  const [typeId, setTypeId] = useState(initialType)
  const [label, setLabel] = useState(
    deadline && deadline.label !== deadline.type?.name ? (deadline.label ?? '') : '',
  )
  const [dueDate, setDueDate] = useState(deadline?.dueDate ?? '')
  const [mode, setMode] = useState<Recurrence>(initialRecurrence.mode)
  const [months, setMonths] = useState(initialRecurrence.months)
  const [errors, setErrors] = useState<Partial<Record<DeadlineField, string>>>({})
  const [formError, setFormError] = useState<string | null>(null)

  const choices = (types.data ?? []).filter((type) => type.active || type.id === initialType)

  function chooseType(id: string) {
    setTypeId(id)
    const type = types.data?.find((candidate) => candidate.id === id)
    const recurrence = recurrenceOf(type?.defaultIntervalMonths)
    setMode(recurrence.mode)
    setMonths(recurrence.months)
  }

  function validate() {
    const found: Partial<Record<DeadlineField, string>> = {}
    if (!typeId) found.typeId = fr.plan.chooseDeadlineType
    else if (typeId === OTHER && label.trim() === '') found.label = fr.plan.labelRequired
    if (label.trim().length > 60) found.label = fr.validation.maxLength(60)
    if (!dueDate) found.dueDate = fr.validation.required
    if (mode === 'OTHER' && (!WHOLE_NUMBER.test(months.trim()) || Number(months) < 1 || Number(months) > 120)) {
      found.recurrenceMonths = fr.plan.invalidNumber
    }
    return found
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    const found = validate()
    setErrors(found)
    setFormError(null)
    if (Object.keys(found).length > 0) return
    const body = {
      typeId: typeId === OTHER ? undefined : typeId,
      label: label.trim() || undefined,
      dueDate,
      recurrenceMonths: mode === 'NONE' ? undefined : mode === 'YEARLY' ? 12 : Number(months.trim()),
    }
    try {
      if (deadline?.deadlineId) await update.mutateAsync({ deadlineId: deadline.deadlineId, body })
      else await create.mutateAsync(body)
      toast.success(fr.plan.deadlineSaved)
      onClose()
    } catch (error) {
      setFormError(placeErrors(error, ['typeId', 'label', 'dueDate', 'recurrenceMonths'], setErrors))
    }
  }

  return (
    <Sheet
      title={deadline ? fr.plan.deadlineEditTitle(itemName(deadline)) : fr.plan.deadlineCreateTitle}
      description={fr.plan.deadlineDescription}
      closeLabel={fr.plan.close}
      onClose={onClose}
    >
      <form noValidate onSubmit={(event) => void onSubmit(event)} className="flex flex-col gap-4">
        <FormField id="deadline-type" label={fr.plan.deadlineType} error={errors.typeId}>
          <Select
            id="deadline-type"
            value={typeId}
            aria-invalid={errors.typeId ? true : undefined}
            onChange={(event) => chooseType(event.target.value)}
          >
            <option value="">{fr.plan.chooseDeadlineType}</option>
            {choices.map((type) => (
              <option key={type.id} value={type.id}>
                {type.name}
              </option>
            ))}
            <option value={OTHER}>{fr.plan.otherType}</option>
          </Select>
        </FormField>
        <FormField id="deadline-label" label={typeId === OTHER ? fr.plan.label : fr.plan.labelOptional} error={errors.label}>
          <Input
            id="deadline-label"
            value={label}
            placeholder={fr.plan.labelPlaceholder}
            maxLength={60}
            aria-invalid={errors.label ? true : undefined}
            onChange={(event) => setLabel(event.target.value)}
          />
        </FormField>
        <FormField id="deadline-date" label={fr.plan.dueDate} error={errors.dueDate}>
          <Input
            id="deadline-date"
            type="date"
            value={dueDate}
            aria-invalid={errors.dueDate ? true : undefined}
            onChange={(event) => setDueDate(event.target.value)}
          />
        </FormField>
        <div className="grid grid-cols-2 gap-4">
          <FormField id="deadline-recurrence" label={fr.plan.recurrenceLabel}>
            <Select
              id="deadline-recurrence"
              value={mode}
              onChange={(event) => {
                const next = event.target.value as Recurrence
                setMode(next)
                if (next === 'OTHER' && !months) setMonths('6')
              }}
            >
              <option value="NONE">{fr.plan.recurrenceNone}</option>
              <option value="YEARLY">{fr.plan.recurrenceYearly}</option>
              <option value="OTHER">{fr.plan.recurrenceOther}</option>
            </Select>
          </FormField>
          {mode === 'OTHER' && (
            <FormField id="deadline-months" label={fr.plan.recurrenceMonths} error={errors.recurrenceMonths}>
              <Input
                id="deadline-months"
                inputMode="numeric"
                className="tabular-nums"
                value={months}
                aria-invalid={errors.recurrenceMonths ? true : undefined}
                onChange={(event) => setMonths(event.target.value)}
              />
            </FormField>
          )}
        </div>
        <FormAlert message={formError} />
        <Button type="submit" disabled={pending} className="w-full">
          {pending ? fr.plan.saving : fr.plan.save}
        </Button>
      </form>
    </Sheet>
  )
}
