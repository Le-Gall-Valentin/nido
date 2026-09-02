import { useQuery } from '@tanstack/react-query'
import { useShoppingApi } from './shoppingApiContext'

export function shoppingItemsKey(spaceId: string) {
  return ['shopping', spaceId, 'items'] as const
}

export function useShoppingItems(spaceId: string | undefined) {
  const api = useShoppingApi()
  return useQuery({
    queryKey: shoppingItemsKey(spaceId ?? ''),
    queryFn: () => api.listItems(spaceId as string),
    enabled: !!spaceId,
  })
}
