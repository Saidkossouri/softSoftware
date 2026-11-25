import { useCallback, useState, type ReactNode } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate, useParams } from 'react-router'
import type { EnergyType, Vehicle, VehicleInput } from '@/api/client'
import { useArchiveVehicle, useCreateVehicle, useUpdateVehicle, useVehicle } from '@/api/vehicles'
import { applyApiErrors, errorMessage, FormAlert, FormField } from '@/components/FormErrors'
import { BackLink, PageHeader, SectionLabel } from '@/components/PageHeader'
import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { ConfirmDialog } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { useToast } from '@/components/ui/toast'
import { fr } from '@/i18n/fr'

type VehicleForm = {
  nickname: string
  make: string
  model: string
  registration: string
  firstRegistrationYear: string
  initialMileageKm: string
  vin: string
  energyType: EnergyType | ''
}

const FIELDS = [
  'nickname',
  'make',
  'model',
  'registration',
  'firstRegistrationYear',
  'initialMileageKm',
  'vin',
  'energyType',
] as const satisfies readonly (keyof VehicleForm)[]

const ENERGY_TYPES = Object.keys(fr.energyTypes) as EnergyType[]
const REGISTRATION_KEY = /^[A-Z0-9]{2,20}$/
const VIN = /^[A-HJ-NPR-Z0-9]{17}$/
const INTEGER = /^\d+$/

const EMPTY_FORM: VehicleForm = {
  nickname: '',
  make: '',
  model: '',
  registration: '',
  firstRegistrationYear: '',
  initialMileageKm: '',
  vin: '',
  energyType: '',
}

function toForm(vehicle: Vehicle): VehicleForm {
  return {
    nickname: vehicle.nickname,
    make: vehicle.make,
    model: vehicle.model,
    registration: vehicle.registration,
    firstRegistrationYear: vehicle.firstRegistrationYear?.toString() ?? '',
    initialMileageKm: vehicle.initialMileageKm.toString(),
    vin: vehicle.vin ?? '',
    energyType: vehicle.energyType ?? '',
  }
}

/** Saisie envoyée au serveur : textes sans espaces de bord, champs facultatifs vides omis. */
function toInput(values: VehicleForm): VehicleInput {
  return {
    nickname: values.nickname.trim(),
    make: values.make.trim(),
    model: values.model.trim(),
    registration: values.registration.trim(),
    initialMileageKm: Number(values.initialMileageKm.trim()),
    firstRegistrationYear: values.firstRegistrationYear.trim() ? Number(values.firstRegistrationYear.trim()) : undefined,
    vin: values.vin.trim() ? values.vin.trim().toUpperCase() : undefined,
    energyType: values.energyType || undefined,
  }
}

/** Création (`/admin/parc/nouveau`) et modification (`/admin/parc/:vehicleId`) d'un véhicule (US3). */
export function VehicleFormPage() {
  const { vehicleId } = useParams()
  return (
    <>
      <BackLink to="/admin/parc">{fr.fleet.back}</BackLink>
      {vehicleId ? <EditVehicle vehicleId={vehicleId} /> : <VehicleForm />}
    </>
  )
}

function EditVehicle({ vehicleId }: { vehicleId: string }) {
  const vehicle = useVehicle(vehicleId)
  if (vehicle.isPending) {
    return (
      <div className="flex flex-col gap-4" aria-busy="true">
        <p className="sr-only">{fr.loading}</p>
        <Skeleton className="h-9 w-2/3" />
        <Skeleton className="h-64" />
      </div>
    )
  }
  if (vehicle.isError) {
    return (
      <Alert variant="destructive">
        {errorMessage(vehicle.error)}
        <Button variant="outline" size="sm" onClick={() => void vehicle.refetch()}>
          {fr.retry}
        </Button>
      </Alert>
    )
  }
  return (
    <>
      {vehicle.data.status === 'ARCHIVED' && <Alert role="status">{fr.fleet.archivedNotice}</Alert>}
      <VehicleForm key={vehicle.data.id} vehicle={vehicle.data} />
      {vehicle.data.status === 'ACTIVE' && <ArchiveVehicleCard vehicle={vehicle.data} />}
    </>
  )
}

