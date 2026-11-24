import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { Link } from 'react-router'
import type { Role } from '@/api/client'
import { useCreateMember, useMembers } from '@/api/members'
import { useAuth } from '@/auth/AuthContext'
import { applyApiErrors, errorMessage, FormAlert, FormField } from '@/components/FormErrors'
import { Alert } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button, buttonVariants } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { fr } from '@/i18n/fr'
import { EMAIL_PATTERN, lastLoginLabel } from './memberFormat'
import { RoleSelect } from './RoleSelect'

type AddMemberForm = { email: string; displayName: string; role: Role }

/** Liste des membres et ajout d'un membre (US2, FR-010). */
export function MembersPage() {
  return (
    <div className="flex flex-col gap-4">
      <MemberList />
      <AddMemberCard />
    </div>
  )
}

function MemberList() {
  const members = useMembers()
  const { state } = useAuth()
  const currentId = state.status === 'authenticated' ? state.member.id : null

  return (
    <Card>
      <CardHeader>
        <CardTitle>{fr.members.title}</CardTitle>
        <CardDescription>{fr.members.description}</CardDescription>
      </CardHeader>
      <CardContent>
        {members.isPending && <p className="text-muted-foreground">{fr.loading}</p>}
        {members.isError && (
          <Alert variant="destructive" className="flex flex-col items-start gap-3">
            {errorMessage(members.error)}
            <Button variant="outline" size="sm" onClick={() => void members.refetch()}>
              {fr.retry}
            </Button>
          </Alert>
        )}
        {members.isSuccess && members.data.length === 0 && (
          <p className="text-muted-foreground">{fr.members.empty}</p>
        )}
        {members.isSuccess && members.data.length > 0 && (
          <ul className="flex flex-col divide-y">
            {members.data.map((member) => (
              <li key={member.id} className="flex items-center justify-between gap-3 py-3">
                <div className="min-w-0">
                  <p className="truncate font-medium">
                    {member.displayName ?? member.email}
                    {member.id === currentId && (
                      <span className="text-muted-foreground"> ({fr.members.you})</span>
                    )}
                  </p>
                  {member.displayName && (
                    <p className="truncate text-sm text-muted-foreground">{member.email}</p>
                  )}
                  <p className="text-sm text-muted-foreground">{lastLoginLabel(member)}</p>
                </div>
                <div className="flex shrink-0 flex-col items-end gap-2">
                  <Badge>{fr.roles[member.role]}</Badge>
                  <Link
                    to={`/admin/membres/${member.id}`}
                    className={buttonVariants({ variant: 'ghost', size: 'sm' })}
                    aria-label={`${fr.members.edit} ${member.email}`}
                  >
                    {fr.members.edit}
                  </Link>
                </div>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}

function AddMemberCard() {
  const createMember = useCreateMember()
  const [generalError, setGeneralError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    reset,
    setError,
    control,
    formState: { errors, isSubmitting },
  } = useForm<AddMemberForm>({ defaultValues: { email: '', displayName: '', role: 'USER' } })

  const role = useWatch({ control, name: 'role' })

  const onSubmit = handleSubmit(async (values) => {
    setGeneralError(null)
    setSuccess(null)
    try {
      const created = await createMember.mutateAsync({
        email: values.email.trim(),
        displayName: values.displayName.trim() || undefined,
        role: values.role,
      })
      reset()
      setSuccess(fr.members.added(created.email))
    } catch (error) {
      setGeneralError(
        applyApiErrors(error, setError, ['email', 'displayName', 'role'], {
          EMAIL_ALREADY_EXISTS: 'email',
        }),
      )
    }
  })

  return (
    <Card>
      <CardHeader>
        <CardTitle>{fr.members.addTitle}</CardTitle>
        <CardDescription>{fr.members.addDescription}</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
          <FormField id="email" label={fr.members.email} error={errors.email?.message}>
            <Input
              id="email"
              type="email"
              autoComplete="off"
              inputMode="email"
              placeholder={fr.members.emailPlaceholder}
              aria-invalid={errors.email ? true : undefined}
              aria-describedby={errors.email ? 'email-error' : undefined}
              {...register('email', {
                required: fr.validation.required,
                maxLength: { value: 320, message: fr.validation.maxLength(320) },
                validate: (value) => EMAIL_PATTERN.test(value.trim()) || fr.validation.email,
              })}
            />
          </FormField>
          <FormField id="displayName" label={fr.members.displayName} error={errors.displayName?.message}>
            <Input
              id="displayName"
              autoComplete="off"
              aria-invalid={errors.displayName ? true : undefined}
              {...register('displayName', {
                maxLength: { value: 100, message: fr.validation.maxLength(100) },
              })}
            />
          </FormField>
          <RoleSelect id="role" error={errors.role?.message} register={register('role')} value={role} />
          <FormAlert message={generalError} />
          {success && <Alert role="status">{success}</Alert>}
          <Button type="submit" disabled={isSubmitting} className="w-full sm:w-auto sm:self-start">
            {isSubmitting ? fr.members.adding : fr.members.add}
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}
