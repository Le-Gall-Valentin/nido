import { useMutation, useQueryClient } from '@tanstack/react-query'
import { SPACES_QUERY_KEY } from '@/features/space-switcher'
import type { AssignableSpaceRole, CreateSpaceInput, UpdateSpaceInput } from './ISpacesPageApi'
import { useSpacesPageApi } from './spacesPageApiContext'
import { spaceDetailKey } from './useSpaceDetail'
import { spaceMembersKey } from './useSpaceMembers'
import { spaceInvitationsKey } from './useSpaceInvitations'
import { RECEIVED_INVITATIONS_QUERY_KEY } from './useReceivedInvitations'

/**
 * One hook per write operation, each invalidating exactly the keys its
 * change affects — never more, per the task's query-key table.
 */

export function useCreateSpace() {
  const api = useSpacesPageApi()
  const queryClient = useQueryClient()
  return useMutation({
    // A new context appears in "my spaces".
    mutationFn: (input: CreateSpaceInput) => api.createSpace(input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SPACES_QUERY_KEY] }),
  })
}

export function useUpdateSpace(spaceId: string) {
  const api = useSpacesPageApi()
  const queryClient = useQueryClient()
  return useMutation({
    // Name/accent/glyph are shown both in the detail and in "my spaces".
    mutationFn: (patch: UpdateSpaceInput) => api.updateSpace(spaceId, patch),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: spaceDetailKey(spaceId) })
      queryClient.invalidateQueries({ queryKey: [SPACES_QUERY_KEY] })
    },
  })
}

export function useDeleteSpace(spaceId: string) {
  const api = useSpacesPageApi()
  const queryClient = useQueryClient()
  return useMutation({
    // The context disappears from "my spaces"; the route guard needs this to
    // notice the current URL is no longer accessible. Redirecting away from
    // it is the calling component's job, not this hook's.
    mutationFn: () => api.deleteSpace(spaceId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SPACES_QUERY_KEY] }),
  })
}

export function useChangeMemberRole(spaceId: string) {
  const api = useSpacesPageApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: AssignableSpaceRole }) =>
      api.changeMemberRole(spaceId, userId, role),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: spaceMembersKey(spaceId) }),
  })
}

export function useRemoveMember(spaceId: string) {
  const api = useSpacesPageApi()
  const queryClient = useQueryClient()
  return useMutation({
    // memberCount changes, both in the detail and in "my spaces". Invalidating
    // the detail key also covers the members list: React Query matches query
    // keys by prefix, and spaceMembersKey(spaceId) extends spaceDetailKey(spaceId).
    mutationFn: (userId: string) => api.removeMember(spaceId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: spaceDetailKey(spaceId) })
      queryClient.invalidateQueries({ queryKey: [SPACES_QUERY_KEY] })
    },
  })
}

export function useTransferOwnership(spaceId: string) {
  const api = useSpacesPageApi()
  const queryClient = useQueryClient()
  return useMutation({
    // myRole changes for both the previous and the new owner, everywhere it
    // appears. Invalidating the detail key also covers the members list (see
    // useRemoveMember).
    mutationFn: (userId: string) => api.transferOwnership(spaceId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: spaceDetailKey(spaceId) })
      queryClient.invalidateQueries({ queryKey: [SPACES_QUERY_KEY] })
    },
  })
}

export function useLeaveSpace(spaceId: string) {
  const api = useSpacesPageApi()
  const queryClient = useQueryClient()
  return useMutation({
    // Leaving (or being removed) makes the current URL inaccessible: the
    // component is responsible for navigating away, this hook only has to
    // invalidate ['spaces'] so the route guard sees the context is gone.
    mutationFn: () => api.leaveSpace(spaceId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SPACES_QUERY_KEY] }),
  })
}

export function useInviteMember(spaceId: string) {
  const api = useSpacesPageApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ email, role }: { email: string; role: AssignableSpaceRole }) =>
      api.inviteMember(spaceId, email, role),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: spaceInvitationsKey(spaceId) }),
  })
}

export function useRevokeInvitation(spaceId: string) {
  const api = useSpacesPageApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (invitationId: string) => api.revokeInvitation(spaceId, invitationId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: spaceInvitationsKey(spaceId) }),
  })
}

export function useAcceptInvitation() {
  const api = useSpacesPageApi()
  const queryClient = useQueryClient()
  return useMutation({
    // A context appears in "my spaces" and the accepted invitation leaves the received list.
    mutationFn: (invitationId: string) => api.acceptInvitation(invitationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [SPACES_QUERY_KEY] })
      queryClient.invalidateQueries({ queryKey: RECEIVED_INVITATIONS_QUERY_KEY })
    },
  })
}
