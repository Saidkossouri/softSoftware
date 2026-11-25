import { cn } from '@/lib/utils'

export type IllustrationName = 'garage' | 'archive' | 'people' | 'locked'

/**
 * Illustrations des états vides (charte v2), dessinées avec les jetons de couleur pour suivre
 * les thèmes clair et sombre. Décoratives : masquées aux lecteurs d'écran.
 */
export function Illustration({ name, className }: { name: IllustrationName; className?: string }) {
  return (
    <svg
      aria-hidden
      viewBox="0 0 160 96"
      fill="none"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={cn('text-primary', className)}
    >
      <ellipse cx="80" cy="86" rx="64" ry="6" className="fill-muted" />
      <circle cx="132" cy="18" r="5" className="fill-warning-soft stroke-warning" strokeWidth="2" />
      <circle cx="26" cy="26" r="3" className="fill-accent" />
      {name === 'garage' && (
        <g strokeWidth="3" className="stroke-current">
          <path d="M34 82V40l46-22 46 22v42" className="fill-card" />
          <path d="M48 82V54h64v28" className="fill-accent" />
          <path d="M56 76v-6l5-8h38l5 8v6M54 76h52" />
          <circle cx="64" cy="77" r="3" className="fill-card" />
          <circle cx="96" cy="77" r="3" className="fill-card" />
        </g>
      )}
      {name === 'archive' && (
        <g strokeWidth="3" className="stroke-current">
          <rect x="42" y="26" width="76" height="16" rx="4" className="fill-accent" />
          <path d="M48 42v38a4 4 0 0 0 4 4h56a4 4 0 0 0 4-4V42" className="fill-card" />
          <path d="M70 56h20" />
        </g>
      )}
      {name === 'people' && (
        <g strokeWidth="3" className="stroke-current">
          <circle cx="66" cy="38" r="12" className="fill-accent" />
          <path d="M42 82a24 24 0 0 1 48 0" className="fill-card" />
          <circle cx="102" cy="44" r="9" className="fill-card" />
          <path d="M92 64a18 18 0 0 1 28 18" />
        </g>
      )}
      {name === 'locked' && (
        <g strokeWidth="3" className="stroke-current">
          <rect x="52" y="44" width="56" height="40" rx="8" className="fill-accent" />
          <path d="M64 44V34a16 16 0 0 1 32 0v10" />
          <circle cx="80" cy="62" r="4" className="fill-card" />
          <path d="M80 66v6" />
        </g>
      )}
    </svg>
  )
}
