import { Link } from 'react-router'
import { buttonVariants } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { fr } from '@/i18n/fr'

/** Page « Accès refusé » interne, pour une page réservée aux ADMIN (le serveur refuse aussi). */
export function ForbiddenPage() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>{fr.forbidden.title}</CardTitle>
        <CardDescription>{fr.forbidden.body}</CardDescription>
      </CardHeader>
      <CardContent>
        <Link to="/" className={buttonVariants({ variant: 'outline' })}>
          {fr.forbidden.back}
        </Link>
      </CardContent>
    </Card>
  )
}
