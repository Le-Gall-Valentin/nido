import { describe, it, expect, vi, beforeEach } from 'vitest'
import { client } from '@/shared/api'
import { NotFoundError, ForbiddenError, RateLimitError, NetworkError } from '@/shared/lib'
import { shoppingApi } from './shoppingApi'

vi.mock('@/shared/api', () => ({
  client: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() },
}))

const mockClient = vi.mocked(client)

beforeEach(() => vi.clearAllMocks())

describe('shoppingApi — categories', () => {
  it('lists categories for a space', async () => {
    mockClient.get.mockResolvedValue({ data: [{ id: 'c1' }] })

    const result = await shoppingApi.listCategories('space-1')

    expect(mockClient.get).toHaveBeenCalledWith('/spaces/space-1/shopping/categories')
    expect(result).toEqual([{ id: 'c1' }])
  })

  it('creates a category', async () => {
    mockClient.post.mockResolvedValue({ data: { id: 'c2', name: 'Bricolage' } })

    await shoppingApi.createCategory('space-1', 'Bricolage')

    expect(mockClient.post).toHaveBeenCalledWith('/spaces/space-1/shopping/categories', { name: 'Bricolage' })
  })

  it('renames a category', async () => {
    mockClient.patch.mockResolvedValue({ data: { id: 'c1', name: 'Épicerie fine' } })

    await shoppingApi.renameCategory('space-1', 'c1', 'Épicerie fine')

    expect(mockClient.patch).toHaveBeenCalledWith('/spaces/space-1/shopping/categories/c1', { name: 'Épicerie fine' })
  })

  it('deletes a category', async () => {
    mockClient.delete.mockResolvedValue({ data: undefined })

    await shoppingApi.deleteCategory('space-1', 'c1')

    expect(mockClient.delete).toHaveBeenCalledWith('/spaces/space-1/shopping/categories/c1')
  })
})

describe('shoppingApi — items', () => {
  it('lists items for a space', async () => {
    mockClient.get.mockResolvedValue({ data: [] })

    await shoppingApi.listItems('space-1')

    expect(mockClient.get).toHaveBeenCalledWith('/spaces/space-1/shopping/items')
  })

  it('adds an item', async () => {
    mockClient.post.mockResolvedValue({ data: { id: 'i1' } })

    await shoppingApi.addItem('space-1', 'c1', 'Pâtes', '500 g')

    expect(mockClient.post).toHaveBeenCalledWith('/spaces/space-1/shopping/items', { categoryId: 'c1', name: 'Pâtes', quantityLabel: '500 g' })
  })

  it('updates an item', async () => {
    mockClient.patch.mockResolvedValue({ data: { id: 'i1' } })

    await shoppingApi.updateItem('space-1', 'i1', 'c2', 'Pâtes complètes', '1 kg')

    expect(mockClient.patch).toHaveBeenCalledWith('/spaces/space-1/shopping/items/i1', { categoryId: 'c2', name: 'Pâtes complètes', quantityLabel: '1 kg' })
  })

  it('toggles an item done with no request body', async () => {
    mockClient.patch.mockResolvedValue({ data: undefined })

    await shoppingApi.toggleItemDone('space-1', 'i1')

    expect(mockClient.patch).toHaveBeenCalledWith('/spaces/space-1/shopping/items/i1/done')
  })

  it('deletes an item', async () => {
    mockClient.delete.mockResolvedValue({ data: undefined })

    await shoppingApi.deleteItem('space-1', 'i1')

    expect(mockClient.delete).toHaveBeenCalledWith('/spaces/space-1/shopping/items/i1')
  })

  it('clears done items', async () => {
    mockClient.post.mockResolvedValue({ data: undefined })

    await shoppingApi.clearDoneItems('space-1')

    expect(mockClient.post).toHaveBeenCalledWith('/spaces/space-1/shopping/items/clear-done')
  })

  it('clears all items', async () => {
    mockClient.post.mockResolvedValue({ data: undefined })

    await shoppingApi.clearAllItems('space-1')

    expect(mockClient.post).toHaveBeenCalledWith('/spaces/space-1/shopping/items/clear-all')
  })

  it('imports items from the menu', async () => {
    mockClient.post.mockResolvedValue({ data: [{ id: 'i1' }] })
    const lines = [{ name: 'Poulet', quantityLabel: '1 kg', categoryId: 'c1' }]

    await shoppingApi.importFromMenu('space-1', lines)

    expect(mockClient.post).toHaveBeenCalledWith('/spaces/space-1/shopping/items/import-from-menu', { lines })
  })
})

describe('shoppingApi — error mapping', () => {
  it('maps a 404 to NotFoundError', async () => {
    mockClient.get.mockRejectedValue({ isAxiosError: true, response: { status: 404 } })

    await expect(shoppingApi.listItems('space-1')).rejects.toBeInstanceOf(NotFoundError)
  })

  it('maps a 403 to ForbiddenError', async () => {
    mockClient.post.mockRejectedValue({ isAxiosError: true, response: { status: 403 } })

    await expect(shoppingApi.addItem('space-1', 'c1', 'Pâtes')).rejects.toBeInstanceOf(ForbiddenError)
  })

  it('maps a 429 to RateLimitError', async () => {
    mockClient.get.mockRejectedValue({ isAxiosError: true, response: { status: 429 } })

    await expect(shoppingApi.listCategories('space-1')).rejects.toBeInstanceOf(RateLimitError)
  })

  it('maps a non-axios failure to NetworkError', async () => {
    mockClient.get.mockRejectedValue({ isAxiosError: false })

    await expect(shoppingApi.listCategories('space-1')).rejects.toBeInstanceOf(NetworkError)
  })
})
