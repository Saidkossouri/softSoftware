import type { ReactNode } from 'react'
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
      return <p className="p-6 text-center text-muted-foreground">{fr.loading}</p>
    case 'anonymous':
      return <LoginPage />
    case 'revoked':
      return (
        <CenteredCard title={fr.revoked.title} description={fr.revoked.body}>
          <a href="/" className="text-sm font-medium text-primary">
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
    <main className="flex min-h-svh items-center justify-center p-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>{title}</CardTitle>
          <CardDescription>{description}</CardDescription>
        </CardHeader>
        {children && <CardContent>{children}</CardContent>}
      </Card>
    </main>
  )
}