function VehicleForm({ vehicle }: { vehicle?: Vehicle }) {
  const createVehicle = useCreateVehicle()
  const updateVehicle = useUpdateVehicle(vehicle?.id ?? '')
  const navigate = useNavigate()
  const toast = useToast()
  const [generalError, setGeneralError] = useState<string | null>(null)
  const currentYear = new Date().getFullYear()
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<VehicleForm>({ defaultValues: vehicle ? toForm(vehicle) : EMPTY_FORM })

  const onSubmit = handleSubmit(async (values) => {
    setGeneralError(null)
    try {
      if (vehicle) {
        await updateVehicle.mutateAsync(toInput(values))
        toast.success(fr.fleet.saved)
      } else {
        const created = await createVehicle.mutateAsync(toInput(values))
        toast.success(fr.fleet.created(created.nickname))
        void navigate('/admin/parc')
      }
    } catch (error) {
      setGeneralError(
        applyApiErrors(error, setError, FIELDS, { REGISTRATION_ALREADY_EXISTS: 'registration' }),
      )
    }
  })

  const required = { required: fr.validation.required, validate: (v: string) => v.trim() !== '' || fr.validation.required }
  const text50 = { ...required, maxLength: { value: 50, message: fr.validation.maxLength(50) } }
  const describedBy = (field: keyof VehicleForm) => (errors[field] ? `${field}-error` : undefined)

  return (
    <>
      <PageHeader
        title={vehicle ? vehicle.nickname : fr.fleet.createTitle}
        description={vehicle ? fr.fleet.editTitle : fr.fleet.createDescription}
      />
      <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
        <FormSection label={fr.fleet.sectionIdentity}>
          <FormField id="nickname" label={fr.fleet.nickname} error={errors.nickname?.message}>
            <Input
              id="nickname"
              autoComplete="off"
              placeholder={fr.fleet.nicknamePlaceholder}
              aria-invalid={errors.nickname ? true : undefined}
              aria-describedby={describedBy('nickname')}
              {...register('nickname', text50)}
            />
          </FormField>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <FormField id="make" label={fr.fleet.make} error={errors.make?.message}>
              <Input
                id="make"
                autoComplete="off"
                aria-invalid={errors.make ? true : undefined}
                aria-describedby={describedBy('make')}
                {...register('make', text50)}
              />
            </FormField>
            <FormField id="model" label={fr.fleet.model} error={errors.model?.message}>
              <Input
                id="model"
                autoComplete="off"
                aria-invalid={errors.model ? true : undefined}
                aria-describedby={describedBy('model')}
                {...register('model', text50)}
              />
            </FormField>
          </div>
        </FormSection>
        <FormSection label={fr.fleet.sectionRegistration}>
          <FormField id="registration" label={fr.fleet.registration} error={errors.registration?.message}>
            <Input
              id="registration"
              autoComplete="off"
              autoCapitalize="characters"
              placeholder={fr.fleet.registrationPlaceholder}
              className="h-12 border-2 border-foreground/70 text-center font-display text-lg font-semibold tracking-widest uppercase placeholder:font-normal sm:text-lg"
              aria-invalid={errors.registration ? true : undefined}
              aria-describedby={describedBy('registration')}
              {...register('registration', {
                ...required,
                maxLength: { value: 20, message: fr.validation.maxLength(20) },
                validate: (v) =>
                  REGISTRATION_KEY.test(v.toUpperCase().replace(/[\s-]+/g, '')) || fr.validation.registration,
              })}
            />
          </FormField>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <FormField id="initialMileageKm" label={fr.fleet.initialMileageKm} error={errors.initialMileageKm?.message}>
              <Input
                id="initialMileageKm"
                inputMode="numeric"
                autoComplete="off"
                className="tabular-nums"
                aria-invalid={errors.initialMileageKm ? true : undefined}
                aria-describedby={describedBy('initialMileageKm')}
                {...register('initialMileageKm', {
                  ...required,
                  validate: (v) => {
                    const value = v.trim()
                    if (value === '') return fr.validation.required
                    return (INTEGER.test(value) && Number(value) <= 2_000_000) || fr.validation.mileage
                  },
                })}
              />
            </FormField>
            <FormField
              id="firstRegistrationYear"
              label={fr.fleet.firstRegistrationYear}
              error={errors.firstRegistrationYear?.message}
            >
              <Input
                id="firstRegistrationYear"
                inputMode="numeric"
                autoComplete="off"
                className="tabular-nums"
                aria-invalid={errors.firstRegistrationYear ? true : undefined}
                aria-describedby={describedBy('firstRegistrationYear')}
                {...register('firstRegistrationYear', {
                  validate: (v) => {
                    const value = v.trim()
                    if (value === '') return true
                    const year = Number(value)
                    return (INTEGER.test(value) && year >= 1900 && year <= currentYear) || fr.validation.year(currentYear)
                  },
                })}
              />
            </FormField>
          </div>
        </FormSection>
        <FormSection label={fr.fleet.sectionDetails}>
          <FormField id="energyType" label={fr.fleet.energyType} error={errors.energyType?.message}>
            <Select id="energyType" aria-invalid={errors.energyType ? true : undefined} {...register('energyType')}>
              <option value="">{fr.fleet.energyNone}</option>
              {ENERGY_TYPES.map((type) => (
                <option key={type} value={type}>
                  {fr.energyTypes[type]}
                </option>
              ))}
            </Select>
          </FormField>
          <FormField id="vin" label={fr.fleet.vin} error={errors.vin?.message} hint={fr.fleet.vinHint}>
            <Input
              id="vin"
              autoComplete="off"
              autoCapitalize="characters"
              className="font-mono uppercase"
              aria-invalid={errors.vin ? true : undefined}
              aria-describedby={describedBy('vin')}
              {...register('vin', {
                validate: (v) => v.trim() === '' || VIN.test(v.trim().toUpperCase()) || fr.validation.vin,
              })}
            />
          </FormField>
        </FormSection>
        <FormAlert message={generalError} />
        <Button type="submit" size="lg" disabled={isSubmitting} className="w-full sm:w-auto sm:self-end">
          {vehicle
            ? isSubmitting ? fr.fleet.saving : fr.fleet.save
            : isSubmitting ? fr.fleet.creating : fr.fleet.create}
        </Button>
      </form>
    </>
  )
}

