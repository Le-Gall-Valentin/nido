import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError, ForbiddenError, NotFoundError } from '@/shared/lib'
import type { IShoppingApi } from '../model/IShoppingApi'
import type { ShoppingCategory, ShoppingItem } from '../model/types'

function handleError(error: unknown): never {
  if (isAxiosError(error)) {
    const status = error.response?.status
    if (status === 429) throw new RateLimitError()
    if (status === 403) throw new ForbiddenError()
    if (status === 404) throw new NotFoundError()
    if (status !== undefined) throw new ServerError()
  }
  throw new NetworkError()
}

export const shoppingApi: IShoppingApi = {
  async listCategories(spaceId) {
    try {
      const res = await client.get<ShoppingCategory[]>(`/spaces/${spaceId}/shopping/categories`)
      return res.data
    } catch (error) { handleError(error) }
  },

  async createCategory(spaceId, name) {
    try {
      const res = await client.post<ShoppingCategory>(`/spaces/${spaceId}/shopping/categories`, { name })
      return res.data
    } catch (error) { handleError(error) }
  },

  async renameCategory(spaceId, categoryId, name) {
    try {
      const res = await client.patch<ShoppingCategory>(`/spaces/${spaceId}/shopping/categories/${categoryId}`, { name })
      return res.data
    } catch (error) { handleError(error) }
  },

  async deleteCategory(spaceId, categoryId) {
    try {
      await client.delete(`/spaces/${spaceId}/shopping/categories/${categoryId}`)
    } catch (error) { handleError(error) }
  },

  async listItems(spaceId) {
    try {
      const res = await client.get<ShoppingItem[]>(`/spaces/${spaceId}/shopping/items`)
      return res.data
    } catch (error) { handleError(error) }
  },

  async addItem(spaceId, categoryId, name, quantityLabel) {
    try {
      const res = await client.post<ShoppingItem>(`/spaces/${spaceId}/shopping/items`, { categoryId, name, quantityLabel })
      return res.data
    } catch (error) { handleError(error) }
  },

  async updateItem(spaceId, itemId, categoryId, name, quantityLabel) {
    try {
      const res = await client.patch<ShoppingItem>(`/spaces/${spaceId}/shopping/items/${itemId}`, { categoryId, name, quantityLabel })
      return res.data
    } catch (error) { handleError(error) }
  },

  async toggleItemDone(spaceId, itemId) {
    try {
      await client.patch(`/spaces/${spaceId}/shopping/items/${itemId}/done`)
    } catch (error) { handleError(error) }
  },

  async deleteItem(spaceId, itemId) {
    try {
      await client.delete(`/spaces/${spaceId}/shopping/items/${itemId}`)
    } catch (error) { handleError(error) }
  },

  async clearDoneItems(spaceId) {
    try {
      await client.post(`/spaces/${spaceId}/shopping/items/clear-done`)
    } catch (error) { handleError(error) }
  },

  async clearAllItems(spaceId) {
    try {
      await client.post(`/spaces/${spaceId}/shopping/items/clear-all`)
    } catch (error) { handleError(error) }
  },

  async importFromMenu(spaceId, lines) {
    try {
      const res = await client.post<ShoppingItem[]>(`/spaces/${spaceId}/shopping/items/import-from-menu`, { lines })
      return res.data
    } catch (error) { handleError(error) }
  },
}
