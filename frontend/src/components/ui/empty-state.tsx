import type { ReactNode } from 'react'
import { Illustration, type IllustrationName } from '@/components/Illustration'
import { cn } from '@/lib/utils'

/** État vide illustré (charte v2) : illustration, titre, explication et action éventuelle. */
export function EmptyState({
  illustration,
  title,
  description,
  action,
  className,
}: {
  illustration: IllustrationName
  title: string
  description?: string
  action?: ReactNode
  className?: string
}) {
  return (
    <div className={cn('flex flex-col items-center gap-3 px-4 py-10 text-center', className)}>
      <Illustration name={illustration} className="mb-2 h-24 w-40" />
      <p className="font-display text-lg font-semibold">{title}</p>
      {description && <p className="max-w-sm text-sm text-muted-foreground">{description}</p>}
      {action && <div className="mt-2 w-full sm:w-auto">{action}</div>}
    </div>
  )
}
