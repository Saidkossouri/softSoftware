import { Link } from 'react-router'
import { buttonVariants } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { EmptyState } from '@/components/ui/empty-state'
import { fr } from '@/i18n/fr'

/** Page « Accès refusé » interne, pour une page réservée aux ADMIN (le serveur refuse aussi). */
export function ForbiddenPage() {
  return (
    <Card>
      <EmptyState
        illustration="locked"
        title={fr.forbidden.title}
        description={fr.forbidden.body}
        action={
          <Link to="/" className={buttonVariants({ variant: 'outline', className: 'w-full sm:w-auto' })}>
            {fr.forbidden.back}
          </Link>
        }
      />
    </Card>
  )
}
