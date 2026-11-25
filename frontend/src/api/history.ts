import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { api, unwrap } from './client'

export type HistoryFilters = { typeId?: string; recordsOnly?: boolean }

/** Historique d'un véhicule : kilométrage actuel, totaux de dépenses et saisies (US3). */
export function useVehicleHistory(vehicleId: string, filters: HistoryFilters = {}, enabled = true) {
  return useQuery({
    queryKey: ['history', vehicleId, filters.typeId ?? null, filters.recordsOnly ?? false],
    queryFn: async () =>
      unwrap(
        await api.GET('/vehicles/{vehicleId}/history', {
          params: {
            path: { vehicleId },
            query: { typeId: filters.typeId, recordsOnly: filters.recordsOnly || undefined },
          },
        }),
      ),
    placeholderData: keepPreviousData,
    retry: false,
    enabled,
  })
}
