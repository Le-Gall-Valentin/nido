import { useQuery } from '@tanstack/react-query'
import { useSpacesPageApi } from './spacesPageApiContext'

export function spaceDetailKey(spaceId: string) {
  return ['space', spaceId] as const
}

export function useSpaceDetail(spaceId: string | undefined) {
  const api = useSpacesPageApi()
  return useQuery({
    queryKey: spaceDetailKey(spaceId ?? ''),
    queryFn: () => api.getSpaceDetail(spaceId as string),
    enabled: !!spaceId,
  })
}
