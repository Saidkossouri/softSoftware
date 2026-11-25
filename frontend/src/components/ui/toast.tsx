import { CircleCheck, X } from 'lucide-react'
import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from 'react'
import { fr } from '@/i18n/fr'

type Toast = { id: number; message: string }
type ToastApi = { success: (message: string) => void }

const ToastContext = createContext<ToastApi | null>(null)
const DURATION_MS = 4000

/** Toasts de succès (charte v2) : en bas de l'écran, au-dessus de la navigation mobile. */
export function Toaster({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const nextId = useRef(0)

  const dismiss = useCallback((id: number) => setToasts((current) => current.filter((t) => t.id !== id)), [])
  const success = useCallback(
    (message: string) => {
      const id = nextId.current++
      setToasts((current) => [...current.slice(-2), { id, message }])
      window.setTimeout(() => dismiss(id), DURATION_MS)
    },
    [dismiss],
  )
  const api = useMemo(() => ({ success }), [success])

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div
        role="status"
        aria-live="polite"
        className="pointer-events-none fixed inset-x-0 bottom-24 z-50 flex flex-col items-center gap-2 px-4 sm:bottom-6"
      >
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className="animate-in pointer-events-auto flex w-full max-w-sm items-center gap-3 rounded-xl bg-foreground py-2 pr-2 pl-4 text-sm font-medium text-background shadow-card"
          >
            <CircleCheck aria-hidden className="size-5 shrink-0 text-success-soft" />
            <p className="min-w-0 flex-1">{toast.message}</p>
            <button
              type="button"
              aria-label={fr.closeNotification}
              onClick={() => dismiss(toast.id)}
              className="flex size-9 shrink-0 items-center justify-center rounded-lg opacity-70 outline-none hover:opacity-100 focus-visible:ring-[3px] focus-visible:ring-ring/50"
            >
              <X aria-hidden className="size-4" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast(): ToastApi {
  const api = useContext(ToastContext)
  if (!api) throw new Error('useToast doit être utilisé sous <Toaster>')
  return api
}
