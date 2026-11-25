import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { Pencil, UserPlus } from 'lucide-react'
import { Link } from 'react-router'
import type { Role } from '@/api/client'
import { useCreateMember, useMembers } from '@/api/members'
import { useAuth } from '@/auth/AuthContext'
import { applyApiErrors, errorMessage, FormAlert, FormField } from '@/components/FormErrors'
import { Avatar, ListCard, ListSkeleton } from '@/components/ListCard'
import { PageHeader } from '@/components/PageHeader'
import { Alert } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button, buttonVariants } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { EmptyState } from '@/components/ui/empty-state'
import { Input } from '@/components/ui/input'
import { useToast } from '@/components/ui/toast'
import { fr } from '@/i18n/fr'
import { EMAIL_PATTERN, lastLoginLabel } from './memberFormat'
import { RoleSelect } from './RoleSelect'

type AddMemberForm = { email: string; displayName: string; role: Role }

/** Liste des membres et ajout d'un membre (US2, FR-010). */
export function MembersPage() {
  return (
    <>
      <PageHeader title={fr.members.title} description={fr.members.description} />
      <MemberList />
      <AddMemberCard />
    </>
  )
}

function MemberList() {
  const members = useMembers()
  const { state } = useAuth()
  const currentId = state.status === 'authenticated' ? state.member.id : null

  if (members.isPending) {
    return <ListSkeleton />
  }
  if (members.isError) {
    return (
      <Alert variant="destructive">
        {errorMessage(members.error)}
        <Button variant="outline" size="sm" onClick={() => void members.refetch()}>
          {fr.retry}
        </Button>
      </Alert>
    )
  }
  if (members.data.length === 0) {
    return (
      <Card>
        <EmptyState illustration="people" title={fr.members.empty} description={fr.members.emptyHint} />
      </Card>
    )
  }
  return (
    <ul className="flex flex-col gap-3">
      {members.data.map((member) => (
        <ListCard
          key={member.id}
          leading={<Avatar name={member.displayName ?? member.email} />}
          title={
            <>
              <span className="truncate">{member.displayName ?? member.email}</span>
              {member.id === currentId && (
                <span className="shrink-0 text-sm font-normal text-muted-foreground">({fr.members.you})</span>
              )}
            </>
          }
          subtitle={member.displayName ? member.email : undefined}
          meta={
            <>
              <Badge variant={member.role === 'ADMIN' ? 'primary' : 'default'}>{fr.roles[member.role]}</Badge>
              <span className="text-xs text-muted-foreground">{lastLoginLabel(member)}</span>
            </>
          }
          trailing={
            <Link
              to={`/admin/membres/${member.id}`}
              className={buttonVariants({ variant: 'ghost', size: 'sm', className: 'size-11 px-0 sm:h-9 sm:w-auto sm:px-3' })}
              aria-label={`${fr.members.edit} ${member.email}`}
            >
              <Pencil aria-hidden className="size-4" />
              <span className="hidden sm:inline">{fr.members.edit}</span>
            </Link>
          }
        />
      ))}
    </ul>
  )
}

function AddMemberCard() {
  const createMember = useCreateMember()
  const [generalError, setGeneralError] = useState<string | null>(null)
  const toast = useToast()
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
    try {
      const created = await createMember.mutateAsync({
        email: values.email.trim(),
        displayName: values.displayName.trim() || undefined,
        role: values.role,
      })
      reset()
      toast.success(fr.members.added(created.email))
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
        <CardTitle className="flex items-center gap-2">
          <UserPlus aria-hidden className="size-5 text-primary" />
          {fr.members.addTitle}
        </CardTitle>
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
          <Button type="submit" disabled={isSubmitting} className="w-full sm:w-auto sm:self-start">
            {isSubmitting ? fr.members.adding : fr.members.add}
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}
