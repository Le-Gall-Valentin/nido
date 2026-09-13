import { useQuery } from '@tanstack/react-query'
import { useSpaceApi } from './spaceApiContext'

export function spaceDetailKey(spaceId: string) {
  return ['space', spaceId] as const
}

export function useSpaceDetail(spaceId: string | undefined) {
  const api = useSpaceApi()
  return useQuery({
    queryKey: spaceDetailKey(spaceId ?? ''),
    queryFn: () => api.getSpaceDetail(spaceId as string),
    enabled: !!spaceId,
  })
}
