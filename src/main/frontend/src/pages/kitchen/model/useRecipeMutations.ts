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

interface TransferVariables {
  recipeId: string
  destinationSpaceId: string
}

/**
 * Copy/move both invalidate recipesKey for the source AND the destination
 * space — cheap even if the destination's list was never fetched, and
 * necessary since the item now also (or only, for move) exists there.
 */
export function useCopyRecipe(spaceId: string) {
  const api = useKitchenApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ recipeId, destinationSpaceId }: TransferVariables) => api.copyRecipe(spaceId, recipeId, destinationSpaceId),
    onSuccess: (_recipe, { destinationSpaceId }) => {
      queryClient.invalidateQueries({ queryKey: recipesKey(spaceId) })
      queryClient.invalidateQueries({ queryKey: recipesKey(destinationSpaceId) })
    },
  })
}

export function useMoveRecipe(spaceId: string) {
  const api = useKitchenApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ recipeId, destinationSpaceId }: TransferVariables) => api.moveRecipe(spaceId, recipeId, destinationSpaceId),
    onSuccess: (_recipe, { destinationSpaceId }) => {
      queryClient.invalidateQueries({ queryKey: recipesKey(spaceId) })
      queryClient.invalidateQueries({ queryKey: recipesKey(destinationSpaceId) })
    },
  })
}
