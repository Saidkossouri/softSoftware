import { GOOGLE_LOGIN_URL } from '@/api/client'
import { CenteredCard } from '@/auth/RequireAuth'
import { buttonVariants } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { fr } from '@/i18n/fr'

/** Écran de connexion : la connexion est entièrement déléguée à Google (FR-001). */
export function LoginPage() {
  return (
    <CenteredCard title={fr.login.title} description={fr.login.subtitle}>
      <div className="flex flex-col gap-3">
        <a href={GOOGLE_LOGIN_URL} className={cn(buttonVariants({ size: 'lg' }), 'w-full')}>
          {fr.login.button}
        </a>
        <p className="text-xs text-muted-foreground">{fr.login.help}</p>
      </div>
    </CenteredCard>
  )
}
