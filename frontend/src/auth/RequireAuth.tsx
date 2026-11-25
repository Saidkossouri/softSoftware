import type { ReactNode } from 'react'
import { BrandMark } from '@/components/BrandMark'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { fr } from '@/i18n/fr'
import { LoginPage } from '@/pages/LoginPage'
import { useAuth } from './AuthContext'

/** Affiche l'écran de connexion tant qu'aucun membre autorisé n'est connecté (FR-006). */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { state } = useAuth()

  switch (state.status) {
    case 'loading':
      return (
        <div className="flex min-h-svh flex-col items-center justify-center gap-4">
          <BrandMark showName={false} className="animate-pulse" />
          <p className="text-sm text-muted-foreground">{fr.loading}</p>
        </div>
      )
    case 'anonymous':
      return <LoginPage />
    case 'revoked':
      return (
        <CenteredCard title={fr.revoked.title} description={fr.revoked.body}>
          <a href="/" className="text-sm font-semibold text-primary hover:underline">
            {fr.retry}
          </a>
        </CenteredCard>
      )
    case 'error':
      return (
        <CenteredCard title={fr.appName} description={fr.unexpectedError}>
          <Button onClick={state.retry}>{fr.retry}</Button>
        </CenteredCard>
      )
    case 'authenticated':
      return <>{children}</>
  }
}

export function CenteredCard({
  title,
  description,
  children,
}: {
  title: string
  description: string
  children?: ReactNode
}) {
  return (
    <main className="flex min-h-svh flex-col items-center justify-center gap-8 p-4">
      <BrandMark className="animate-in" />
      <Card className="animate-in w-full max-w-sm">
        <CardHeader className="gap-2">
          <CardTitle className="text-2xl">{title}</CardTitle>
          <CardDescription>{description}</CardDescription>
        </CardHeader>
        {children && <CardContent>{children}</CardContent>}
      </Card>
    </main>
  )
}
