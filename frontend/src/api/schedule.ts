import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { api, unwrap, type DeadlineInput, type MaintenanceRuleInput, type MaintenanceRuleUpdate } from './client'

/** Échéances calculées d'un véhicule (règles et échéances à date fixe), en retard d'abord. */
export function useDueItems(vehicleId: string) {
  return useQuery({
    queryKey: ['due-items', vehicleId],
    queryFn: async () =>
      unwrap(await api.GET('/vehicles/{vehicleId}/due-items', { params: { path: { vehicleId } } })),
    retry: false,
  })
}

/** Rappels de l'accueil : échéances en retard ou proches, relevés de kilométrage à faire. */
export function useReminders(enabled = true) {
  return useQuery({
    queryKey: ['reminders'],
    queryFn: async () => unwrap(await api.GET('/reminders')),
    enabled,
  })
}

export function useDeadlineSuggestions(vehicleId: string) {
  return useQuery({
    queryKey: ['deadline-suggestions', vehicleId],
    queryFn: async () =>
      unwrap(await api.GET('/vehicles/{vehicleId}/deadline-suggestions', { params: { path: { vehicleId } } })),
  })
}

/**
 * Après un relevé ou un entretien : échéances et rappels recalculés en arrière-plan, sans
 * retarder la confirmation de la saisie.
 */
export function refreshSchedule(queryClient: QueryClient) {
  void queryClient.invalidateQueries({ queryKey: ['due-items'] })
  void queryClient.invalidateQueries({ queryKey: ['reminders'] })
}

/** Toute écriture du plan change les échéances, les rappels et les suggestions. */
export function useInvalidateSchedule() {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: ['due-items'] }),
      queryClient.invalidateQueries({ queryKey: ['reminders'] }),
      queryClient.invalidateQueries({ queryKey: ['deadline-suggestions'] }),
    ])
}

export function useCreateRule(vehicleId: string) {
  const invalidate = useInvalidateSchedule()
  return useMutation({
    mutationFn: async (body: MaintenanceRuleInput) =>
      unwrap(await api.POST('/vehicles/{vehicleId}/maintenance-rules', { params: { path: { vehicleId } }, body })),
    onSuccess: invalidate,
  })
}

export function useUpdateRule() {
  const invalidate = useInvalidateSchedule()
  return useMutation({
    mutationFn: async ({ ruleId, body }: { ruleId: string; body: MaintenanceRuleUpdate }) =>
      unwrap(await api.PUT('/maintenance-rules/{ruleId}', { params: { path: { ruleId } }, body })),
    onSuccess: invalidate,
  })
}

export function useDeleteRule() {
  const invalidate = useInvalidateSchedule()
  return useMutation({
    mutationFn: async (ruleId: string) =>
      unwrap(await api.DELETE('/maintenance-rules/{ruleId}', { params: { path: { ruleId } } })),
    onSuccess: invalidate,
  })
}

export function useCreateDeadline(vehicleId: string) {
  const invalidate = useInvalidateSchedule()
  return useMutation({
    mutationFn: async (body: DeadlineInput) =>
      unwrap(await api.POST('/vehicles/{vehicleId}/deadlines', { params: { path: { vehicleId } }, body })),
    onSuccess: invalidate,
  })
}

export function useUpdateDeadline() {
  const invalidate = useInvalidateSchedule()
  return useMutation({
    mutationFn: async ({ deadlineId, body }: { deadlineId: string; body: DeadlineInput }) =>
      unwrap(await api.PUT('/deadlines/{deadlineId}', { params: { path: { deadlineId } }, body })),
    onSuccess: invalidate,
  })
}

export function useDeleteDeadline() {
  const invalidate = useInvalidateSchedule()
  return useMutation({
    mutationFn: async (deadlineId: string) =>
      unwrap(await api.DELETE('/deadlines/{deadlineId}', { params: { path: { deadlineId } } })),
    onSuccess: invalidate,
  })
}

export function useCompleteDeadline() {
  const invalidate = useInvalidateSchedule()
  return useMutation({
    mutationFn: async (deadlineId: string) =>
      unwrap(await api.POST('/deadlines/{deadlineId}/complete', { params: { path: { deadlineId } } })),
    onSuccess: invalidate,
  })
}

export function useDismissSuggestion(vehicleId: string) {
  const invalidate = useInvalidateSchedule()
  return useMutation({
    mutationFn: async (typeId: string) =>
      unwrap(
        await api.POST('/vehicles/{vehicleId}/deadline-suggestions/{typeId}/dismiss', {
          params: { path: { vehicleId, typeId } },
        }),
      ),
    onSuccess: invalidate,
  })
}
