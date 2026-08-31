import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { SpacesApiProvider } from '@/features/space-switcher'
import type { ISpacesApi } from '@/features/space-switcher'
import type { SpaceSummary } from '@/entities/space'
import { ShoppingListPage } from './ShoppingListPage'
import type { IShoppingApi, ShoppingCategory, ShoppingItem } from '@/entities/shopping-list'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const CATEGORIES: ShoppingCategory[] = [
  { id: 'cat-1', name: 'Épicerie', position: 0, fallback: false },
  { id: 'cat-2', name: 'Maison & divers', position: 1, fallback: true },
]

const ITEMS: ShoppingItem[] = [
  { id: 'i1', categoryId: 'cat-1', name: 'Pâtes', quantityLabel: '500 g', done: false, position: 0 },
]

const CURRENT_SPACE: SpaceSummary = {
  id: 'space-1', type: 'SHARED', name: 'Chez nous', accent: '#c17a5c', glyph: '🏡', myRole: 'MEMBER', memberCount: 2,
}

function fakeApi(overrides: Partial<IShoppingApi> = {}): IShoppingApi {
  return {
    listCategories: vi.fn().mockResolvedValue(CATEGORIES),
    createCategory: vi.fn(),
    renameCategory: vi.fn(),
    deleteCategory: vi.fn(),
    listItems: vi.fn().mockResolvedValue(ITEMS),
    addItem: vi.fn(),
    updateItem: vi.fn(),
    toggleItemDone: vi.fn().mockResolvedValue(undefined),
    deleteItem: vi.fn().mockResolvedValue(undefined),
    clearDoneItems: vi.fn().mockResolvedValue(undefined),
    clearAllItems: vi.fn().mockResolvedValue(undefined),
    importFromMenu: vi.fn(),
    ...overrides,
  }
}

function fakeSpacesApi(mySpaces: SpaceSummary[] = [CURRENT_SPACE]): ISpacesApi {
  return {
    listMySpaces: vi.fn().mockResolvedValue(mySpaces),
    getSpace: vi.fn(),
  }
}

function setup(api: IShoppingApi = fakeApi(), spacesApi: ISpacesApi = fakeSpacesApi()) {
  const queryClient = createTestQueryClient()
  render(
    <QueryClientProvider client={queryClient}>
      <SpacesApiProvider api={spacesApi}>
        <MemoryRouter initialEntries={['/s/space-1/organisation/courses']}>
          <Routes>
            <Route path="/s/:spaceId/organisation/courses" element={<ShoppingListPage api={api} />} />
          </Routes>
        </MemoryRouter>
      </SpacesApiProvider>
    </QueryClientProvider>
  )
  return { api, spacesApi }
}

describe('ShoppingListPage', () => {
  it('shows items grouped under their category', async () => {
    setup()

    // "Épicerie" also appears as an <option> in the category <select> dropdowns,
    // so at least one match (not exactly one) is the meaningful assertion here.
    expect((await screen.findAllByText('Épicerie')).length).toBeGreaterThan(0)
    expect(screen.getByText('Pâtes')).toBeDefined()
  })

  it('adds an item to the selected category', async () => {
    const api = fakeApi({ addItem: vi.fn().mockResolvedValue({ ...ITEMS[0], id: 'i2', name: 'Riz' }) })
    setup(api)
    await screen.findByText('Pâtes')

    fireEvent.change(screen.getByLabelText('add_item_placeholder'), { target: { value: 'Riz' } })
    fireEvent.click(screen.getByLabelText('add_item'))

    await waitFor(() => expect(api.addItem).toHaveBeenCalledWith('space-1', 'cat-1', 'Riz', undefined))
  })

  it('recategorizes an item via its row dropdown', async () => {
    const api = fakeApi()
    setup(api)
    await screen.findByText('Pâtes')

    fireEvent.change(screen.getByLabelText('recategorize'), { target: { value: 'cat-2' } })

    await waitFor(() => expect(api.updateItem).toHaveBeenCalledWith('space-1', 'i1', 'cat-2', 'Pâtes', '500 g'))
  })

  it('toggles an item done', async () => {
    const api = fakeApi()
    setup(api)
    await screen.findByText('Pâtes')

    fireEvent.click(screen.getByLabelText('toggle_done'))

    await waitFor(() => expect(api.toggleItemDone).toHaveBeenCalledWith('space-1', 'i1'))
  })

  it('deletes an item', async () => {
    const api = fakeApi()
    setup(api)
    await screen.findByText('Pâtes')

    fireEvent.click(screen.getByLabelText('delete_item'))

    await waitFor(() => expect(api.deleteItem).toHaveBeenCalledWith('space-1', 'i1'))
  })

  it('clears done items', async () => {
    const api = fakeApi()
    setup(api)
    await screen.findByText('Pâtes')

    fireEvent.click(screen.getByText('clear_done'))

    await waitFor(() => expect(api.clearDoneItems).toHaveBeenCalledWith('space-1'))
  })

  it('creates a new category', async () => {
    const api = fakeApi({ createCategory: vi.fn().mockResolvedValue({ id: 'cat-3', name: 'Bricolage', position: 2, fallback: false }) })
    setup(api)
    await screen.findByText('Pâtes')

    fireEvent.change(screen.getByLabelText('new_category_placeholder'), { target: { value: 'Bricolage' } })
    fireEvent.click(screen.getByText('new_category'))

    await waitFor(() => expect(api.createCategory).toHaveBeenCalledWith('space-1', 'Bricolage'))
  })

  it('deletes a non-fallback category but not the fallback one', async () => {
    const api = fakeApi()
    setup(api)
    await screen.findByText('Pâtes')

    expect(screen.queryByLabelText('category_delete')).not.toBeNull()
    fireEvent.click(screen.getByLabelText('category_delete'))

    await waitFor(() => expect(api.deleteCategory).toHaveBeenCalledWith('space-1', 'cat-1'))
  })
})

describe('ShoppingListPage — read-only role', () => {
  it('hides every write control for a viewer', async () => {
    const spacesApi = fakeSpacesApi([{ ...CURRENT_SPACE, myRole: 'VIEWER' }])
    setup(fakeApi(), spacesApi)
    await screen.findByText('Pâtes')

    expect(screen.queryByLabelText('add_item_placeholder')).toBeNull()
    expect(screen.queryByLabelText('recategorize')).toBeNull()
    expect(screen.queryByLabelText('delete_item')).toBeNull()
    expect(screen.queryByText('new_category')).toBeNull()
  })
})

describe('ShoppingListPage — mutation errors', () => {
  it('shows an error message when adding an item fails', async () => {
    const api = fakeApi({ addItem: vi.fn().mockRejectedValue(new Error('boom')) })
    setup(api)
    await screen.findByText('Pâtes')

    fireEvent.change(screen.getByLabelText('add_item_placeholder'), { target: { value: 'Riz' } })
    fireEvent.click(screen.getByLabelText('add_item'))

    expect(await screen.findByText('error.action_failed')).toBeDefined()
  })
})
