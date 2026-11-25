import { CarFront } from 'lucide-react'
import { fr } from '@/i18n/fr'
import { cn } from '@/lib/utils'

/** Logo de l'application : pastille bleue et nom. */
export function BrandMark({ className, showName = true }: { className?: string; showName?: boolean }) {
  return (
    <span className={cn('inline-flex items-center gap-2.5', className)}>
      <span
        aria-hidden
        className="flex size-9 items-center justify-center rounded-xl bg-primary text-primary-foreground shadow-primary"
      >
        <CarFront className="size-5" />
      </span>
      {showName && <span className="font-display text-lg font-bold tracking-tight">{fr.appName}</span>}
    </span>
  )
}
