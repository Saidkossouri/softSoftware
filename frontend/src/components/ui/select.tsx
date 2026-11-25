import { ChevronDown } from 'lucide-react'
import type { ComponentProps } from 'react'
import { cn } from '@/lib/utils'
import { fieldClasses } from './input'

// Liste déroulante native au style shadcn/ui : sélecteur du système sur mobile, sans Radix.
export function Select({ className, ...props }: ComponentProps<'select'>) {
  return (
    <div className="relative">
      <select className={cn(fieldClasses, 'appearance-none pr-10', className)} {...props} />
      <ChevronDown
        aria-hidden
        className="pointer-events-none absolute top-1/2 right-3.5 size-4 -translate-y-1/2 text-muted-foreground"
      />
    </div>
  )
}
