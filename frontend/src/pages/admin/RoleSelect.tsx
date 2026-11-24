import type { UseFormRegisterReturn } from 'react-hook-form'
import type { Role } from '@/api/client'
import { FormField } from '@/components/FormErrors'
import { Select } from '@/components/ui/select'
import { fr } from '@/i18n/fr'

/** Choix du rôle ADMIN / USER, avec une aide qui explique ce que le rôle permet. */
export function RoleSelect({
  id,
  error,
  register,
  value,
}: {
  id: string
  error?: string
  register: UseFormRegisterReturn<'role'>
  value?: Role
}) {
  return (
    <FormField id={id} label={fr.members.role} error={error} hint={value ? fr.roleHelp[value] : undefined}>
      <Select id={id} aria-invalid={error ? true : undefined} {...register}>
        <option value="USER">{fr.roles.USER}</option>
        <option value="ADMIN">{fr.roles.ADMIN}</option>
      </Select>
    </FormField>
  )
}
