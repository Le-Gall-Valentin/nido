import { useQuery } from '@tanstack/react-query'
import { useSpaceMembersApi } from './spaceMembersApiContext'

export function spaceMembersKey(spaceId: string) {
  return ['space', spaceId, 'members'] as const
}

export function useSpaceMembers(spaceId: string | undefined) {
  const api = useSpaceMembersApi()
  return useQuery({
    queryKey: spaceMembersKey(spaceId ?? ''),
    queryFn: () => api.listMembers(spaceId as string),
    enabled: !!spaceId,
  })
}
