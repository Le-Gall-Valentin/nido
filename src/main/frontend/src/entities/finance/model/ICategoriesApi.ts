import type { Category, TransactionType } from './types'

/** Port for category CRUD. */
export interface ICategoriesApi {
  listCategories(spaceId: string): Promise<Category[]>
  createCategory(spaceId: string, label: string, color: string, icon: string, type: TransactionType): Promise<Category>
  updateCategory(spaceId: string, categoryId: string, label: string, color: string, icon: string): Promise<Category>
  deleteCategory(spaceId: string, categoryId: string): Promise<void>
}
