import { useQuery } from '@tanstack/react-query'
import { useKitchenApi } from './kitchenApiContext'

export function recipeKey(spaceId: string, recipeId: string) {
  return ['kitchen', spaceId, 'recipes', recipeId] as const
}

export function useRecipe(spaceId: string | undefined, recipeId: string | undefined) {
  const api = useKitchenApi()
  return useQuery({
    queryKey: recipeKey(spaceId ?? '', recipeId ?? ''),
    queryFn: () => api.getRecipe(spaceId as string, recipeId as string),
    enabled: !!spaceId && !!recipeId,
  })
}