function FormSection({ label, children }: { label: string; children: ReactNode }) {
  return (
    <Card>
      <CardContent className="flex flex-col gap-4 pt-5 sm:pt-6">
        <SectionLabel>{label}</SectionLabel>
        {children}
      </CardContent>
    </Card>
  )
}

function ArchiveVehicleCard({ vehicle }: { vehicle: Vehicle }) {
  const archiveVehicle = useArchiveVehicle(vehicle.id)
  const navigate = useNavigate()
  const toast = useToast()
  const [confirming, setConfirming] = useState(false)
  const { reset } = archiveVehicle
  const close = useCallback(() => {
    setConfirming(false)
    reset()
  }, [reset])

  const archive = async () => {
    try {
      await archiveVehicle.mutateAsync()
      toast.success(fr.fleet.archived(vehicle.nickname))
      void navigate('/admin/parc')
    } catch {
      // l'erreur est affichée dans le dialogue
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">{fr.fleet.archiveTitle}</CardTitle>
        <CardDescription>{fr.fleet.archiveDescription}</CardDescription>
      </CardHeader>
      <CardContent>
        <Button variant="destructive-outline" className="w-full sm:w-auto" onClick={() => setConfirming(true)}>
          {fr.fleet.archive}
        </Button>
      </CardContent>
      <ConfirmDialog
        open={confirming}
        title={fr.fleet.archive}
        description={fr.fleet.archiveConfirm(vehicle.nickname)}
        confirmLabel={archiveVehicle.isPending ? fr.fleet.archiving : fr.fleet.archiveConfirmButton}
        cancelLabel={fr.fleet.cancel}
        pending={archiveVehicle.isPending}
        error={archiveVehicle.isError && <Alert variant="destructive">{errorMessage(archiveVehicle.error)}</Alert>}
        onConfirm={() => void archive()}
        onCancel={close}
      />
    </Card>
  )
}
