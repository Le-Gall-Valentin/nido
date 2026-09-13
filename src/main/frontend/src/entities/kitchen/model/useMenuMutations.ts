import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useKitchenApi } from './kitchenApiContext'

/**
 * Every menu mutation invalidates the whole ['kitchen', spaceId, 'menu']
 * prefix: it covers both `menuEntriesKey` and `shoppingListKey` for
 * whichever week range is currently mounted, the same prefix-matching
 * reasoning `useRecipeMutations` documents for recipes.
 */
function menuKey(spaceId: string) {
  return ['kitchen', spaceId, 'menu'] as const
}

export function useAddMenuEntry(spaceId: string) {
  const api = useKitchenApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ date, recipeId, portions }: { date: string; recipeId: string; portions: number }) =>
      api.addMenuEntry(spaceId, date, recipeId, portions),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: menuKey(spaceId) }),
  })
}

export function useUpdateMenuEntryPortions(spaceId: string) {
  const api = useKitchenApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ entryId, portions }: { entryId: string; portions: number }) =>
      api.updateMenuEntryPortions(spaceId, entryId, portions),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: menuKey(spaceId) }),
  })
}

export function useRemoveMenuEntry(spaceId: string) {
  const api = useKitchenApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (entryId: string) => api.removeMenuEntry(spaceId, entryId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: menuKey(spaceId) }),
  })
}
