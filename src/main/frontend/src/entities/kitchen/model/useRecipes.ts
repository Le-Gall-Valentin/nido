import { useQuery } from '@tanstack/react-query'
import { useKitchenApi } from './kitchenApiContext'

export function recipesKey(spaceId: string) {
  return ['kitchen', spaceId, 'recipes'] as const
}

export function useRecipes(spaceId: string | undefined) {
  const api = useKitchenApi()
  return useQuery({
    queryKey: recipesKey(spaceId ?? ''),
    queryFn: () => api.listRecipes(spaceId as string),
    enabled: !!spaceId,
  })
}
