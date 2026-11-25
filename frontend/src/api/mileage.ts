import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api, unwrap, type MileageReadingInput } from './client'

/** Un relevé change le kilométrage actuel (véhicules) et l'historique du véhicule. */
function useInvalidateMileage() {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: ['vehicles'] }),
      queryClient.invalidateQueries({ queryKey: ['history'] }),
    ])
}

export function useCreateMileageReading(vehicleId: string) {
  const invalidate = useInvalidateMileage()
  return useMutation({
    mutationFn: async (body: MileageReadingInput) =>
      unwrap(
        await api.POST('/vehicles/{vehicleId}/mileage-readings', { params: { path: { vehicleId } }, body }),
      ),
    onSuccess: invalidate,
  })
}

export function useUpdateMileageReading() {
  const invalidate = useInvalidateMileage()
  return useMutation({
    mutationFn: async ({ readingId, body }: { readingId: string; body: MileageReadingInput }) =>
      unwrap(await api.PUT('/mileage-readings/{readingId}', { params: { path: { readingId } }, body })),
    onSuccess: invalidate,
  })
}

export function useDeleteMileageReading() {
  const invalidate = useInvalidateMileage()
  return useMutation({
    mutationFn: async (readingId: string) =>
      unwrap(await api.DELETE('/mileage-readings/{readingId}', { params: { path: { readingId } } })),
    onSuccess: invalidate,
  })
}
