import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useShoppingApi } from './shoppingApiContext'
import { shoppingCategoriesKey } from './useShoppingCategories'
import { shoppingItemsKey } from './useShoppingItems'

export function useCreateCategory(spaceId: string) {
  const api = useShoppingApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (name: string) => api.createCategory(spaceId, name),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: shoppingCategoriesKey(spaceId) }),
  })
}

export function useRenameCategory(spaceId: string) {
  const api = useShoppingApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ categoryId, name }: { categoryId: string; name: string }) => api.renameCategory(spaceId, categoryId, name),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: shoppingCategoriesKey(spaceId) }),
  })
}

export function useDeleteCategory(spaceId: string) {
  const api = useShoppingApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (categoryId: string) => api.deleteCategory(spaceId, categoryId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: shoppingCategoriesKey(spaceId) })
      queryClient.invalidateQueries({ queryKey: shoppingItemsKey(spaceId) })
    },
  })
}
