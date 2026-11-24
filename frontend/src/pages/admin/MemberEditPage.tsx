import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { Link, useNavigate, useParams } from 'react-router'
import type { Member, Role } from '@/api/client'
import { useDeleteMember, useMember, useUpdateMember } from '@/api/members'
import { applyApiErrors, errorMessage, FormAlert, FormField } from '@/components/FormErrors'
import { Alert } from '@/components/ui/alert'
import { Button, buttonVariants } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { fr } from '@/i18n/fr'
import { lastLoginLabel } from './memberFormat'
import { RoleSelect } from './RoleSelect'

type EditMemberForm = { displayName: string; role: Role }

/** Modification du nom et du rôle, retrait de l'accès (US2, FR-010, FR-012). */
export function MemberEditPage() {
  const { memberId = '' } = useParams()
  const member = useMember(memberId)

  return (
    <div className="flex flex-col gap-4">
      <Link to="/admin/membres" className={buttonVariants({ variant: 'ghost', className: 'self-start' })}>
        ← {fr.members.back}
      </Link>
      {member.isPending && <p className="text-muted-foreground">{fr.loading}</p>}
      {member.isError && (
        <Alert variant="destructive" className="flex flex-col items-start gap-3">
          {errorMessage(member.error)}
          <Button variant="outline" size="sm" onClick={() => void member.refetch()}>
            {fr.retry}
          </Button>
        </Alert>
      )}
      {member.isSuccess && (
        <>
          <EditMemberCard key={member.data.id} member={member.data} />
          <RemoveMemberCard member={member.data} />
        </>
      )}
    </div>
  )
}

function EditMemberCard({ member }: { member: Member }) {
  const updateMember = useUpdateMember(member.id)
  const [generalError, setGeneralError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)
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
    setSaved(false)
    try {
      await updateMember.mutateAsync({ displayName: values.displayName.trim(), role: values.role })
      setSaved(true)
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
        <CardDescription>{lastLoginLabel(member)}</CardDescription>
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
          {saved && <Alert role="status">{fr.members.saved}</Alert>}
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
  const [confirming, setConfirming] = useState(false)

  const remove = async () => {
    try {
      await deleteMember.mutateAsync()
      void navigate('/admin/membres')
    } catch {
      setConfirming(false)
    }
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-5">
        {deleteMember.isError && <Alert variant="destructive">{errorMessage(deleteMember.error)}</Alert>}
        {confirming ? (
          <>
            <p className="text-sm">{fr.members.removeConfirm(member.email)}</p>
            <div className="flex flex-col gap-2 sm:flex-row">
              <Button variant="destructive" disabled={deleteMember.isPending} onClick={() => void remove()}>
                {deleteMember.isPending ? fr.members.removing : fr.members.removeConfirmButton}
              </Button>
              <Button variant="outline" onClick={() => setConfirming(false)}>
                {fr.members.cancel}
              </Button>
            </div>
          </>
        ) : (
          <Button variant="outline" className="text-destructive sm:self-start" onClick={() => setConfirming(true)}>
            {fr.members.remove}
          </Button>
        )}
      </CardContent>
    </Card>
  )
}
