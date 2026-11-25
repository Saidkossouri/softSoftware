import type { ComponentProps, ReactNode } from 'react'
import { Skeleton } from '@/components/ui/skeleton'
import { fr } from '@/i18n/fr'
import { cn } from '@/lib/utils'

/** Carte d'un élément de liste (charte v2) : visuel à gauche, texte au centre, actions à droite. */
export function ListCard({
  leading,
  title,
  subtitle,
  meta,
  trailing,
  className,
  ...props
}: Omit<ComponentProps<'li'>, 'title'> & {
  leading?: ReactNode
  title: ReactNode
  subtitle?: ReactNode
  meta?: ReactNode
  trailing?: ReactNode
}) {
  return (
    <li
      className={cn(
        'flex items-center gap-3 rounded-2xl border border-border/60 bg-card p-4 shadow-card',
        className,
      )}
      {...props}
    >
      <div className="flex min-w-0 flex-1 items-center gap-3.5">
        {leading}
        <div className="min-w-0 flex-1">
          <div className="flex min-w-0 items-center gap-2 font-semibold">{title}</div>
          {subtitle && <div className="truncate text-sm text-muted-foreground">{subtitle}</div>}
          {meta && <div className="mt-2 flex flex-wrap items-center gap-2">{meta}</div>}
        </div>
      </div>
      {trailing && <div className="flex shrink-0 items-center justify-end gap-2">{trailing}</div>}
    </li>
  )
}

/** Tuile d'icône colorée. */
export function IconTile({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div
      aria-hidden
      className={cn(
        'flex size-12 shrink-0 items-center justify-center rounded-xl bg-accent text-accent-foreground [&_svg]:size-6',
        className,
      )}
    >
      {children}
    </div>
  )
}

/** Avatar à initiale. */
export function Avatar({ name, className }: { name: string; className?: string }) {
  return (
    <div
      aria-hidden
      className={cn(
        'flex size-11 shrink-0 items-center justify-center rounded-full bg-primary font-display text-base font-semibold text-primary-foreground',
        className,
      )}
    >
      {name.trim().charAt(0).toUpperCase()}
    </div>
  )
}

/** Squelette de chargement d'une liste de cartes. */
export function ListSkeleton() {
  return (
    <div className="flex flex-col gap-3" aria-busy="true">
      <p className="sr-only">{fr.loading}</p>
      {[0, 1, 2].map((index) => (
        <div key={index} className="flex items-center gap-3.5 rounded-2xl bg-card p-4 shadow-card">
          <Skeleton className="size-12" />
          <div className="flex flex-1 flex-col gap-2">
            <Skeleton className="h-4 w-1/3" />
            <Skeleton className="h-3 w-1/2" />
          </div>
        </div>
      ))}
    </div>
  )
}
