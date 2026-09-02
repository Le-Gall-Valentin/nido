import type { MeasurementUnit } from '@/shared/lib'
import type { ShoppingCategory, ShoppingImportLine, ShoppingItem } from './types'

/**
 * Port for the Shopping List page. Consumers (hooks) depend on this
 * contract, never on the concrete axios-backed implementation, which is
 * injected through ShoppingApiProvider.
 */
export interface IShoppingApi {
  listCategories(spaceId: string): Promise<ShoppingCategory[]>
  createCategory(spaceId: string, name: string): Promise<ShoppingCategory>
  renameCategory(spaceId: string, categoryId: string, name: string): Promise<ShoppingCategory>
  deleteCategory(spaceId: string, categoryId: string): Promise<void>

  listItems(spaceId: string): Promise<ShoppingItem[]>
  addItem(spaceId: string, categoryId: string, name: string, quantity?: number | null, unit?: MeasurementUnit | null): Promise<ShoppingItem>
  updateItem(spaceId: string, itemId: string, categoryId: string, name: string, quantity?: number | null, unit?: MeasurementUnit | null): Promise<ShoppingItem>
  toggleItemDone(spaceId: string, itemId: string): Promise<void>
  deleteItem(spaceId: string, itemId: string): Promise<void>
  clearDoneItems(spaceId: string): Promise<void>
  clearAllItems(spaceId: string): Promise<void>
  importFromMenu(spaceId: string, lines: ShoppingImportLine[]): Promise<ShoppingItem[]>
}
