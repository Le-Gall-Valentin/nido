import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { RecipeInput } from './types'
import { useKitchenApi } from './kitchenApiContext'
import { recipesKey } from './useRecipes'

/**
 * Every recipe mutation invalidates just recipesKey(spaceId): React Query
 * matches keys by prefix, and recipeKey(spaceId, id) extends it, so a single
 * recipe's detail query is covered too — same reasoning `useRemoveMember`
 * documents on the spaces page.
 */

export function useCreateRecipe(spaceId: string) {
  const api = useKitchenApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: RecipeInput) => api.createRecipe(spaceId, input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: recipesKey(spaceId) }),
  })
}

export function useUpdateRecipe(spaceId: string, recipeId: string) {
  const api = useKitchenApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: RecipeInput) => api.updateRecipe(spaceId, recipeId, input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: recipesKey(spaceId) }),
  })
}

export function useDeleteRecipe(spaceId: string) {
  const api = useKitchenApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (recipeId: string) => api.deleteRecipe(spaceId, recipeId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: recipesKey(spaceId) }),
  })
}

export function useToggleFavorite(spaceId: string) {
  const api = useKitchenApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (recipeId: string) => api.toggleFavorite(spaceId, recipeId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: recipesKey(spaceId) }),
  })
}
