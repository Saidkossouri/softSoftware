import { CircleAlert, Info } from 'lucide-react'
import type { ComponentProps } from 'react'
import { cn } from '@/lib/utils'

// Composant shadcn/ui, charte v2 : bandeau avec icône.
export function Alert({
  className,
  variant = 'default',
  children,
  ...props
}: ComponentProps<'div'> & { variant?: 'default' | 'destructive' }) {
  const Icon = variant === 'destructive' ? CircleAlert : Info
  return (
    <div
      role="alert"
      className={cn(
        'flex gap-3 rounded-xl px-4 py-3 text-sm',
        variant === 'destructive' ? 'bg-destructive-soft text-destructive' : 'bg-accent text-accent-foreground',
        className,
      )}
      {...props}
    >
      <Icon aria-hidden className="mt-0.5 size-4 shrink-0" />
      <div className="flex min-w-0 flex-1 flex-col items-start gap-3">{children}</div>
    </div>
  )
}
