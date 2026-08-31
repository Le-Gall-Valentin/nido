import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useShoppingApi } from './shoppingApiContext'
import { shoppingItemsKey } from './useShoppingItems'
import type { ShoppingImportLine } from './types'

export function useAddItem(spaceId: string) {
  const api = useShoppingApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ categoryId, name, quantityLabel }: { categoryId: string; name: string; quantityLabel?: string | null }) =>
      api.addItem(spaceId, categoryId, name, quantityLabel),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: shoppingItemsKey(spaceId) }),
  })
}

export function useUpdateItem(spaceId: string) {
  const api = useShoppingApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ itemId, categoryId, name, quantityLabel }: { itemId: string; categoryId: string; name: string; quantityLabel?: string | null }) =>
      api.updateItem(spaceId, itemId, categoryId, name, quantityLabel),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: shoppingItemsKey(spaceId) }),
  })
}

export function useToggleItemDone(spaceId: string) {
  const api = useShoppingApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (itemId: string) => api.toggleItemDone(spaceId, itemId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: shoppingItemsKey(spaceId) }),
  })
}

export function useDeleteItem(spaceId: string) {
  const api = useShoppingApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (itemId: string) => api.deleteItem(spaceId, itemId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: shoppingItemsKey(spaceId) }),
  })
}

export function useClearDoneItems(spaceId: string) {
  const api = useShoppingApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api.clearDoneItems(spaceId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: shoppingItemsKey(spaceId) }),
  })
}

export function useClearAllItems(spaceId: string) {
  const api = useShoppingApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api.clearAllItems(spaceId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: shoppingItemsKey(spaceId) }),
  })
}

export function useImportFromMenu(spaceId: string) {
  const api = useShoppingApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (lines: ShoppingImportLine[]) => api.importFromMenu(spaceId, lines),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: shoppingItemsKey(spaceId) }),
  })
}
