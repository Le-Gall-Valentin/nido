import { useQuery } from '@tanstack/react-query'
import { useShoppingApi } from './shoppingApiContext'

export function shoppingCategoriesKey(spaceId: string) {
  return ['shopping', spaceId, 'categories'] as const
}

export function useShoppingCategories(spaceId: string | undefined) {
  const api = useShoppingApi()
  return useQuery({
    queryKey: shoppingCategoriesKey(spaceId ?? ''),
    queryFn: () => api.listCategories(spaceId as string),
    enabled: !!spaceId,
  })
}
