import type { ComponentProps } from 'react'
import { cn } from '@/lib/utils'

/** Case à cocher native aux couleurs de la charte (jeton `primary`), focus visible conservé. */
export function Checkbox({ className, ...props }: Omit<ComponentProps<'input'>, 'type'>) {
  return (
    <input
      type="checkbox"
      className={cn(
        'size-5 shrink-0 cursor-pointer rounded-md border-input accent-primary outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50',
        className,
      )}
      {...props}
    />
  )
}
