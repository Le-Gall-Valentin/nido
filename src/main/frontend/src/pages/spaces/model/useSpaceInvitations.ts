import { useQuery } from '@tanstack/react-query'
import { useSpacesPageApi } from './spacesPageApiContext'

export function spaceInvitationsKey(spaceId: string) {
  return ['space', spaceId, 'invitations'] as const
}

/**
 * `enabled` lets callers skip the request entirely for a role that cannot
 * manage the space — the backend would refuse it (403 InsufficientRole)
 * anyway, so there is no reason to fire it. Defaults to true so the hook
 * behaves like the others when the caller does not care.
 */
export function useSpaceInvitations(spaceId: string | undefined, enabled = true) {
  const api = useSpacesPageApi()
  return useQuery({
    queryKey: spaceInvitationsKey(spaceId ?? ''),
    queryFn: () => api.listInvitations(spaceId as string),
    enabled: !!spaceId && enabled,
  })
}
