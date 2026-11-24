import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, unwrap, type VehicleInput, type VehicleStatus } from './client'

const VEHICLES_KEY = ['vehicles'] as const
const vehicleListKey = (status: VehicleStatus) => ['vehicles', 'list', status] as const
const vehicleKey = (id: string) => ['vehicles', 'detail', id] as const

/** Véhicules d'un statut, triés par surnom par le serveur. */
export function useVehicles(status: VehicleStatus = 'ACTIVE') {
  return useQuery({
    queryKey: vehicleListKey(status),
    queryFn: async () => unwrap(await api.GET('/vehicles', { params: { query: { status } } })),
  })
}

export function useVehicle(vehicleId: string) {
  return useQuery({
    queryKey: vehicleKey(vehicleId),
    queryFn: async () =>
      unwrap(await api.GET('/vehicles/{vehicleId}', { params: { path: { vehicleId } } })),
    retry: false,
  })
}

/** Après chaque modification : toutes les listes et tous les détails de véhicules. */
function useInvalidateVehicles() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: VEHICLES_KEY })
}

export function useCreateVehicle() {
  const invalidate = useInvalidateVehicles()
  return useMutation({
    mutationFn: async (body: VehicleInput) => unwrap(await api.POST('/vehicles', { body })),
    onSuccess: invalidate,
  })
}

export function useUpdateVehicle(vehicleId: string) {
  const invalidate = useInvalidateVehicles()
  return useMutation({
    mutationFn: async (body: VehicleInput) =>
      unwrap(await api.PUT('/vehicles/{vehicleId}', { params: { path: { vehicleId } }, body })),
    onSuccess: invalidate,
  })
}

export function useArchiveVehicle(vehicleId: string) {
  const invalidate = useInvalidateVehicles()
  return useMutation({
    mutationFn: async () =>
      unwrap(await api.POST('/vehicles/{vehicleId}/archive', { params: { path: { vehicleId } } })),
    onSuccess: invalidate,
  })
}

/** Réactivation depuis la liste des archivés : l'identifiant est donné à l'appel. */
export function useReactivateVehicle() {
  const invalidate = useInvalidateVehicles()
  return useMutation({
    mutationFn: async (vehicleId: string) =>
      unwrap(await api.POST('/vehicles/{vehicleId}/reactivate', { params: { path: { vehicleId } } })),
    onSuccess: invalidate,
  })
}
