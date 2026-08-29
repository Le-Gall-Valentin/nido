import { useQuery } from '@tanstack/react-query'
import { useKitchenApi } from './kitchenApiContext'

export function menuEntriesKey(spaceId: string, from: string, to: string) {
  return ['kitchen', spaceId, 'menu', 'entries', from, to] as const
}

export function useMenuEntries(spaceId: string, from: string, to: string) {
  const api = useKitchenApi()
  return useQuery({
    queryKey: menuEntriesKey(spaceId, from, to),
    queryFn: () => api.listMenuEntries(spaceId, from, to),
  })
}
