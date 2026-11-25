import { CalendarClock, Wrench } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link } from 'react-router'
import type { DueItem } from '@/api/client'
import { IconTile, ListCard } from '@/components/ListCard'
import { Badge } from '@/components/ui/badge'
import { fr } from '@/i18n/fr'
import { formatDate, formatKm } from '@/lib/format'
import { dueText, frequencyText, itemName, remainingText, statusVariant } from '@/lib/schedule'
import { cn } from '@/lib/utils'

/**
 * Une échéance calculée (US4, FR-024 à FR-027) : opération ou libellé, état coloré, reste en
 * mots (« dans 700 km », « en retard de 12 jours »), termes et dernière réalisation.
 */
export function DueItemCard({
  item,
  context,
  actions,
  to,
  className,
}: {
  item: DueItem
  /** Rend toute la carte cliquable (vers la fiche du véhicule, sur l'accueil). */
  to?: string
  /** Ligne de contexte au-dessus du titre (le véhicule, sur l'accueil). */
  context?: string
  actions?: ReactNode
  className?: string
}) {
  const last =
    item.kind === 'RULE' && item.lastDoneDate && item.lastDoneMileageKm != null
      ? fr.schedule.lastDone(formatDate(item.lastDoneDate), formatKm(item.lastDoneMileageKm))
      : null
  return (
    <ListCard
      stack={actions !== undefined}
      wrap
      className={cn(
        to && 'relative transition-shadow duration-150 focus-within:ring-[3px] focus-within:ring-ring/50 hover:shadow-primary',
        className,
      )}
      leading={
        <IconTile
          className={cn(
            item.status === 'OVERDUE' && 'bg-destructive-soft text-destructive',
            item.status === 'SOON' && 'bg-warning-soft text-warning',
          )}
        >
          {item.kind === 'RULE' ? <Wrench /> : <CalendarClock />}
        </IconTile>
      }
      title={
        <span className="flex min-w-0 flex-col">
          {context && <span className="truncate text-xs font-medium text-muted-foreground">{context}</span>}
          {to ? (
            <Link to={to} className="truncate outline-none after:absolute after:inset-0 after:rounded-2xl">
              {itemName(item)}
            </Link>
          ) : (
            <span className="truncate">{itemName(item)}</span>
          )}
        </span>
      }
      subtitle={remainingText(item)}
      meta={
        <>
          <Badge variant={statusVariant[item.status]}>{fr.dueStatus[item.status]}</Badge>
          <span className="text-xs text-muted-foreground">{[dueText(item), frequencyText(item)].filter(Boolean).join(' · ')}</span>
          {last && <span className="w-full text-xs text-muted-foreground">{last}</span>}
        </>
      }
      trailing={actions}
    />
  )
}
