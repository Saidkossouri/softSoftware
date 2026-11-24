import type { ComponentProps } from 'react'
import { cn } from '@/lib/utils'

// Composant shadcn/ui.
export function Alert({
  className,
  variant = 'default',
  ...props
}: ComponentProps<'div'> & { variant?: 'default' | 'destructive' }) {
  return (
    <div
      role="alert"
      className={cn(
        'rounded-lg border px-4 py-3 text-sm',
        variant === 'destructive' && 'border-destructive/40 text-destructive',
        className,
      )}
      {...props}
    />
  )
}
