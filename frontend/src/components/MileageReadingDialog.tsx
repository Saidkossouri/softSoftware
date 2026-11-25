import { TriangleAlert, X } from 'lucide-react'
import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { createPortal } from 'react-dom'
import { ApiError, type MileageReadingInput, type Vehicle } from '@/api/client'
import { useCreateMileageReading } from '@/api/mileage'
import { FormAlert, FormField, errorMessage } from '@/components/FormErrors'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useToast } from '@/components/ui/toast'
import { fr } from '@/i18n/fr'
import { formatDate, formatKm, todayIso } from '@/lib/format'

type Errors = Partial<Record<'mileageKm' | 'readingDate' | 'note', string>>

/**
 * Saisie d'un relevé (US1, FR-005 à FR-008) : feuille en bas de l'écran (plein écran sur
 * mobile), kilométrage pré-rempli avec le kilométrage actuel, date du jour par défaut. Un écart de
 * plus de 100 000 km demande une confirmation ; un relevé incohérent est signalé sous le champ
 * sans perdre la saisie.
 */
export function MileageReadingDialog({ vehicle, onClose }: { vehicle: Vehicle; onClose: () => void }) {
  const titleId = useId()
  const descriptionId = useId()
  const kmRef = useRef<HTMLInputElement>(null)
  const onCloseRef = useRef(onClose)
  const toast = useToast()
  const create = useCreateMileageReading(vehicle.id)
  const today = todayIso()

  const [km, setKm] = useState(String(vehicle.currentMileage.mileageKm))
  const [date, setDate] = useState(today)
  const [note, setNote] = useState('')
  const [errors, setErrors] = useState<Errors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [jumpWarning, setJumpWarning] = useState<string | null>(null)

  useEffect(() => {
    onCloseRef.current = onClose
  }, [onClose])

  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null
    kmRef.current?.focus()
    kmRef.current?.select()
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onCloseRef.current()
    }
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('keydown', onKey)
      previous?.focus()
    }
  }, [])

  function validate(): Errors {
    const found: Errors = {}
    if (!/^\d{1,7}$/.test(km.trim())) found.mileageKm = fr.mileage.invalidKm
    if (!date) found.readingDate = fr.validation.required
    else if (date > today) found.readingDate = fr.mileage.futureDate
    if (note.length > 200) found.note = fr.validation.maxLength(200)
    return found
  }

  async function send(confirmLargeJump: boolean) {
    const found = validate()
    setErrors(found)
    setFormError(null)
    if (Object.keys(found).length > 0) return
    const mileageKm = Number(km.trim())
    const body: MileageReadingInput = { readingDate: date, mileageKm, confirmLargeJump }
    if (note.trim()) body.note = note.trim()
    try {
      await create.mutateAsync(body)
      toast.success(fr.mileage.saved(formatKm(mileageKm)))
      onClose()
    } catch (error) {
      setJumpWarning(null)
      if (error instanceof ApiError && error.code === 'MILEAGE_JUMP_NOT_CONFIRMED') {
        setJumpWarning(error.problem.detail)
      } else if (error instanceof ApiError && error.code === 'MILEAGE_INCONSISTENT') {
        setErrors({ mileageKm: error.problem.detail })
      } else if (error instanceof ApiError && error.problem.fieldErrors?.length) {
        const placed: Errors = {}
        for (const fieldError of error.problem.fieldErrors) {
          if (fieldError.field === 'mileageKm' || fieldError.field === 'readingDate' || fieldError.field === 'note') {
            placed[fieldError.field] = fieldError.message
          }
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

  const current = fr.fleet.currentMileage(
    formatKm(vehicle.currentMileage.mileageKm),
    formatDate(vehicle.currentMileage.date),
  )

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center sm:p-4">
      <div aria-hidden className="absolute inset-0 bg-foreground/40 backdrop-blur-[2px]" onClick={onClose} />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={descriptionId}
        className="animate-in relative flex max-h-full w-full flex-col gap-4 overflow-y-auto rounded-t-2xl bg-card p-6 text-card-foreground shadow-card sm:max-w-md sm:rounded-2xl"
      >
        <div className="flex items-start gap-3">
          <div className="min-w-0 flex-1">
            <h2 id={titleId} className="font-display text-lg font-semibold">
              {fr.mileage.dialogTitle}
            </h2>
            <p id={descriptionId} className="text-sm text-muted-foreground">
              {fr.mileage.dialogDescription(vehicle.nickname, current)}
            </p>
          </div>
          <Button variant="ghost" size="icon" aria-label={fr.mileage.close} onClick={onClose}>
            <X aria-hidden />
          </Button>
        </div>

        <form noValidate onSubmit={onSubmit} className="flex flex-col gap-4">
          <FormField id="mileage-km" label={fr.mileage.km} error={errors.mileageKm}>
            <Input
              id="mileage-km"
              ref={kmRef}
              inputMode="numeric"
              autoComplete="off"
              className="font-display text-lg tabular-nums"
              value={km}
              aria-invalid={errors.mileageKm ? true : undefined}
              aria-describedby={errors.mileageKm ? 'mileage-km-error' : undefined}
              onChange={(event) => {
                setKm(event.target.value)
                setJumpWarning(null)
              }}
            />
          </FormField>
          <FormField id="mileage-date" label={fr.mileage.date} error={errors.readingDate}>
            <Input
              id="mileage-date"
              type="date"
              max={today}
              value={date}
              aria-invalid={errors.readingDate ? true : undefined}
              aria-describedby={errors.readingDate ? 'mileage-date-error' : undefined}
              onChange={(event) => setDate(event.target.value)}
            />
          </FormField>
          <FormField id="mileage-note" label={fr.mileage.note} error={errors.note}>
            <Input
              id="mileage-note"
              maxLength={200}
              placeholder={fr.mileage.notePlaceholder}
              value={note}
              aria-invalid={errors.note ? true : undefined}
              onChange={(event) => setNote(event.target.value)}
            />
          </FormField>

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
                <Button type="button" disabled={create.isPending} onClick={() => void send(true)}>
                  {fr.mileage.jumpConfirm}
                </Button>
              </div>
            </div>
          ) : (
            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <Button type="button" variant="outline" onClick={onClose}>
                {fr.mileage.cancel}
              </Button>
              <Button type="submit" disabled={create.isPending}>
                {create.isPending ? fr.mileage.saving : fr.mileage.save}
              </Button>
            </div>
          )}
        </form>
      </div>
    </div>,
    document.body,
  )
}
