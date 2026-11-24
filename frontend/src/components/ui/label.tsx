import type { ComponentProps } from 'react'
import { cn } from '@/lib/utils'

// Composant shadcn/ui (version simple, sans Radix).
export function Label({ className, ...props }: ComponentProps<'label'>) {
  return <label className={cn('text-sm font-medium leading-none', className)} {...props} />
}
