import { useQuery } from '@tanstack/react-query'
import { useKitchenApi } from './kitchenApiContext'

export function shoppingListKey(spaceId: string, from: string, to: string) {
  return ['kitchen', spaceId, 'menu', 'shopping-list', from, to] as const
}

export function useShoppingList(spaceId: string, from: string, to: string) {
  const api = useKitchenApi()
  return useQuery({
    queryKey: shoppingListKey(spaceId, from, to),
    queryFn: () => api.getShoppingList(spaceId, from, to),
  })
}
