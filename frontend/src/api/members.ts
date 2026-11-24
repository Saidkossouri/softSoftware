import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, unwrap, type MemberCreateRequest, type MemberUpdateRequest } from './client'
import { CURRENT_MEMBER_KEY } from '@/auth/AuthContext'

const MEMBERS_KEY = ['members'] as const
const memberKey = (id: string) => ['members', id] as const

export function useMembers() {
  return useQuery({
    queryKey: MEMBERS_KEY,
    queryFn: async () => unwrap(await api.GET('/members')),
  })
}

export function useMember(memberId: string) {
  return useQuery({
    queryKey: memberKey(memberId),
    queryFn: async () =>
      unwrap(await api.GET('/members/{memberId}', { params: { path: { memberId } } })),
    retry: false,
  })
}

/** Après chaque modification : liste, détail et membre connecté (son propre rôle a pu changer). */
function useInvalidateMembers() {
  const queryClient = useQueryClient()
  return async () => {
    await queryClient.invalidateQueries({ queryKey: MEMBERS_KEY })
    await queryClient.invalidateQueries({ queryKey: CURRENT_MEMBER_KEY })
  }
}

export function useCreateMember() {
  const invalidate = useInvalidateMembers()
  return useMutation({
    mutationFn: async (body: MemberCreateRequest) => unwrap(await api.POST('/members', { body })),
    onSuccess: invalidate,
  })
}

export function useUpdateMember(memberId: string) {
  const invalidate = useInvalidateMembers()
  return useMutation({
    mutationFn: async (body: MemberUpdateRequest) =>
      unwrap(await api.PATCH('/members/{memberId}', { params: { path: { memberId } }, body })),
    onSuccess: invalidate,
  })
}

export function useDeleteMember(memberId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async () =>
      unwrap(await api.DELETE('/members/{memberId}', { params: { path: { memberId } } })),
    onSuccess: async () => {
      queryClient.removeQueries({ queryKey: memberKey(memberId) })
      await queryClient.invalidateQueries({ queryKey: MEMBERS_KEY })
    },
  })
}
