import type { ComponentProps } from 'react'
import { cn } from '@/lib/utils'

// Liste déroulante native au style shadcn/ui : sélecteur du système sur mobile, sans Radix.
export function Select({ className, ...props }: ComponentProps<'select'>) {
  return (
    <select
      className={cn(
        'flex h-10 w-full min-w-0 rounded-md border bg-card px-3 py-2 text-base focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 aria-invalid:border-destructive sm:text-sm',
        className,
      )}
      {...props}
    />
  )
}
