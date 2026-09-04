import type { ShoppingItem } from '@/entities/shopping-list'

/** Resolves a drag/drop-or-click move: which item, if any, actually needs to change category. */
export function resolveItemMove(items: ShoppingItem[], itemId: string, targetCategoryId: string): ShoppingItem | null {
  const item = items.find((i) => i.id === itemId)
  if (!item || item.categoryId === targetCategoryId) return null
  return item
}
