import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError, ForbiddenError, NotFoundError } from '@/shared/lib'
import type { IKitchenApi } from '../model/IKitchenApi'
import type { MenuEntry, Recipe, RecipeInput, ShoppingListLine } from '../model/types'

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

export const kitchenApi: IKitchenApi = {
  async listRecipes(spaceId) {
    try {
      const res = await client.get<Recipe[]>(`/spaces/${spaceId}/kitchen/recipes`)
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async getRecipe(spaceId, recipeId) {
    try {
      const res = await client.get<Recipe>(`/spaces/${spaceId}/kitchen/recipes/${recipeId}`)
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async createRecipe(spaceId, input) {
    try {
      const res = await client.post<Recipe>(`/spaces/${spaceId}/kitchen/recipes`, input)
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async updateRecipe(spaceId, recipeId, input: RecipeInput) {
    try {
      const res = await client.patch<Recipe>(`/spaces/${spaceId}/kitchen/recipes/${recipeId}`, input)
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async deleteRecipe(spaceId, recipeId) {
    try {
      await client.delete(`/spaces/${spaceId}/kitchen/recipes/${recipeId}`)
    } catch (error) {
      handleError(error)
    }
  },

  async toggleFavorite(spaceId, recipeId) {
    try {
      const res = await client.patch<Recipe>(`/spaces/${spaceId}/kitchen/recipes/${recipeId}/favorite`)
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async copyRecipe(spaceId, recipeId, destinationSpaceId) {
    try {
      const res = await client.post<Recipe>(`/spaces/${spaceId}/kitchen/recipes/${recipeId}/copy`, { destinationSpaceId })
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async moveRecipe(spaceId, recipeId, destinationSpaceId) {
    try {
      const res = await client.post<Recipe>(`/spaces/${spaceId}/kitchen/recipes/${recipeId}/move`, { destinationSpaceId })
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async listMenuEntries(spaceId, from, to) {
    try {
      const res = await client.get<MenuEntry[]>(`/spaces/${spaceId}/kitchen/menu`, { params: { from, to } })
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async addMenuEntry(spaceId, date, recipeId, portions) {
    try {
      const res = await client.post<MenuEntry>(`/spaces/${spaceId}/kitchen/menu`, { date, recipeId, portions })
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async updateMenuEntryPortions(spaceId, entryId, portions) {
    try {
      await client.patch(`/spaces/${spaceId}/kitchen/menu/${entryId}`, { portions })
    } catch (error) {
      handleError(error)
    }
  },

  async removeMenuEntry(spaceId, entryId) {
    try {
      await client.delete(`/spaces/${spaceId}/kitchen/menu/${entryId}`)
    } catch (error) {
      handleError(error)
    }
  },

  async getShoppingList(spaceId, from, to) {
    try {
      const res = await client.get<ShoppingListLine[]>(
        `/spaces/${spaceId}/kitchen/menu/shopping-list`, { params: { from, to } })
      return res.data
    } catch (error) {
      handleError(error)
    }
  },
}
