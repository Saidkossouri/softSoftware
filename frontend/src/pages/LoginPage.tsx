import { GOOGLE_LOGIN_URL } from '@/api/client'
import { CenteredCard } from '@/auth/RequireAuth'
import { buttonVariants } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { fr } from '@/i18n/fr'

/** Écran de connexion : la connexion est entièrement déléguée à Google (FR-001). */
export function LoginPage() {
  return (
    <CenteredCard title={fr.login.title} description={fr.login.subtitle}>
      <div className="flex flex-col gap-4">
        <a href={GOOGLE_LOGIN_URL} className={cn(buttonVariants({ size: 'lg' }), 'w-full')}>
          <GoogleIcon />
          {fr.login.button}
        </a>
        <p className="text-center text-xs text-muted-foreground">{fr.login.help}</p>
      </div>
    </CenteredCard>
  )
}

/** « G » de Google en blanc, sur le bouton bleu (pas de couleur en dur). */
function GoogleIcon() {
  return (
    <svg aria-hidden viewBox="0 0 24 24" className="size-5 fill-current">
      <path d="M21.6 12.23c0-.7-.06-1.37-.18-2.02H12v3.83h5.38a4.6 4.6 0 0 1-2 3.02v2.5h3.24c1.9-1.75 2.98-4.32 2.98-7.33ZM12 22c2.7 0 4.96-.9 6.62-2.43l-3.24-2.5c-.9.6-2.04.95-3.38.95-2.6 0-4.8-1.76-5.59-4.12H3.07v2.6A10 10 0 0 0 12 22Zm-5.59-8.1a6 6 0 0 1 0-3.8V7.5H3.07a10 10 0 0 0 0 9l3.34-2.6ZM12 5.98c1.47 0 2.79.5 3.83 1.5l2.87-2.87A9.6 9.6 0 0 0 12 2a10 10 0 0 0-8.93 5.5l3.34 2.6C7.2 7.74 9.4 5.98 12 5.98Z" />
    </svg>
  )
}
