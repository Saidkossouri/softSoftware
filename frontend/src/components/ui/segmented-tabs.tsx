import { cn } from '@/lib/utils'

/** Onglets en contrôle segmenté (charte v2) : fond neutre, onglet actif blanc avec ombre. */
export function SegmentedTabs<T extends string>({
  label,
  value,
  options,
  onChange,
  className,
}: {
  label: string
  value: T
  options: readonly { value: T; label: string }[]
  onChange: (value: T) => void
  className?: string
}) {
  return (
    <div
      role="tablist"
      aria-label={label}
      className={cn('grid auto-cols-fr grid-flow-col gap-1 rounded-xl bg-muted p-1', className)}
    >
      {options.map((option) => {
        const selected = option.value === value
        return (
          <button
            key={option.value}
            type="button"
            role="tab"
            aria-selected={selected}
            onClick={() => onChange(option.value)}
            className={cn(
              'h-10 rounded-lg px-4 text-sm font-medium transition-[color,background-color,box-shadow] duration-150 outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50',
              selected ? 'bg-card font-semibold text-foreground shadow-card' : 'text-muted-foreground hover:text-foreground',
            )}
          >
            {option.label}
          </button>
        )
      })}
    </div>
  )
}
