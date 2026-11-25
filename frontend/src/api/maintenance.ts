import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, unwrap, type MaintenanceRecordInput, type MaintenanceTypeCategory } from './client'

const recordKey = (recordId: string) => ['maintenance-records', recordId] as const

/** Types actifs du catalogue, dans l'ordre d'affichage. */
export function useMaintenanceTypes(category?: MaintenanceTypeCategory) {
  return useQuery({
    queryKey: ['maintenance-types', category ?? 'ALL'],
    queryFn: async () => unwrap(await api.GET('/maintenance-types', { params: { query: { category } } })),
    staleTime: 5 * 60 * 1000,
  })
}

export function useMaintenanceRecord(recordId: string) {
  return useQuery({
    queryKey: recordKey(recordId),
    queryFn: async () =>
      unwrap(await api.GET('/maintenance-records/{recordId}', { params: { path: { recordId } } })),
    retry: false,
  })
}

/** Un entretien change le kilométrage actuel (véhicules), l'historique et l'entretien lui-même. */
function useInvalidateMaintenance() {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: ['vehicles'] }),
      queryClient.invalidateQueries({ queryKey: ['history'] }),
      queryClient.invalidateQueries({ queryKey: ['maintenance-records'] }),
    ])
}

export function useCreateMaintenanceRecord(vehicleId: string) {
  const invalidate = useInvalidateMaintenance()
  return useMutation({
    mutationFn: async (body: MaintenanceRecordInput) =>
      unwrap(
        await api.POST('/vehicles/{vehicleId}/maintenance-records', { params: { path: { vehicleId } }, body }),
      ),
    onSuccess: invalidate,
  })
}

export function useUpdateMaintenanceRecord(recordId: string) {
  const invalidate = useInvalidateMaintenance()
  return useMutation({
    mutationFn: async (body: MaintenanceRecordInput) =>
      unwrap(await api.PUT('/maintenance-records/{recordId}', { params: { path: { recordId } }, body })),
    onSuccess: invalidate,
  })
}

export function useDeleteMaintenanceRecord() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (recordId: string) =>
      unwrap(await api.DELETE('/maintenance-records/{recordId}', { params: { path: { recordId } } })),
    onSuccess: (_data, recordId) => {
      queryClient.removeQueries({ queryKey: recordKey(recordId) })
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: ['vehicles'] }),
        queryClient.invalidateQueries({ queryKey: ['history'] }),
      ])
    },
  })
}
