import { cn } from '@/lib/utils'

/** Immatriculation au style plaque française (bandeau bleu « F »). */
export function Plate({ registration, className }: { registration: string; className?: string }) {
  return (
    <span
      className={cn(
        'inline-flex items-stretch overflow-hidden rounded-md border-[1.5px] border-foreground/80 bg-card font-display text-sm leading-none font-semibold tracking-wider',
        className,
      )}
    >
      <span aria-hidden className="flex items-end bg-primary px-1 pb-0.5 text-[10px] text-primary-foreground">
        F
      </span>
      <span className="px-2 py-1">{registration}</span>
    </span>
  )
}
