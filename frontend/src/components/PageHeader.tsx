import { ChevronLeft } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { cn } from '@/lib/utils'

/** En-tête de page (charte v2) : titre, description, action principale. */
export function PageHeader({
  title,
  description,
  action,
  className,
}: {
  title: string
  description?: string
  action?: ReactNode
  className?: string
}) {
  return (
    <div className={cn('flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between', className)}>
      <div className="min-w-0">
        <h1 className="truncate text-2xl font-bold sm:text-3xl">{title}</h1>
        {description && <p className="mt-1 text-sm text-muted-foreground">{description}</p>}
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  )
}

/** Lien de retour vers la page parente. */
export function BackLink({ to, children }: { to: string; children: string }) {
  return (
    <Link
      to={to}
      className="-ml-2 inline-flex h-10 items-center gap-1 self-start rounded-lg pr-3 pl-1 text-sm font-medium text-muted-foreground outline-none hover:text-foreground focus-visible:ring-[3px] focus-visible:ring-ring/50"
    >
      <ChevronLeft aria-hidden className="size-5" />
      {children}
    </Link>
  )
}

/** Petite étiquette de section, en majuscules. */
export function SectionLabel({ children, className }: { children: string; className?: string }) {
  return (
    <p className={cn('text-xs font-semibold tracking-wider text-muted-foreground uppercase', className)}>{children}</p>
  )
}
