import type { ReactNode } from 'react'
import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { ApiError } from '@/api/client'
import { Alert } from '@/components/ui/alert'
import { Label } from '@/components/ui/label'
import { fr } from '@/i18n/fr'

/** Message lisible pour une erreur d'API (le serveur renvoie déjà un texte en français). */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError && error.problem.detail) {
    return error.problem.detail
  }
  return fr.unexpectedError
}

/**
 * Reporte les erreurs de l'API sur le formulaire : erreurs par champ (`fieldErrors`) sous chaque
 * champ, sinon message général. La saisie n'est jamais réinitialisée.
 * @return le message général à afficher, ou `null` si tout a été placé sous les champs
 */
export function applyApiErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  fields: readonly Path<T>[],
  fieldForCode: Partial<Record<string, Path<T>>> = {},
): string | null {
  if (!(error instanceof ApiError)) {
    return fr.unexpectedError
  }
  const codeField = fieldForCode[error.code]
  if (codeField) {
    setError(codeField, { type: 'server', message: error.problem.detail })
    return null
  }
  const placed = (error.problem.fieldErrors ?? []).filter((fieldError) =>
    (fields as readonly string[]).includes(fieldError.field),
  )
  placed.forEach((fieldError) =>
    setError(fieldError.field as Path<T>, { type: 'server', message: fieldError.message }),
  )
  return placed.length > 0 && placed.length === (error.problem.fieldErrors ?? []).length
    ? null
    : error.problem.detail
}

/** Libellé, champ et message d'erreur sous le champ (charte : erreur en text-destructive). */
export function FormField({
  id,
  label,
  error,
  hint,
  children,
}: {
  id: string
  label: string
  error?: string
  hint?: string
  children: ReactNode
}) {
  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor={id}>{label}</Label>
      {children}
      {hint && !error && <p className="text-sm text-muted-foreground">{hint}</p>}
      {error && (
        <p id={`${id}-error`} className="text-sm text-destructive">
          {error}
        </p>
      )}
    </div>
  )
}

/** Message d'erreur général d'un formulaire. */
export function FormAlert({ message }: { message: string | null }) {
  return message ? <Alert variant="destructive">{message}</Alert> : null
}
