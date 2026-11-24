import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useParams } from 'react-router'
import type { EnergyType, Vehicle, VehicleInput } from '@/api/client'
import { useArchiveVehicle, useCreateVehicle, useUpdateVehicle, useVehicle } from '@/api/vehicles'
import { applyApiErrors, errorMessage, FormAlert, FormField } from '@/components/FormErrors'
import { Alert } from '@/components/ui/alert'
import { Button, buttonVariants } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
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
    <div className="flex flex-col gap-4">
      <Link to="/admin/parc" className={buttonVariants({ variant: 'ghost', className: 'self-start' })}>
        ← {fr.fleet.back}
      </Link>
      {vehicleId ? <EditVehicle vehicleId={vehicleId} /> : <VehicleFormCard />}
    </div>
  )
}

function EditVehicle({ vehicleId }: { vehicleId: string }) {
  const vehicle = useVehicle(vehicleId)
  if (vehicle.isPending) {
    return <p className="text-muted-foreground">{fr.loading}</p>
  }
  if (vehicle.isError) {
    return (
      <Alert variant="destructive" className="flex flex-col items-start gap-3">
        {errorMessage(vehicle.error)}
        <Button variant="outline" size="sm" onClick={() => void vehicle.refetch()}>
          {fr.retry}
        </Button>
      </Alert>
    )
  }
  return (
    <>
      <VehicleFormCard key={vehicle.data.id} vehicle={vehicle.data} />
      {vehicle.data.status === 'ACTIVE' ? (
        <ArchiveVehicleCard vehicle={vehicle.data} />
      ) : (
        <Alert role="status">{fr.fleet.archivedNotice}</Alert>
      )}
    </>
  )
}

function VehicleFormCard({ vehicle }: { vehicle?: Vehicle }) {
  const createVehicle = useCreateVehicle()
  const updateVehicle = useUpdateVehicle(vehicle?.id ?? '')
  const navigate = useNavigate()
  const [generalError, setGeneralError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)
  const currentYear = new Date().getFullYear()
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<VehicleForm>({ defaultValues: vehicle ? toForm(vehicle) : EMPTY_FORM })

  const onSubmit = handleSubmit(async (values) => {
    setGeneralError(null)
    setSaved(false)
    try {
      if (vehicle) {
        await updateVehicle.mutateAsync(toInput(values))
        setSaved(true)
      } else {
        await createVehicle.mutateAsync(toInput(values))
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
    <Card>
      <CardHeader>
        <CardTitle className="truncate">{vehicle ? fr.fleet.editTitle : fr.fleet.createTitle}</CardTitle>
        <CardDescription className="truncate">{vehicle ? vehicle.nickname : fr.fleet.createDescription}</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
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
          <div className="grid gap-4 sm:grid-cols-2">
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
          <FormField id="registration" label={fr.fleet.registration} error={errors.registration?.message}>
            <Input
              id="registration"
              autoComplete="off"
              autoCapitalize="characters"
              placeholder={fr.fleet.registrationPlaceholder}
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
          <div className="grid gap-4 sm:grid-cols-2">
            <FormField
              id="firstRegistrationYear"
              label={fr.fleet.firstRegistrationYear}
              error={errors.firstRegistrationYear?.message}
            >
              <Input
                id="firstRegistrationYear"
                inputMode="numeric"
                autoComplete="off"
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
            <FormField id="initialMileageKm" label={fr.fleet.initialMileageKm} error={errors.initialMileageKm?.message}>
              <Input
                id="initialMileageKm"
                inputMode="numeric"
                autoComplete="off"
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
          </div>
          <FormField id="vin" label={fr.fleet.vin} error={errors.vin?.message} hint={fr.fleet.vinHint}>
            <Input
              id="vin"
              autoComplete="off"
              autoCapitalize="characters"
              aria-invalid={errors.vin ? true : undefined}
              aria-describedby={describedBy('vin')}
              {...register('vin', {
                validate: (v) => v.trim() === '' || VIN.test(v.trim().toUpperCase()) || fr.validation.vin,
              })}
            />
          </FormField>
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
          <FormAlert message={generalError} />
          {saved && <Alert role="status">{fr.fleet.saved}</Alert>}
          <Button type="submit" disabled={isSubmitting} className="w-full sm:w-auto sm:self-start">
            {vehicle
              ? isSubmitting ? fr.fleet.saving : fr.fleet.save
              : isSubmitting ? fr.fleet.creating : fr.fleet.create}
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}

function ArchiveVehicleCard({ vehicle }: { vehicle: Vehicle }) {
  const archiveVehicle = useArchiveVehicle(vehicle.id)
  const navigate = useNavigate()
  const [confirming, setConfirming] = useState(false)

  const archive = async () => {
    try {
      await archiveVehicle.mutateAsync()
      void navigate('/admin/parc')
    } catch {
      setConfirming(false)
    }
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-5">
        {archiveVehicle.isError && <Alert variant="destructive">{errorMessage(archiveVehicle.error)}</Alert>}
        {confirming ? (
          <>
            <p className="text-sm">{fr.fleet.archiveConfirm(vehicle.nickname)}</p>
            <div className="flex flex-col gap-2 sm:flex-row">
              <Button variant="destructive" disabled={archiveVehicle.isPending} onClick={() => void archive()}>
                {archiveVehicle.isPending ? fr.fleet.archiving : fr.fleet.archiveConfirmButton}
              </Button>
              <Button variant="outline" onClick={() => setConfirming(false)}>
                {fr.fleet.cancel}
              </Button>
            </div>
          </>
        ) : (
          <Button variant="outline" className="text-destructive sm:self-start" onClick={() => setConfirming(true)}>
            {fr.fleet.archive}
          </Button>
        )}
      </CardContent>
    </Card>
  )
}
