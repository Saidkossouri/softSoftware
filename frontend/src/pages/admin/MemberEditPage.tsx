import { useCallback, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { useNavigate, useParams } from 'react-router'
import type { Member, Role } from '@/api/client'
import { useDeleteMember, useMember, useUpdateMember } from '@/api/members'
import { applyApiErrors, errorMessage, FormAlert, FormField } from '@/components/FormErrors'
import { Avatar } from '@/components/ListCard'
import { BackLink } from '@/components/PageHeader'
import { Alert } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { ConfirmDialog } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { useToast } from '@/components/ui/toast'
import { fr } from '@/i18n/fr'
import { lastLoginLabel } from './memberFormat'
import { RoleSelect } from './RoleSelect'

type EditMemberForm = { displayName: string; role: Role }

/** Modification du nom et du rôle, retrait de l'accès (US2, FR-010, FR-012). */
export function MemberEditPage() {
  const { memberId = '' } = useParams()
  const member = useMember(memberId)

  return (
    <>
      <BackLink to="/admin/membres">{fr.members.back}</BackLink>
      {member.isPending && (
        <div className="flex flex-col gap-4" aria-busy="true">
          <p className="sr-only">{fr.loading}</p>
          <Skeleton className="h-16" />
          <Skeleton className="h-56" />
        </div>
      )}
      {member.isError && (
        <Alert variant="destructive">
          {errorMessage(member.error)}
          <Button variant="outline" size="sm" onClick={() => void member.refetch()}>
            {fr.retry}
          </Button>
        </Alert>
      )}
      {member.isSuccess && (
        <>
          <MemberSummary member={member.data} />
          <EditMemberCard key={member.data.id} member={member.data} />
          <RemoveMemberCard member={member.data} />
        </>
      )}
    </>
  )
}

function MemberSummary({ member }: { member: Member }) {
  return (
    <div className="flex min-w-0 items-center gap-4">
      <Avatar name={member.displayName ?? member.email} className="size-14 text-xl" />
      <div className="min-w-0">
        <h1 className="truncate text-2xl font-bold">{member.displayName ?? member.email}</h1>
        <div className="mt-1 flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
          <Badge variant={member.role === 'ADMIN' ? 'primary' : 'default'}>{fr.roles[member.role]}</Badge>
          <span>{lastLoginLabel(member)}</span>
        </div>
      </div>
    </div>
  )
}

function EditMemberCard({ member }: { member: Member }) {
  const updateMember = useUpdateMember(member.id)
  const [generalError, setGeneralError] = useState<string | null>(null)
  const toast = useToast()
  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors, isSubmitting },
  } = useForm<EditMemberForm>({
    defaultValues: { displayName: member.displayName ?? '', role: member.role },
  })

  const role = useWatch({ control, name: 'role' })

  const onSubmit = handleSubmit(async (values) => {
    setGeneralError(null)
    try {
      await updateMember.mutateAsync({ displayName: values.displayName.trim(), role: values.role })
      toast.success(fr.members.saved)
    } catch (error) {
      setGeneralError(
        applyApiErrors(error, setError, ['displayName', 'role'], { LAST_ADMIN: 'role' }),
      )
    }
  })

  return (
    <Card>
      <CardHeader>
        <CardTitle className="truncate">{fr.members.editTitle}</CardTitle>
        <CardDescription className="truncate">{member.email}</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
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
            {isSubmitting ? fr.members.saving : fr.members.save}
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}

function RemoveMemberCard({ member }: { member: Member }) {
  const deleteMember = useDeleteMember(member.id)
  const navigate = useNavigate()
  const toast = useToast()
  const [confirming, setConfirming] = useState(false)
  const { reset } = deleteMember
  const close = useCallback(() => {
    setConfirming(false)
    reset()
  }, [reset])

  const remove = async () => {
    try {
      await deleteMember.mutateAsync()
      toast.success(fr.members.removed(member.email))
      void navigate('/admin/membres')
    } catch {
      // l'erreur est affichée dans le dialogue
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">{fr.members.remove}</CardTitle>
        <CardDescription>{fr.members.removeDescription}</CardDescription>
      </CardHeader>
      <CardContent>
        <Button variant="destructive-outline" className="w-full sm:w-auto" onClick={() => setConfirming(true)}>
          {fr.members.remove}
        </Button>
      </CardContent>
      <ConfirmDialog
        open={confirming}
        title={fr.members.remove}
        description={fr.members.removeConfirm(member.email)}
        confirmLabel={deleteMember.isPending ? fr.members.removing : fr.members.removeConfirmButton}
        cancelLabel={fr.members.cancel}
        pending={deleteMember.isPending}
        error={deleteMember.isError && <Alert variant="destructive">{errorMessage(deleteMember.error)}</Alert>}
        onConfirm={() => void remove()}
        onCancel={close}
      />
    </Card>
  )
}
