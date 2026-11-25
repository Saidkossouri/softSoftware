import { X } from 'lucide-react'
import { useEffect, useId, useRef, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { Button } from '@/components/ui/button'

/**
 * Feuille de saisie (charte v2) : en bas de l'écran sur mobile, centrée sur desktop. Fermeture
 * par Échap, par le fond ou par la croix ; le focus va au premier champ puis revient à l'appelant.
 */
export function Sheet({
  title,
  description,
  closeLabel,
  onClose,
  children,
}: {
  title: string
  description?: string
  closeLabel: string
  onClose: () => void
  children: ReactNode
}) {
  const titleId = useId()
  const descriptionId = useId()
  const panelRef = useRef<HTMLDivElement>(null)
  const onCloseRef = useRef(onClose)

  useEffect(() => {
    onCloseRef.current = onClose
  }, [onClose])

  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null
    panelRef.current?.querySelector<HTMLElement>('input, select, textarea')?.focus()
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onCloseRef.current()
    }
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('keydown', onKey)
      previous?.focus()
    }
  }, [])

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center sm:p-4">
      <div aria-hidden className="absolute inset-0 bg-foreground/40 backdrop-blur-[2px]" onClick={onClose} />
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={description ? descriptionId : undefined}
        className="animate-in relative flex max-h-full w-full flex-col gap-4 overflow-y-auto rounded-t-2xl bg-card p-6 text-card-foreground shadow-card sm:max-w-md sm:rounded-2xl"
      >
        <div className="flex items-start gap-3">
          <div className="min-w-0 flex-1">
            <h2 id={titleId} className="font-display text-lg font-semibold">
              {title}
            </h2>
            {description && (
              <p id={descriptionId} className="text-sm text-muted-foreground">
                {description}
              </p>
            )}
          </div>
          <Button variant="ghost" size="icon" aria-label={closeLabel} onClick={onClose}>
            <X aria-hidden />
          </Button>
        </div>
        {children}
      </div>
    </div>,
    document.body,
  )
}
