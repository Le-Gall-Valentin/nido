import { describe, it, expect, vi, beforeEach } from 'vitest'
import { client } from '@/shared/api'
import { NotFoundError, ForbiddenError, RateLimitError, NetworkError } from '@/shared/lib'
import { kitchenApi } from './kitchenApi'
import type { RecipeInput } from '../model/types'

vi.mock('@/shared/api', () => ({
  client: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() },
}))

const mockClient = vi.mocked(client)

beforeEach(() => vi.clearAllMocks())

const RECIPE_INPUT: RecipeInput = {
  name: 'Riz cantonais', category: 'PLAT', minutes: 20, referencePortions: 2,
  ingredients: [{ name: 'Riz', quantity: 200, unit: 'GRAM' }], steps: [],
}

describe('kitchenApi — recipes', () => {
  it('lists recipes for a space', async () => {
    mockClient.get.mockResolvedValue({ data: [{ id: 'r1' }] })

    const result = await kitchenApi.listRecipes('space-1')

    expect(mockClient.get).toHaveBeenCalledWith('/spaces/space-1/kitchen/recipes')
    expect(result).toEqual([{ id: 'r1' }])
  })

  it('creates a recipe', async () => {
    mockClient.post.mockResolvedValue({ data: { id: 'r2', ...RECIPE_INPUT, favorite: false } })

    await kitchenApi.createRecipe('space-1', RECIPE_INPUT)

    expect(mockClient.post).toHaveBeenCalledWith('/spaces/space-1/kitchen/recipes', RECIPE_INPUT)
  })

  it('updates a recipe', async () => {
    mockClient.patch.mockResolvedValue({ data: { id: 'r1', ...RECIPE_INPUT, favorite: false } })

    await kitchenApi.updateRecipe('space-1', 'r1', RECIPE_INPUT)

    expect(mockClient.patch).toHaveBeenCalledWith('/spaces/space-1/kitchen/recipes/r1', RECIPE_INPUT)
  })

  it('deletes a recipe', async () => {
    mockClient.delete.mockResolvedValue({ data: undefined })

    await kitchenApi.deleteRecipe('space-1', 'r1')

    expect(mockClient.delete).toHaveBeenCalledWith('/spaces/space-1/kitchen/recipes/r1')
  })

  it('toggles favorite with no request body', async () => {
    mockClient.patch.mockResolvedValue({ data: { id: 'r1', favorite: true } })

    await kitchenApi.toggleFavorite('space-1', 'r1')

    expect(mockClient.patch).toHaveBeenCalledWith('/spaces/space-1/kitchen/recipes/r1/favorite')
  })

  it('copies a recipe to another space', async () => {
    mockClient.post.mockResolvedValue({ data: { id: 'r3', ...RECIPE_INPUT, favorite: false } })

    await kitchenApi.copyRecipe('space-1', 'r1', 'space-2')

    expect(mockClient.post).toHaveBeenCalledWith('/spaces/space-1/kitchen/recipes/r1/copy', { destinationSpaceId: 'space-2' })
  })

  it('moves a recipe to another space', async () => {
    mockClient.post.mockResolvedValue({ data: { id: 'r1', ...RECIPE_INPUT, favorite: false } })

    await kitchenApi.moveRecipe('space-1', 'r1', 'space-2')

    expect(mockClient.post).toHaveBeenCalledWith('/spaces/space-1/kitchen/recipes/r1/move', { destinationSpaceId: 'space-2' })
  })
})

describe('kitchenApi — menu', () => {
  it('lists menu entries with a date range', async () => {
    mockClient.get.mockResolvedValue({ data: [] })

    await kitchenApi.listMenuEntries('space-1', '2026-09-07', '2026-09-13')

    expect(mockClient.get).toHaveBeenCalledWith(
      '/spaces/space-1/kitchen/menu', { params: { from: '2026-09-07', to: '2026-09-13' } })
  })

  it('adds a menu entry', async () => {
    mockClient.post.mockResolvedValue({ data: { id: 'e1' } })

    await kitchenApi.addMenuEntry('space-1', '2026-09-07', 'r1', 4)

    expect(mockClient.post).toHaveBeenCalledWith(
      '/spaces/space-1/kitchen/menu', { date: '2026-09-07', recipeId: 'r1', portions: 4 })
  })

  it('updates a menu entrys portions', async () => {
    mockClient.patch.mockResolvedValue({ data: undefined })

    await kitchenApi.updateMenuEntryPortions('space-1', 'e1', 6)

    expect(mockClient.patch).toHaveBeenCalledWith('/spaces/space-1/kitchen/menu/e1', { portions: 6 })
  })

  it('removes a menu entry', async () => {
    mockClient.delete.mockResolvedValue({ data: undefined })

    await kitchenApi.removeMenuEntry('space-1', 'e1')

    expect(mockClient.delete).toHaveBeenCalledWith('/spaces/space-1/kitchen/menu/e1')
  })

  it('computes the shopping list for a range', async () => {
    mockClient.get.mockResolvedValue({ data: [] })

    await kitchenApi.getShoppingList('space-1', '2026-09-07', '2026-09-13')

    expect(mockClient.get).toHaveBeenCalledWith(
      '/spaces/space-1/kitchen/menu/shopping-list', { params: { from: '2026-09-07', to: '2026-09-13' } })
  })
})

describe('kitchenApi — error mapping', () => {
  it('maps a 404 to NotFoundError', async () => {
    mockClient.get.mockRejectedValue({ isAxiosError: true, response: { status: 404 } })

    await expect(kitchenApi.getRecipe('space-1', 'missing')).rejects.toBeInstanceOf(NotFoundError)
  })

  it('maps a 403 to ForbiddenError', async () => {
    mockClient.post.mockRejectedValue({ isAxiosError: true, response: { status: 403 } })

    await expect(kitchenApi.createRecipe('space-1', RECIPE_INPUT)).rejects.toBeInstanceOf(ForbiddenError)
  })

  it('maps a 429 to RateLimitError', async () => {
    mockClient.get.mockRejectedValue({ isAxiosError: true, response: { status: 429 } })

    await expect(kitchenApi.listRecipes('space-1')).rejects.toBeInstanceOf(RateLimitError)
  })

  it('maps a non-axios failure to NetworkError', async () => {
    mockClient.get.mockRejectedValue({ isAxiosError: false })

    await expect(kitchenApi.listRecipes('space-1')).rejects.toBeInstanceOf(NetworkError)
  })
})
