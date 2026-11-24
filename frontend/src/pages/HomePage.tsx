import { Card, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { fr } from '@/i18n/fr'

/** Accueil provisoire, remplacé par « Mes véhicules » avec l'histoire 4. */
export function HomePage() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>{fr.home.title}</CardTitle>
        <CardDescription>{fr.home.body}</CardDescription>
      </CardHeader>
    </Card>
  )
}
