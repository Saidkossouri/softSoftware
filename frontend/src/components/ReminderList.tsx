import { CircleCheck, Gauge } from 'lucide-react'
import { useState } from 'react'
import type { Vehicle } from '@/api/client'
import { useReminders } from '@/api/schedule'
import { DueItemCard } from '@/components/DueItemCard'
import { errorMessage } from '@/components/FormErrors'
import { IconTile, ListCard } from '@/components/ListCard'
import { MileageReadingDialog } from '@/components/MileageReadingDialog'
import { SectionLabel } from '@/components/PageHeader'
import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { fr } from '@/i18n/fr'
import { formatDate, formatKm } from '@/lib/format'

/**
 * Rappels de l'accueil (US4, FR-028, FR-035, SC-008) : échéances en retard ou proches de tous les
 * véhicules accessibles, et relevés de kilométrage oubliés depuis plus de 30 jours.
 */
export function ReminderList({ vehicles }: { vehicles: Vehicle[] }) {
  const reminders = useReminders()
  const [recording, setRecording] = useState<Vehicle | null>(null)

  if (reminders.isPending) {
    return (
      <div aria-busy="true">
        <p className="sr-only">{fr.loading}</p>
        <Skeleton className="h-20" />
      </div>
    )
  }
  if (reminders.isError) {
    return (
      <Alert variant="destructive">
        {errorMessage(reminders.error)}
        <Button variant="outline" size="sm" onClick={() => void reminders.refetch()}>
          {fr.retry}
        </Button>
      </Alert>
    )
  }

  const { dueItems, mileageReadings } = reminders.data
  if (dueItems.length === 0 && mileageReadings.length === 0) {
    return (
      <ul aria-label={fr.reminders.title}>
        <ListCard
          leading={
            <IconTile className="bg-success-soft text-success">
              <CircleCheck />
            </IconTile>
          }
          title={fr.reminders.allGood}
          subtitle={fr.reminders.allGoodBody}
        />
      </ul>
    )
  }

  return (
    <section className="flex flex-col gap-3" aria-labelledby="reminders-title">
      <SectionLabel id="reminders-title">{fr.reminders.title}</SectionLabel>
      <ul className="flex flex-col gap-3">
        {dueItems.map(({ vehicleId, vehicleNickname, item }) => (
          <DueItemCard
            key={`${vehicleId}-${item.ruleId ?? item.deadlineId}`}
            item={item}
            context={vehicleNickname}
            to={`/vehicules/${vehicleId}`}
          />
        ))}
        {mileageReadings.map((reminder) => {
          const vehicle = vehicles.find((candidate) => candidate.id === reminder.vehicleId)
          return (
            <ListCard
              key={reminder.vehicleId}
              wrap
              leading={
                <IconTile className="bg-warning-soft text-warning">
                  <Gauge />
                </IconTile>
              }
              title={<span className="text-pretty">{fr.reminders.mileageTitle(reminder.vehicleNickname)}</span>}
              subtitle={fr.reminders.mileageBody(
                formatKm(reminder.currentMileage.mileageKm),
                formatDate(reminder.currentMileage.date),
              )}
              trailing={
                vehicle && (
                  <Button
                    variant="outline"
                    size="sm"
                    aria-label={fr.reminders.mileageTitle(reminder.vehicleNickname)}
                    onClick={() => setRecording(vehicle)}
                  >
                    {fr.reminders.record}
                  </Button>
                )
              }
            />
          )
        })}
      </ul>
      {recording && <MileageReadingDialog vehicle={recording} onClose={() => setRecording(null)} />}
    </section>
  )
}
